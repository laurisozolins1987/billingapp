package com.example.billingapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billingapp.databinding.ActivityArchiveBinding

class ArchiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArchiveBinding
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarArchive.setNavigationOnClickListener { finish() }

        val adapter = ArchiveAdapter { transaction ->
            viewModel.unarchive(transaction)
            Toast.makeText(this, R.string.transaction_unarchived, Toast.LENGTH_SHORT).show()
        }

        binding.rvArchive.layoutManager = LinearLayoutManager(this)
        binding.rvArchive.adapter = adapter

        viewModel.archivedTransactions.observe(this) { transactions ->
            adapter.submitList(transactions)
            binding.tvEmptyArchive.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            binding.rvArchive.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
