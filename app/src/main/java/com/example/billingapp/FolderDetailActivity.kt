package com.example.billingapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billingapp.databinding.ActivityFolderDetailBinding

class FolderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderDetailBinding
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val folderId = intent.getIntExtra(FoldersActivity.EXTRA_FOLDER_ID, -1)
        val folderName = intent.getStringExtra(FoldersActivity.EXTRA_FOLDER_NAME) ?: ""

        binding.toolbarFolderDetail.title = folderName
        binding.toolbarFolderDetail.setNavigationOnClickListener { finish() }

        if (folderId == -1) {
            finish()
            return
        }

        val adapter = TransactionAdapter { transaction ->
            val intent = Intent(this, TransactionDetailActivity::class.java)
            intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_ID, transaction.id)
            startActivity(intent)
        }

        binding.rvFolderTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvFolderTransactions.adapter = adapter

        viewModel.getTransactionsByFolder(folderId).observe(this) { transactions ->
            adapter.submitList(transactions)
            binding.tvEmptyFolder.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            binding.rvFolderTransactions.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
