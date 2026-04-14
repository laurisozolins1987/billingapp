package com.example.billingapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billingapp.databinding.ActivityTrashBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrashBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var trashAdapter: TrashAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        if (SettingsActivity.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarTrash.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        observeTrash()

        binding.btnEmptyTrash.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.empty_trash)
                .setMessage(R.string.empty_trash_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.emptyTrash()
                    Toast.makeText(this, R.string.trash_emptied, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun setupRecyclerView() {
        trashAdapter = TrashAdapter(
            onRestore = { transaction ->
                viewModel.restore(transaction)
                Toast.makeText(this, R.string.transaction_restored, Toast.LENGTH_SHORT).show()
            },
            onDelete = { transaction ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_transaction)
                    .setMessage(R.string.permanent_delete_confirm)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.permanentDelete(transaction)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        binding.rvTrash.layoutManager = LinearLayoutManager(this)
        binding.rvTrash.adapter = trashAdapter
    }

    private fun observeTrash() {
        viewModel.deletedTransactions.observe(this) { transactions ->
            trashAdapter.submitList(transactions)
            binding.tvTrashCount.text = getString(R.string.transaction_count, transactions.size)

            if (transactions.isEmpty()) {
                binding.rvTrash.visibility = View.GONE
                binding.tvTrashEmpty.visibility = View.VISIBLE
                binding.btnEmptyTrash.visibility = View.GONE
            } else {
                binding.rvTrash.visibility = View.VISIBLE
                binding.tvTrashEmpty.visibility = View.GONE
                binding.btnEmptyTrash.visibility = View.VISIBLE
            }
        }
    }
}
