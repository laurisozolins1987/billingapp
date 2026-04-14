package com.example.billingapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billingapp.databinding.ActivityCalendarBinding
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var calendarAdapter: CalendarDayAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    private val displayCalendar = Calendar.getInstance()
    private var selectedDay: Int = -1
    private var allTransactions: List<Transaction> = emptyList()
    private var allBills: List<Bill> = emptyList()

    private val monthYearFormat = SimpleDateFormat("yyyy. 'gada' MMMM", Locale("lv"))
    private val dayMonthFormat = SimpleDateFormat("d. MMMM", Locale("lv"))

    override fun onCreate(savedInstanceState: Bundle?) {
        if (SettingsActivity.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCalendarGrid()
        setupTransactionList()
        setupNavigation()
        observeTransactions()

        // Select today by default
        selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCalendarGrid() {
        calendarAdapter = CalendarDayAdapter { day ->
            selectedDay = day
            refreshCalendar()
            showDayTransactions(day)
        }
        binding.rvCalendar.layoutManager = GridLayoutManager(this, 7)
        binding.rvCalendar.adapter = calendarAdapter
    }

    private fun setupTransactionList() {
        transactionAdapter = TransactionAdapter { transaction ->
            val intent = Intent(this, TransactionDetailActivity::class.java)
            intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_ID, transaction.id)
            startActivity(intent)
        }
        binding.rvDayTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvDayTransactions.adapter = transactionAdapter
    }

    private fun setupNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, -1)
            selectedDay = -1
            refreshCalendar()
            clearDaySelection()
        }
        binding.btnNextMonth.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, 1)
            selectedDay = -1
            refreshCalendar()
            clearDaySelection()
        }
    }

    private fun observeTransactions() {
        viewModel.allTransactions.observe(this) { transactions ->
            allTransactions = transactions
            refreshCalendar()
            if (selectedDay > 0) {
                showDayTransactions(selectedDay)
            } else {
                // Show today's transactions by default
                val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                val todayCal = Calendar.getInstance()
                if (todayCal.get(Calendar.YEAR) == displayCalendar.get(Calendar.YEAR) &&
                    todayCal.get(Calendar.MONTH) == displayCalendar.get(Calendar.MONTH)) {
                    selectedDay = today
                    showDayTransactions(today)
                    refreshCalendar()
                }
            }
        }

        viewModel.allBills.observe(this) { bills ->
            allBills = bills
            refreshCalendar()
            if (selectedDay > 0) {
                showDayTransactions(selectedDay)
            }
        }
    }

    private fun refreshCalendar() {
        // Update month/year title
        binding.tvMonthYear.text = monthYearFormat.format(displayCalendar.time)

        val year = displayCalendar.get(Calendar.YEAR)
        val month = displayCalendar.get(Calendar.MONTH)

        // Count transactions per day for this month
        val transactionCountsByDay = getTransactionCountsByDay(year, month)
        val billCountsByDay = getBillCountsByDay(year, month)

        // Determine today
        val todayCal = Calendar.getInstance()
        val isCurrentMonth = todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) == month
        val todayDay = if (isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1

        // Build calendar grid
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // Monday=1 based offset (Calendar.MONDAY=2, so adjust)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = offset + daysInMonth
        val rows = if (totalCells % 7 == 0) totalCells / 7 else totalCells / 7 + 1
        val gridSize = rows * 7

        val days = mutableListOf<CalendarDay>()

        // Empty cells before first day
        for (i in 0 until offset) {
            days.add(CalendarDay(day = 0))
        }

        // Actual days
        for (d in 1..daysInMonth) {
            days.add(
                CalendarDay(
                    day = d,
                    isToday = d == todayDay,
                    isSelected = d == selectedDay,
                    transactionCount = transactionCountsByDay[d] ?: 0,
                    billCount = billCountsByDay[d] ?: 0
                )
            )
        }

        // Trailing empty cells
        while (days.size < gridSize) {
            days.add(CalendarDay(day = 0))
        }

        calendarAdapter.submitList(days)
    }

    private fun getTransactionCountsByDay(year: Int, month: Int): Map<Int, Int> {
        val cal = Calendar.getInstance()
        val counts = mutableMapOf<Int, Int>()

        for (transaction in allTransactions) {
            cal.timeInMillis = transaction.date
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                counts[day] = (counts[day] ?: 0) + 1
            }
        }

        return counts
    }

    private fun getBillCountsByDay(year: Int, month: Int): Map<Int, Int> {
        val cal = Calendar.getInstance()
        val counts = mutableMapOf<Int, Int>()

        for (bill in allBills) {
            cal.timeInMillis = bill.dueDate
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                counts[day] = (counts[day] ?: 0) + 1
            }
        }

        return counts
    }

    private fun showDayTransactions(day: Int) {
        val year = displayCalendar.get(Calendar.YEAR)
        val month = displayCalendar.get(Calendar.MONTH)

        // Update selected date header
        val dateCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        binding.tvSelectedDate.text = dayMonthFormat.format(dateCal.time)

        // Filter transactions for this day
        val cal = Calendar.getInstance()
        val dayTransactions = allTransactions.filter { transaction ->
            cal.timeInMillis = transaction.date
            cal.get(Calendar.YEAR) == year &&
                    cal.get(Calendar.MONTH) == month &&
                    cal.get(Calendar.DAY_OF_MONTH) == day
        }.sortedByDescending { it.date }

        // Filter bills due on this day
        val dayBills = allBills.filter { bill ->
            cal.timeInMillis = bill.dueDate
            cal.get(Calendar.YEAR) == year &&
                    cal.get(Calendar.MONTH) == month &&
                    cal.get(Calendar.DAY_OF_MONTH) == day
        }

        val totalItems = dayTransactions.size + dayBills.size
        binding.tvDayTransactionCount.text = getString(R.string.transaction_count, totalItems)

        if (totalItems == 0) {
            binding.cardDaySummary.visibility = View.GONE
            binding.rvDayTransactions.visibility = View.GONE
            binding.tvDayEmpty.visibility = View.VISIBLE
        } else {
            binding.tvDayEmpty.visibility = View.GONE
            binding.cardDaySummary.visibility = View.VISIBLE
            binding.rvDayTransactions.visibility = View.VISIBLE

            // Calculate day summary (transactions only)
            val currencySymbol = SettingsActivity.getCurrencySymbol(this)
            val income = dayTransactions.filter { it.isIncome }.sumOf { it.amount }
            val expense = dayTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val billsTotal = dayBills.filter { !it.isPaid }.sumOf { it.amount }

            binding.tvDayIncome.text = String.format("+%,.2f %s", income, currencySymbol)
            binding.tvDayExpense.text = String.format("-%,.2f %s", expense + billsTotal, currencySymbol)

            transactionAdapter.submitList(dayTransactions)
        }
    }

    private fun clearDaySelection() {
        binding.tvSelectedDate.text = ""
        binding.tvDayTransactionCount.text = ""
        binding.cardDaySummary.visibility = View.GONE
        binding.rvDayTransactions.visibility = View.GONE
        binding.tvDayEmpty.visibility = View.VISIBLE
    }
}
