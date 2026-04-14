package com.example.billingapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.billingapp.databinding.ActivityTransactionDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding
    private val viewModel: TransactionViewModel by viewModels()
    private var currentTransaction: Transaction? = null

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

        viewModel.getTransactionById(transactionId).observe(this) { transaction ->
            if (transaction != null) {
                currentTransaction = transaction
                displayTransaction(transaction)
            } else {
                finish()
            }
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
