package com.example.billingapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billingapp.databinding.ItemArchiveBinding
import java.text.SimpleDateFormat
import java.util.*

class ArchiveAdapter(
    private val onUnarchiveClick: (Transaction) -> Unit
) : ListAdapter<Transaction, ArchiveAdapter.ArchiveViewHolder>(ArchiveDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveViewHolder {
        val binding = ItemArchiveBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArchiveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArchiveViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArchiveViewHolder(private val binding: ItemArchiveBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("d. MMM yyyy", Locale.getDefault())

        fun bind(transaction: Transaction) {
            val context = binding.root.context
            val currencySymbol = SettingsActivity.getCurrencySymbol(context)

            binding.tvArchiveNote.text = transaction.note.ifEmpty {
                if (transaction.isIncome) context.getString(R.string.income) else context.getString(R.string.expense)
            }
            binding.tvArchiveDate.text = dateFormat.format(Date(transaction.date))

            if (transaction.isIncome) {
                binding.tvArchiveAmount.text = String.format("+%,.2f %s", transaction.amount, currencySymbol)
                binding.tvArchiveAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                binding.iconContainerArchive.setBackgroundResource(R.drawable.bg_icon_income)
                binding.ivArchiveType.setImageResource(R.drawable.ic_income)
            } else {
                binding.tvArchiveAmount.text = String.format("-%,.2f %s", transaction.amount, currencySymbol)
                binding.tvArchiveAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                binding.iconContainerArchive.setBackgroundResource(R.drawable.bg_icon_expense)
                binding.ivArchiveType.setImageResource(R.drawable.ic_expense)
            }

            binding.btnUnarchive.setOnClickListener { onUnarchiveClick(transaction) }
        }
    }

    class ArchiveDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem == newItem
    }
}
