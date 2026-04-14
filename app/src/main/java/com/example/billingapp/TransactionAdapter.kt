package com.example.billingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billingapp.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit = {}
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
        holder.itemView.setOnClickListener { onItemClick(transaction) }
    }

    class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateTimeFormat = SimpleDateFormat("d. MMM yyyy, HH:mm", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(transaction: Transaction) {
            val context = binding.root.context
            val currencySymbol = SettingsActivity.getCurrencySymbol(context)

            binding.apply {
                tvNote.text = transaction.note.ifEmpty { if (transaction.isIncome) context.getString(R.string.income) else context.getString(R.string.expense) }
                tvDate.text = dateTimeFormat.format(Date(transaction.date))

                // Category
                if (transaction.category.isNotEmpty()) {
                    tvCategory.text = transaction.category
                    tvCategory.visibility = View.VISIBLE
                } else {
                    tvCategory.visibility = View.GONE
                }

                // Bookmark indicator
                ivBookmark.visibility = if (transaction.isBookmarked) View.VISIBLE else View.GONE

                // Created time
                if (transaction.createdAt > 0) {
                    tvCreatedTime.text = context.getString(R.string.created_short, timeFormat.format(Date(transaction.createdAt)))
                    tvCreatedTime.visibility = View.VISIBLE
                } else {
                    tvCreatedTime.visibility = View.GONE
                }

                if (transaction.isIncome) {
                    tvAmount.text = String.format("+%,.2f %s", transaction.amount, currencySymbol)
                    tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                    iconContainer.setBackgroundResource(R.drawable.bg_icon_income)
                    ivTransactionType.setImageResource(R.drawable.ic_income)
                } else {
                    tvAmount.text = String.format("-%,.2f %s", transaction.amount, currencySymbol)
                    tvAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                    iconContainer.setBackgroundResource(R.drawable.bg_icon_expense)
                    ivTransactionType.setImageResource(R.drawable.ic_expense)
                }
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
