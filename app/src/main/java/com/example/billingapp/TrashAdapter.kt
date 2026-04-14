package com.example.billingapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billingapp.databinding.ItemTrashBinding
import java.text.SimpleDateFormat
import java.util.*

class TrashAdapter(
    private val onRestore: (Transaction) -> Unit,
    private val onDelete: (Transaction) -> Unit
) : ListAdapter<Transaction, TrashAdapter.TrashViewHolder>(TrashDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val binding = ItemTrashBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrashViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrashViewHolder(private val binding: ItemTrashBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateTimeFormat = SimpleDateFormat("d. MMM yyyy, HH:mm", Locale.getDefault())
        private val deletedFormat = SimpleDateFormat("d. MMM HH:mm", Locale.getDefault())

        fun bind(transaction: Transaction) {
            val context = binding.root.context
            val currencySymbol = SettingsActivity.getCurrencySymbol(context)

            binding.tvNote.text = transaction.note.ifEmpty {
                if (transaction.isIncome) context.getString(R.string.income) else context.getString(R.string.expense)
            }
            binding.tvDate.text = dateTimeFormat.format(Date(transaction.date))

            if (transaction.deletedAt > 0) {
                binding.tvDeletedAt.text = context.getString(R.string.deleted_at, deletedFormat.format(Date(transaction.deletedAt)))
            }

            if (transaction.isIncome) {
                binding.tvAmount.text = String.format("+%,.2f %s", transaction.amount, currencySymbol)
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                binding.iconContainer.setBackgroundResource(R.drawable.bg_icon_income)
                binding.ivTransactionType.setImageResource(R.drawable.ic_income)
            } else {
                binding.tvAmount.text = String.format("-%,.2f %s", transaction.amount, currencySymbol)
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                binding.iconContainer.setBackgroundResource(R.drawable.bg_icon_expense)
                binding.ivTransactionType.setImageResource(R.drawable.ic_expense)
            }

            binding.btnRestore.setOnClickListener { onRestore(transaction) }
            binding.btnPermanentDelete.setOnClickListener { onDelete(transaction) }
        }
    }

    class TrashDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem == newItem
    }
}
