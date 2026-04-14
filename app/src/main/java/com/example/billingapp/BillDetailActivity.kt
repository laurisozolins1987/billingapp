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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.billingapp.databinding.ActivityBillDetailBinding
import com.example.billingapp.databinding.DialogAddBillBinding
import com.example.billingapp.databinding.DialogPayBillBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BillDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBillDetailBinding
    private val viewModel: TransactionViewModel by viewModels()
    private var currentBill: Bill? = null
    private var categoryNames: List<String> = emptyList()
    private var pendingImagePath: String? = null
    private var currentDialogBinding: DialogAddBillBinding? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
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

    companion object {
        const val EXTRA_BILL_ID = "bill_id"
    }

    private val dateTimeFormat = SimpleDateFormat("d. MMMM yyyy, HH:mm", Locale("lv"))
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        if (SettingsActivity.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityBillDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarDetail.setNavigationOnClickListener { finish() }

        val billId = intent.getIntExtra(EXTRA_BILL_ID, -1)
        if (billId == -1) { finish(); return }

        viewModel.allCategories.observe(this) { categories ->
            categoryNames = categories.map { it.name }
        }

        viewModel.getBillById(billId).observe(this) { bill ->
            if (bill != null) {
                currentBill = bill
                displayBill(bill)
            }
        }

        binding.btnEdit.setOnClickListener { currentBill?.let { showEditDialog(it) } }

        binding.btnPay.setOnClickListener {
            currentBill?.let { bill ->
                if (bill.isPaid) {
                    // Mark as unpaid
                    viewModel.markBillAsUnpaid(bill.id)
                    Toast.makeText(this, R.string.bill_marked_unpaid, Toast.LENGTH_SHORT).show()
                } else {
                    showPayDialog(bill)
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            currentBill?.let { bill ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.bill_delete)
                    .setMessage(R.string.bill_delete_confirm)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteBill(bill)
                        finish()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun displayBill(bill: Bill) {
        val currencySymbol = SettingsActivity.getCurrencySymbol(this)
        val now = System.currentTimeMillis()

        binding.tvDetailAmount.text = String.format("%,.2f %s", bill.amount, currencySymbol)
        binding.tvDetailTitle.text = bill.title
        binding.tvDetailCategory.text = bill.category.ifEmpty { getString(R.string.no_category) }
        binding.tvDetailDescription.text = bill.description.ifEmpty { getString(R.string.no_description) }
        binding.tvDetailDueDate.text = dateTimeFormat.format(Date(bill.dueDate))

        // Recurring info
        binding.tvDetailRecurring.text = if (bill.isRecurring) {
            when (bill.recurringInterval) {
                "weekly" -> getString(R.string.bill_weekly)
                "monthly" -> getString(R.string.bill_monthly)
                "yearly" -> getString(R.string.bill_yearly)
                else -> getString(R.string.bill_no)
            }
        } else {
            getString(R.string.bill_no)
        }

        // Reminder info
        binding.tvDetailReminder.text = if (bill.reminderEnabled) {
            when (bill.reminderDaysBefore) {
                0 -> getString(R.string.bill_reminder_same_day)
                1 -> getString(R.string.bill_reminder_1_day)
                3 -> getString(R.string.bill_reminder_3_days)
                7 -> getString(R.string.bill_reminder_7_days)
                else -> getString(R.string.bill_reminder_1_day)
            }
        } else {
            getString(R.string.bill_no)
        }

        // Status styling
        when {
            bill.isPaid -> {
                binding.tvDetailStatus.text = getString(R.string.bill_paid)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.income_green))
                binding.tvDetailAmount.setTextColor(ContextCompat.getColor(this, R.color.income_green))
                binding.ivStatusIcon.setImageResource(R.drawable.ic_paid)
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.income_green))
                binding.btnPay.text = getString(R.string.bill_mark_unpaid)
                binding.btnPay.setBackgroundColor(ContextCompat.getColor(this, R.color.bill_blue))
                binding.btnPay.setIconResource(R.drawable.ic_recurring)

                // Show payment info
                binding.cardPayment.visibility = View.VISIBLE
                binding.tvDetailPaidAt.text = if (bill.paidAt > 0) {
                    dateTimeFormat.format(Date(bill.paidAt))
                } else {
                    getString(R.string.unknown)
                }
                binding.tvDetailPaidLocation.text = bill.paidLocation.ifEmpty { getString(R.string.bill_no_location) }
            }
            bill.dueDate < now -> {
                binding.tvDetailStatus.text = getString(R.string.bill_overdue)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.bill_overdue))
                binding.tvDetailAmount.setTextColor(ContextCompat.getColor(this, R.color.bill_overdue))
                binding.ivStatusIcon.setImageResource(R.drawable.ic_overdue)
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.bill_overdue))
                binding.btnPay.text = getString(R.string.bill_mark_paid)
                binding.btnPay.setBackgroundColor(ContextCompat.getColor(this, R.color.income_green))
                binding.cardPayment.visibility = View.GONE
            }
            else -> {
                binding.tvDetailStatus.text = getString(R.string.bill_unpaid)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.bill_blue))
                binding.tvDetailAmount.setTextColor(ContextCompat.getColor(this, R.color.bill_blue))
                binding.ivStatusIcon.setImageResource(R.drawable.ic_bill)
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.bill_blue))
                binding.btnPay.text = getString(R.string.bill_mark_paid)
                binding.btnPay.setBackgroundColor(ContextCompat.getColor(this, R.color.income_green))
                binding.cardPayment.visibility = View.GONE
            }
        }

        // Receipt image
        if (bill.imagePath.isNotEmpty()) {
            val file = File(bill.imagePath)
            if (file.exists()) {
                binding.cardReceipt.visibility = View.VISIBLE
                binding.ivReceipt.setImageBitmap(BitmapFactory.decodeFile(bill.imagePath))
            } else {
                binding.cardReceipt.visibility = View.GONE
            }
        } else {
            binding.cardReceipt.visibility = View.GONE
        }
    }

    private fun showPayDialog(bill: Bill) {
        val dialogBinding = DialogPayBillBinding.inflate(LayoutInflater.from(this))

        val calendar = Calendar.getInstance()
        var selectedDate = calendar.timeInMillis
        var selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        var selectedMinute = calendar.get(Calendar.MINUTE)

        dialogBinding.btnPickDate.text = dateFormat.format(Date(selectedDate))
        dialogBinding.btnPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)

        dialogBinding.btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.bill_paid_at)
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
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTheme(R.style.ThemeOverlay_Billingapp_TimePicker)
                .build()
            timePicker.addOnPositiveButtonClickListener {
                selectedHour = timePicker.hour
                selectedMinute = timePicker.minute
                dialogBinding.btnPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            }
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bill_mark_paid)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.bill_mark_paid) { _, _ ->
                val finalCalendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val location = dialogBinding.etLocation.text.toString().trim()
                viewModel.markBillAsPaid(bill.id, finalCalendar.timeInMillis, location)
                Toast.makeText(this, R.string.bill_marked_paid, Toast.LENGTH_SHORT).show()

                // If recurring, create next bill
                if (bill.isRecurring && bill.recurringInterval.isNotEmpty()) {
                    createNextRecurringBill(bill)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createNextRecurringBill(bill: Bill) {
        val nextDueCalendar = Calendar.getInstance().apply {
            timeInMillis = bill.dueDate
        }
        when (bill.recurringInterval) {
            "weekly" -> nextDueCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> nextDueCalendar.add(Calendar.MONTH, 1)
            "yearly" -> nextDueCalendar.add(Calendar.YEAR, 1)
        }

        val nextBill = bill.copy(
            id = 0,
            dueDate = nextDueCalendar.timeInMillis,
            isPaid = false,
            paidAt = 0,
            paidLocation = "",
            createdAt = System.currentTimeMillis()
        )
        viewModel.insertBill(nextBill)

        if (nextBill.reminderEnabled) {
            BillReminderManager.scheduleReminder(this, nextBill)
        }

        Toast.makeText(this, R.string.bill_next_created, Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(bill: Bill) {
        val dialogBinding = DialogAddBillBinding.inflate(LayoutInflater.from(this))
        currentDialogBinding = dialogBinding
        pendingImagePath = if (bill.imagePath.isNotEmpty()) bill.imagePath else null

        val calendar = Calendar.getInstance().apply { timeInMillis = bill.dueDate }
        var selectedDate = bill.dueDate
        var selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        var selectedMinute = calendar.get(Calendar.MINUTE)

        // Pre-fill
        dialogBinding.etTitle.setText(bill.title)
        dialogBinding.etAmount.setText(bill.amount.toString())
        dialogBinding.actCategory.setText(bill.category, false)
        dialogBinding.etDescription.setText(bill.description)
        dialogBinding.btnPickDate.text = dateFormat.format(Date(selectedDate))
        dialogBinding.btnPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
        dialogBinding.switchRecurring.isChecked = bill.isRecurring
        dialogBinding.switchReminder.isChecked = bill.reminderEnabled

        if (bill.isRecurring) {
            dialogBinding.tilInterval.visibility = View.VISIBLE
            dialogBinding.actInterval.setText(
                when (bill.recurringInterval) {
                    "weekly" -> getString(R.string.bill_weekly)
                    "monthly" -> getString(R.string.bill_monthly)
                    "yearly" -> getString(R.string.bill_yearly)
                    else -> ""
                }, false
            )
        }

        if (bill.reminderEnabled) {
            dialogBinding.tilReminderDays.visibility = View.VISIBLE
            dialogBinding.actReminderDays.setText(
                when (bill.reminderDaysBefore) {
                    0 -> getString(R.string.bill_reminder_same_day)
                    1 -> getString(R.string.bill_reminder_1_day)
                    3 -> getString(R.string.bill_reminder_3_days)
                    7 -> getString(R.string.bill_reminder_7_days)
                    else -> getString(R.string.bill_reminder_1_day)
                }, false
            )
        }

        if (bill.imagePath.isNotEmpty()) {
            val file = File(bill.imagePath)
            if (file.exists()) {
                dialogBinding.cardImagePreview.visibility = View.VISIBLE
                dialogBinding.ivImagePreview.setImageBitmap(BitmapFactory.decodeFile(bill.imagePath))
            }
        }

        // Category dropdown
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        dialogBinding.actCategory.setAdapter(categoryAdapter)

        // Interval dropdown
        val intervals = arrayOf(getString(R.string.bill_weekly), getString(R.string.bill_monthly), getString(R.string.bill_yearly))
        dialogBinding.actInterval.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, intervals))

        // Reminder days dropdown
        val reminderDayOptions = arrayOf(
            getString(R.string.bill_reminder_same_day), getString(R.string.bill_reminder_1_day),
            getString(R.string.bill_reminder_3_days), getString(R.string.bill_reminder_7_days)
        )
        dialogBinding.actReminderDays.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reminderDayOptions))

        dialogBinding.switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilInterval.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        dialogBinding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilReminderDays.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        dialogBinding.btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.bill_due_date)
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
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTheme(R.style.ThemeOverlay_Billingapp_TimePicker)
                .build()
            timePicker.addOnPositiveButtonClickListener {
                selectedHour = timePicker.hour
                selectedMinute = timePicker.minute
                dialogBinding.btnPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            }
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        dialogBinding.btnAttachImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        dialogBinding.btnRemoveImage.setOnClickListener {
            pendingImagePath?.let { path ->
                if (path != bill.imagePath) File(path).delete()
            }
            pendingImagePath = null
            dialogBinding.cardImagePreview.visibility = View.GONE
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bill_edit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = dialogBinding.etTitle.text.toString().trim()
                val amountText = dialogBinding.etAmount.text.toString()
                val category = dialogBinding.actCategory.text.toString()
                val description = dialogBinding.etDescription.text.toString()
                val isRecurring = dialogBinding.switchRecurring.isChecked
                val intervalText = dialogBinding.actInterval.text.toString()
                val reminderEnabled = dialogBinding.switchReminder.isChecked
                val reminderDaysText = dialogBinding.actReminderDays.text.toString()

                if (title.isEmpty() || amountText.isEmpty()) return@setPositiveButton

                try {
                    val amount = amountText.toDouble()
                    val finalCalendar = Calendar.getInstance().apply {
                        timeInMillis = selectedDate
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val recurringInterval = if (isRecurring) {
                        when (intervalText) {
                            getString(R.string.bill_weekly) -> "weekly"
                            getString(R.string.bill_monthly) -> "monthly"
                            getString(R.string.bill_yearly) -> "yearly"
                            else -> ""
                        }
                    } else ""

                    val reminderDays = if (reminderEnabled) {
                        when (reminderDaysText) {
                            getString(R.string.bill_reminder_same_day) -> 0
                            getString(R.string.bill_reminder_1_day) -> 1
                            getString(R.string.bill_reminder_3_days) -> 3
                            getString(R.string.bill_reminder_7_days) -> 7
                            else -> 1
                        }
                    } else bill.reminderDaysBefore

                    val updatedBill = bill.copy(
                        title = title,
                        amount = amount,
                        dueDate = finalCalendar.timeInMillis,
                        category = category,
                        description = description,
                        isRecurring = isRecurring,
                        recurringInterval = recurringInterval,
                        reminderEnabled = reminderEnabled,
                        reminderDaysBefore = reminderDays,
                        imagePath = pendingImagePath ?: ""
                    )
                    viewModel.updateBill(updatedBill)

                    if (reminderEnabled) {
                        BillReminderManager.scheduleReminder(this, updatedBill)
                    } else {
                        BillReminderManager.cancelReminder(this, updatedBill.id)
                    }

                    Toast.makeText(this, R.string.bill_updated, Toast.LENGTH_SHORT).show()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                pendingImagePath?.let { path ->
                    if (path != bill.imagePath) File(path).delete()
                }
                currentDialogBinding = null
            }
            .setOnDismissListener { currentDialogBinding = null }
            .show()
    }

    private fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val dir = File(filesDir, "receipts")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "bill_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output -> inputStream.copyTo(output) }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
