package com.example.billingapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.billingapp.databinding.ActivityMainBinding
import com.example.billingapp.databinding.DialogAddTransactionBinding
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.File
import java.util.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    private var isAuthenticated = false
    private var categoryNames: List<String> = emptyList()
    private var folderNames: List<String> = emptyList()
    private var folderList: List<Folder> = emptyList()
    private var pendingImageUri: Uri? = null
    private var pendingImagePath: String? = null
    private var currentDialogBinding: DialogAddTransactionBinding? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            val imagePath = copyImageToInternal(it)
            if (imagePath != null) {
                pendingImagePath = imagePath
                currentDialogBinding?.let { db ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dark mode before super
        if (SettingsActivity.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupSearch()
        setupFilterChips()
    }

    override fun onResume() {
        super.onResume()
        if (SettingsActivity.isBiometricEnabled(this) && !isAuthenticated) {
            showBiometricPrompt()
        }
        // Refresh adapter in case currency changed
        adapter.notifyDataSetChanged()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            isAuthenticated = true
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAuthenticated = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        finishAffinity()
                    }
                }

                override fun onAuthenticationFailed() {
                    // Stay on prompt
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_negative))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            val intent = Intent(this, TransactionDetailActivity::class.java)
            intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_ID, transaction.id)
            startActivity(intent)
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = adapter.currentList[position]
                viewModel.delete(transaction)
                Snackbar.make(binding.root, R.string.transaction_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.restore(transaction) }
                    .show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvTransactions)
    }

    private fun setupObservers() {
        viewModel.filteredTransactions.observe(this) { transactions ->
            adapter.submitList(transactions)
            updateSummary(transactions)
            binding.tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            binding.rvTransactions.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
            binding.tvTransactionCount.text = getString(R.string.transaction_count, transactions.size)
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener { showAddTransactionDialog() }
        binding.btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Long press on calendar to clear date filter
        binding.btnCalendar.setOnLongClickListener {
            viewModel.clearDateFilter()
            binding.tvFilterLabel.visibility = View.GONE
            Toast.makeText(this, R.string.filter_cleared, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.search(s?.toString()?.trim() ?: "")
            }
        })
    }

    private fun setupFilterChips() {
        // Type filter chips
        binding.chipAll.setOnClickListener {
            viewModel.setTypeFilter(null)
            binding.chipAll.isChecked = true
            binding.chipIncome.isChecked = false
            binding.chipExpense.isChecked = false
        }
        binding.chipIncome.setOnClickListener {
            viewModel.setTypeFilter(true)
            binding.chipAll.isChecked = false
            binding.chipIncome.isChecked = true
            binding.chipExpense.isChecked = false
        }
        binding.chipExpense.setOnClickListener {
            viewModel.setTypeFilter(false)
            binding.chipAll.isChecked = false
            binding.chipIncome.isChecked = false
            binding.chipExpense.isChecked = true
        }

        // Category chip - shows picker dialog
        binding.chipCategory.setOnClickListener {
            showCategoryFilterPicker()
        }

        // Observe category filter to update chip text
        viewModel.categoryFilter.observe(this) { category ->
            if (category != null) {
                binding.chipCategory.text = category
                binding.chipCategory.isChecked = true
                binding.chipCategory.isCloseIconVisible = true
            } else {
                binding.chipCategory.text = getString(R.string.category)
                binding.chipCategory.isChecked = false
                binding.chipCategory.isCloseIconVisible = false
            }
        }
        binding.chipCategory.setOnCloseIconClickListener {
            viewModel.setCategoryFilter(null)
        }

        // Observe categories for the picker
        viewModel.allCategories.observe(this) { categories ->
            categoryNames = categories.map { it.name }
        }

        // Observe folders for the picker
        viewModel.allFolders.observe(this) { folders ->
            folderList = folders
            folderNames = folders.map { it.name }
        }
    }

    private fun showCategoryFilterPicker() {
        if (categoryNames.isEmpty()) {
            Toast.makeText(this, R.string.no_categories, Toast.LENGTH_SHORT).show()
            return
        }
        val items = categoryNames.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.filter_by_category)
            .setItems(items) { _, which ->
                viewModel.setCategoryFilter(items[which])
            }
            .setNeutralButton(R.string.clear_filter) { _, _ ->
                viewModel.setCategoryFilter(null)
            }
            .show()
    }

    private fun updateSummary(transactions: List<Transaction>) {
        val currencySymbol = SettingsActivity.getCurrencySymbol(this)
        val income = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val total = income - expense

        binding.tvBalance.text = String.format("%,.2f %s", total, currencySymbol)
        binding.tvIncome.text = String.format("%,.2f %s", income, currencySymbol)
        binding.tvExpense.text = String.format("%,.2f %s", expense, currencySymbol)
    }

    private fun showAddTransactionDialog() {
        val dialogBinding = DialogAddTransactionBinding.inflate(LayoutInflater.from(this))
        currentDialogBinding = dialogBinding
        pendingImagePath = null
        pendingImageUri = null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val calendar = Calendar.getInstance()
        var selectedDate = calendar.timeInMillis
        var selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        var selectedMinute = calendar.get(Calendar.MINUTE)

        dialogBinding.btnPickDate.text = dateFormat.format(Date(selectedDate))
        dialogBinding.btnPickTime.text = timeFormat.format(Date(selectedDate))

        // Setup category dropdown from DB
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        dialogBinding.actCategory.setAdapter(categoryAdapter)

        // Setup folder dropdown
        val folderOptions = mutableListOf(getString(R.string.no_folder))
        folderOptions.addAll(folderNames)
        val folderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, folderOptions)
        dialogBinding.actFolder.setAdapter(folderAdapter)

        // Attach image button
        dialogBinding.btnAttachImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Remove image button
        dialogBinding.btnRemoveImage.setOnClickListener {
            pendingImagePath?.let { path ->
                File(path).delete()
            }
            pendingImagePath = null
            pendingImageUri = null
            dialogBinding.cardImagePreview.visibility = View.GONE
        }

        // Toggle group syncs with hidden radio buttons
        dialogBinding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                dialogBinding.rbIncome.isChecked = checkedId == R.id.btn_type_income
                dialogBinding.rbExpense.isChecked = checkedId == R.id.btn_type_expense
            }
        }

        dialogBinding.btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(selectedDate)
                .build()

            datePicker.addOnPositiveButtonClickListener {
                selectedDate = it
                dialogBinding.btnPickDate.text = dateFormat.format(Date(it))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

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
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_transaction)
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
                val imagePath = pendingImagePath ?: ""

                if (amountText.isNotEmpty()) {
                    try {
                        val amount = amountText.toDouble()
                        // Combine date + time
                        val finalCalendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val transaction = Transaction(
                            amount = amount,
                            note = note,
                            date = finalCalendar.timeInMillis,
                            isIncome = isIncome,
                            category = category,
                            description = description,
                            createdAt = System.currentTimeMillis(),
                            folderId = folderId,
                            imagePath = imagePath
                        )
                        viewModel.insert(transaction)
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, R.string.enter_amount, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                // Clean up unused image if dialog cancelled
                pendingImagePath?.let { path ->
                    File(path).delete()
                }
                pendingImagePath = null
                currentDialogBinding = null
            }
            .setOnDismissListener {
                currentDialogBinding = null
            }
            .show()
    }

}
