package com.example.billingapp

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billingapp.databinding.ActivityBillsBinding
import com.example.billingapp.databinding.DialogAddBillBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBillsBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: BillAdapter
    private var categoryNames: List<String> = emptyList()
    private var allBillsList: List<Bill> = emptyList()
    private var currentFilter = FilterType.ALL

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

    enum class FilterType { ALL, UNPAID, OVERDUE, PAID }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (SettingsActivity.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityBillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupObservers()

        binding.fabAddBill.setOnClickListener { showAddBillDialog() }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = BillAdapter { bill ->
            val intent = Intent(this, BillDetailActivity::class.java)
            intent.putExtra(BillDetailActivity.EXTRA_BILL_ID, bill.id)
            startActivity(intent)
        }
        binding.rvBills.layoutManager = LinearLayoutManager(this)
        binding.rvBills.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener { setFilter(FilterType.ALL) }
        binding.chipUnpaid.setOnClickListener { setFilter(FilterType.UNPAID) }
        binding.chipOverdue.setOnClickListener { setFilter(FilterType.OVERDUE) }
        binding.chipPaid.setOnClickListener { setFilter(FilterType.PAID) }
    }

    private fun setFilter(type: FilterType) {
        currentFilter = type
        binding.chipAll.isChecked = type == FilterType.ALL
        binding.chipUnpaid.isChecked = type == FilterType.UNPAID
        binding.chipOverdue.isChecked = type == FilterType.OVERDUE
        binding.chipPaid.isChecked = type == FilterType.PAID
        applyFilter()
    }

    private fun applyFilter() {
        val now = System.currentTimeMillis()
        val filtered = when (currentFilter) {
            FilterType.ALL -> allBillsList
            FilterType.UNPAID -> allBillsList.filter { !it.isPaid }
            FilterType.OVERDUE -> allBillsList.filter { !it.isPaid && it.dueDate < now }
            FilterType.PAID -> allBillsList.filter { it.isPaid }
        }
        adapter.submitList(filtered)
        binding.tvBillCount.text = getString(R.string.bill_count, filtered.size)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvBills.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupObservers() {
        viewModel.allBills.observe(this) { bills ->
            allBillsList = bills
            applyFilter()
            updateSummary(bills)
        }

        viewModel.allCategories.observe(this) { categories ->
            categoryNames = categories.map { it.name }
        }
    }

    private fun updateSummary(bills: List<Bill>) {
        val currencySymbol = SettingsActivity.getCurrencySymbol(this)
        val now = System.currentTimeMillis()
        val unpaidTotal = bills.filter { !it.isPaid }.sumOf { it.amount }
        val overdueCount = bills.count { !it.isPaid && it.dueDate < now }
        val paidTotal = bills.filter { it.isPaid }.sumOf { it.amount }

        binding.tvUnpaidAmount.text = String.format("%,.2f %s", unpaidTotal, currencySymbol)
        binding.tvOverdueCount.text = overdueCount.toString()
        binding.tvPaidAmount.text = String.format("%,.2f %s", paidTotal, currencySymbol)
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

    private fun showAddBillDialog() {
        val dialogBinding = DialogAddBillBinding.inflate(LayoutInflater.from(this))
        currentDialogBinding = dialogBinding
        pendingImagePath = null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val calendar = Calendar.getInstance()
        // Default due date: +30 days
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        var selectedDate = calendar.timeInMillis
        var selectedHour = 12
        var selectedMinute = 0

        dialogBinding.btnPickDate.text = dateFormat.format(Date(selectedDate))
        dialogBinding.btnPickTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)

        // Category dropdown
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        dialogBinding.actCategory.setAdapter(categoryAdapter)

        // Recurring interval dropdown
        val intervals = arrayOf(
            getString(R.string.bill_weekly),
            getString(R.string.bill_monthly),
            getString(R.string.bill_yearly)
        )
        val intervalAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, intervals)
        dialogBinding.actInterval.setAdapter(intervalAdapter)

        // Reminder days dropdown
        val reminderDayOptions = arrayOf(
            getString(R.string.bill_reminder_same_day),
            getString(R.string.bill_reminder_1_day),
            getString(R.string.bill_reminder_3_days),
            getString(R.string.bill_reminder_7_days)
        )
        val reminderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reminderDayOptions)
        dialogBinding.actReminderDays.setAdapter(reminderAdapter)

        // Toggle recurring visibility
        dialogBinding.switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilInterval.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Toggle reminder visibility
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
            pendingImagePath?.let { path -> File(path).delete() }
            pendingImagePath = null
            dialogBinding.cardImagePreview.visibility = View.GONE
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_bill)
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

                if (title.isEmpty()) {
                    Toast.makeText(this, R.string.bill_enter_title, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (amountText.isEmpty()) {
                    Toast.makeText(this, R.string.enter_amount, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

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
                    } else 1

                    val bill = Bill(
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

                    viewModel.insertBill(bill)

                    // Schedule reminder if enabled
                    if (reminderEnabled) {
                        BillReminderManager.scheduleReminder(this, bill)
                    }

                } catch (e: NumberFormatException) {
                    Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                pendingImagePath?.let { path -> File(path).delete() }
                pendingImagePath = null
                currentDialogBinding = null
            }
            .setOnDismissListener { currentDialogBinding = null }
            .show()
    }
}
