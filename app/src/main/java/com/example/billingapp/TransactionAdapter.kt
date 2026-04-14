package com.example.billingapp

import android.view.LayoutInflater
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
        private val dateFormat = SimpleDateFormat("d. MMM yyyy", Locale.getDefault())

        fun bind(transaction: Transaction) {
            val context = binding.root.context
            val currencySymbol = SettingsActivity.getCurrencySymbol(context)

            binding.apply {
                tvNote.text = transaction.note.ifEmpty { if (transaction.isIncome) context.getString(R.string.income) else context.getString(R.string.expense) }
                tvDate.text = dateFormat.format(Date(transaction.date))

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
