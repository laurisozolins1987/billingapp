package com.example.billingapp

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.billingapp.databinding.ActivityTransactionDetailBinding
import com.example.billingapp.databinding.DialogAddTransactionBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding
    private val viewModel: TransactionViewModel by viewModels()
    private var currentTransaction: Transaction? = null
    private var categoryNames: List<String> = emptyList()
    private var folderNames: List<String> = emptyList()
    private var folderList: List<Folder> = emptyList()
    private var editImagePath: String? = null
    private var editDialogBinding: DialogAddTransactionBinding? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val imagePath = copyImageToInternal(it)
            if (imagePath != null) {
                editImagePath = imagePath
                editDialogBinding?.let { db ->
                    db.cardImagePreview.visibility = View.VISIBLE
                    db.ivImagePreview.setImageBitmap(BitmapFactory.decodeFile(imagePath))
                }
            }
        }
    }

    private fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val dir = File(filesDir, "receipts")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarDetail.setNavigationOnClickListener { finish() }

        val transactionId = intent.getIntExtra(EXTRA_TRANSACTION_ID, -1)
        if (transactionId == -1) {
            finish()
            return
        }

        // Observe categories and folders for edit dialog
        viewModel.allCategories.observe(this) { categories ->
            categoryNames = categories.map { it.name }
        }
        viewModel.allFolders.observe(this) { folders ->
            folderList = folders
            folderNames = folders.map { it.name }
        }

        viewModel.getTransactionById(transactionId).observe(this) { transaction ->
            if (transaction != null) {
                currentTransaction = transaction
                displayTransaction(transaction)
            } else {
                finish()
            }
        }

        // Edit button
        binding.btnEdit.setOnClickListener {
            currentTransaction?.let { showEditDialog(it) }
        }

        // Bookmark toggle
        binding.btnBookmark.setOnClickListener {
            currentTransaction?.let { t ->
                viewModel.toggleBookmark(t)
            }
        }

        // Archive button
        binding.btnArchive.setOnClickListener {
            currentTransaction?.let { t ->
                if (t.isArchived) {
                    viewModel.unarchive(t)
                    Toast.makeText(this, R.string.transaction_unarchived, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.archive(t)
                    Toast.makeText(this, R.string.transaction_archived, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            currentTransaction?.let { t ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_transaction)
                    .setMessage(R.string.delete_confirm)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.delete(t)
                        Toast.makeText(this, R.string.transaction_deleted, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogBinding = DialogAddTransactionBinding.inflate(LayoutInflater.from(this))
        editDialogBinding = dialogBinding
        editImagePath = transaction.imagePath.ifEmpty { null }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
        var selectedDate = transaction.date
        var selectedHour = cal.get(Calendar.HOUR_OF_DAY)
        var selectedMinute = cal.get(Calendar.MINUTE)

        // Pre-fill fields
        dialogBinding.etAmount.setText(String.format("%.2f", transaction.amount))
        dialogBinding.etNote.setText(transaction.note)
        dialogBinding.etDescription.setText(transaction.description)
        dialogBinding.btnPickDate.text = dateFormat.format(Date(selectedDate))
        dialogBinding.btnPickTime.text = timeFormat.format(Date(selectedDate))

        // Type toggle
        if (transaction.isIncome) {
            dialogBinding.toggleType.check(R.id.btn_type_income)
            dialogBinding.rbIncome.isChecked = true
            dialogBinding.rbExpense.isChecked = false
        } else {
            dialogBinding.toggleType.check(R.id.btn_type_expense)
            dialogBinding.rbIncome.isChecked = false
            dialogBinding.rbExpense.isChecked = true
        }

        // Category dropdown
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        dialogBinding.actCategory.setAdapter(categoryAdapter)
        if (transaction.category.isNotEmpty()) {
            dialogBinding.actCategory.setText(transaction.category, false)
        }

        // Folder dropdown
        val folderOptions = mutableListOf(getString(R.string.no_folder))
        folderOptions.addAll(folderNames)
        val folderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, folderOptions)
        dialogBinding.actFolder.setAdapter(folderAdapter)
        if (transaction.folderId > 0) {
            val folderName = folderList.find { it.id == transaction.folderId }?.name
            if (folderName != null) {
                dialogBinding.actFolder.setText(folderName, false)
            }
        }

        // Show existing image
        if (transaction.imagePath.isNotEmpty()) {
            val imageFile = File(transaction.imagePath)
            if (imageFile.exists()) {
                dialogBinding.cardImagePreview.visibility = View.VISIBLE
                dialogBinding.ivImagePreview.setImageBitmap(BitmapFactory.decodeFile(transaction.imagePath))
            }
        }

        // Attach image button
        dialogBinding.btnAttachImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Remove image button
        dialogBinding.btnRemoveImage.setOnClickListener {
            editImagePath = null
            dialogBinding.cardImagePreview.visibility = View.GONE
        }

        // Toggle syncs with hidden radio
        dialogBinding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                dialogBinding.rbIncome.isChecked = checkedId == R.id.btn_type_income
                dialogBinding.rbExpense.isChecked = checkedId == R.id.btn_type_expense
            }
        }

        // Date picker
        dialogBinding.btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(selectedDate)
                .build()
            datePicker.addOnPositiveButtonClickListener {
                selectedDate = it
                dialogBinding.btnPickDate.text = dateFormat.format(Date(it))
            }
            datePicker.show(supportFragmentManager, "EDIT_DATE_PICKER")
        }

        // Time picker
        dialogBinding.btnPickTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText(R.string.select_time)
                .build()
            timePicker.addOnPositiveButtonClickListener {
                selectedHour = timePicker.hour
                selectedMinute = timePicker.minute
                dialogBinding.btnPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            }
            timePicker.show(supportFragmentManager, "EDIT_TIME_PICKER")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_transaction)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val amountText = dialogBinding.etAmount.text.toString()
                val note = dialogBinding.etNote.text.toString()
                val category = dialogBinding.actCategory.text.toString()
                val description = dialogBinding.etDescription.text.toString()
                val isIncome = dialogBinding.rbIncome.isChecked
                val selectedFolderName = dialogBinding.actFolder.text.toString()
                val folderId = if (selectedFolderName.isNotEmpty() && selectedFolderName != getString(R.string.no_folder)) {
                    folderList.find { it.name == selectedFolderName }?.id ?: 0
                } else 0
                val imagePath = editImagePath ?: ""

                if (amountText.isNotEmpty()) {
                    try {
                        val amount = amountText.toDouble()
                        val finalCalendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val updated = transaction.copy(
                            amount = amount,
                            note = note,
                            date = finalCalendar.timeInMillis,
                            isIncome = isIncome,
                            category = category,
                            description = description,
                            folderId = folderId,
                            imagePath = imagePath
                        )
                        viewModel.update(updated)
                        Toast.makeText(this, R.string.transaction_updated, Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, R.string.enter_amount, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { editDialogBinding = null }
            .show()
    }

    private fun displayTransaction(transaction: Transaction) {
        val currencySymbol = SettingsActivity.getCurrencySymbol(this)
        val fullDateFormat = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fullDateTimeFormat = SimpleDateFormat("d. MMMM yyyy, HH:mm", Locale.getDefault())

        // Amount & type
        if (transaction.isIncome) {
            binding.tvDetailAmount.text = String.format("+%,.2f %s", transaction.amount, currencySymbol)
            binding.tvDetailAmount.setTextColor(ContextCompat.getColor(this, R.color.income_green))
            binding.tvDetailType.text = getString(R.string.income)
            binding.iconContainerDetail.setBackgroundResource(R.drawable.bg_icon_income)
            binding.ivTypeIcon.setImageResource(R.drawable.ic_income)
        } else {
            binding.tvDetailAmount.text = String.format("-%,.2f %s", transaction.amount, currencySymbol)
            binding.tvDetailAmount.setTextColor(ContextCompat.getColor(this, R.color.expense_red))
            binding.tvDetailType.text = getString(R.string.expense)
            binding.iconContainerDetail.setBackgroundResource(R.drawable.bg_icon_expense)
            binding.ivTypeIcon.setImageResource(R.drawable.ic_expense)
        }

        // Bookmark state
        if (transaction.isBookmarked) {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark)
            binding.btnBookmark.imageTintList = ContextCompat.getColorStateList(this, R.color.bookmark_gold)
        } else {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_border)
            binding.btnBookmark.imageTintList = null
        }

        // Archive button state
        if (transaction.isArchived) {
            binding.btnArchive.text = getString(R.string.unarchive)
            binding.btnArchive.setIconResource(R.drawable.ic_unarchive)
        } else {
            binding.btnArchive.text = getString(R.string.archive_transaction)
            binding.btnArchive.setIconResource(R.drawable.ic_archive)
        }

        // Note
        binding.tvDetailNote.text = transaction.note.ifEmpty {
            if (transaction.isIncome) getString(R.string.income) else getString(R.string.expense)
        }

        // Category
        binding.tvDetailCategory.text = transaction.category.ifEmpty { getString(R.string.no_category) }

        // Description
        binding.tvDetailDescription.text = transaction.description.ifEmpty { getString(R.string.no_description) }

        // Date
        val transactionDate = Date(transaction.date)
        binding.tvDetailDate.text = "${fullDateFormat.format(transactionDate)}, ${timeFormat.format(transactionDate)}"

        // Created at
        if (transaction.createdAt > 0) {
            binding.tvDetailCreated.text = fullDateTimeFormat.format(Date(transaction.createdAt))
        } else {
            binding.tvDetailCreated.text = getString(R.string.unknown)
        }

        // Receipt image
        if (transaction.imagePath.isNotEmpty()) {
            val imageFile = File(transaction.imagePath)
            if (imageFile.exists()) {
                binding.cardReceipt.visibility = View.VISIBLE
                binding.ivReceipt.setImageBitmap(BitmapFactory.decodeFile(transaction.imagePath))
            } else {
                binding.cardReceipt.visibility = View.GONE
            }
        } else {
            binding.cardReceipt.visibility = View.GONE
        }

        // Folder
        if (transaction.folderId > 0) {
            viewModel.allFolders.observe(this) { folders ->
                val folder = folders.find { it.id == transaction.folderId }
                if (folder != null) {
                    binding.cardFolder.visibility = View.VISIBLE
                    binding.tvDetailFolder.text = folder.name
                } else {
                    binding.cardFolder.visibility = View.GONE
                }
            }
        } else {
            binding.cardFolder.visibility = View.GONE
        }
    }
}
