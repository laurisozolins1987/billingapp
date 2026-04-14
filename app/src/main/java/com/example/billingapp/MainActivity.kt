package com.example.billingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.billingapp.databinding.ActivityMainBinding
import com.example.billingapp.databinding.DialogAddTransactionBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.util.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TransactionViewModel by viewModels()
    private val adapter = TransactionAdapter()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = adapter.currentList[position]
                viewModel.delete(transaction)
                Snackbar.make(binding.root, R.string.transaction_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.insert(transaction) }
                    .show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvTransactions)
    }

    private fun setupObservers() {
        viewModel.filteredTransactions.observe(this) { transactions ->
            adapter.submitList(transactions)
            updateBalance(transactions)
            binding.tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            binding.rvTransactions.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            showAddTransactionDialog()
        }

        binding.btnFilterDate.setOnClickListener {
            showDatePicker()
        }
        
        binding.btnFilterDate.setOnLongClickListener {
            viewModel.clearFilter()
            binding.btnFilterDate.setText(R.string.select_date)
            Toast.makeText(this, "Filtrs notīrīts", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun updateBalance(transactions: List<Transaction>) {
        val total = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }
        binding.tvBalance.text = getString(R.string.balance, total)
    }

    private fun showAddTransactionDialog() {
        val dialogBinding = DialogAddTransactionBinding.inflate(LayoutInflater.from(this))
        var selectedDate = System.currentTimeMillis()
        dialogBinding.btnPickDate.text = dateFormat.format(Date(selectedDate))

        dialogBinding.btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener {
                selectedDate = it
                dialogBinding.btnPickDate.text = dateFormat.format(Date(it))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_transaction)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val amountText = dialogBinding.etAmount.text.toString()
                val note = dialogBinding.etNote.text.toString()
                val isIncome = dialogBinding.rbIncome.isChecked

                if (amountText.isNotEmpty()) {
                    try {
                        val amount = amountText.toDouble()
                        val transaction = Transaction(
                            amount = amount,
                            note = note,
                            date = selectedDate,
                            isIncome = isIncome
                        )
                        viewModel.insert(transaction)
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Nederīgs summas formāts", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ievadiet summu", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDatePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.select_date)
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            if (selection.first != null && selection.second != null) {
                viewModel.setDateFilter(selection.first!!, selection.second!!)
                val start = dateFormat.format(Date(selection.first!!))
                val end = dateFormat.format(Date(selection.second!!))
                binding.btnFilterDate.text = "$start - $end (Dzēst ar garu nospiedienu)"
            }
        }

        dateRangePicker.show(supportFragmentManager, "RANGE_PICKER")
    }
}
