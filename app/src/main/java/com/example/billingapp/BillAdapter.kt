package com.example.billingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billingapp.databinding.ItemBillBinding
import java.text.SimpleDateFormat
import java.util.*

class BillAdapter(
    private val onItemClick: (Bill) -> Unit = {}
) : ListAdapter<Bill, BillAdapter.BillViewHolder>(BillDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        val bill = getItem(position)
        holder.bind(bill)
        holder.itemView.setOnClickListener { onItemClick(bill) }
    }

    class BillViewHolder(private val binding: ItemBillBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("d. MMM yyyy, HH:mm", Locale.getDefault())

        fun bind(bill: Bill) {
            val context = binding.root.context
            val currencySymbol = SettingsActivity.getCurrencySymbol(context)

            binding.apply {
                tvBillTitle.text = bill.title
                tvBillAmount.text = String.format("%,.2f %s", bill.amount, currencySymbol)
                tvBillDueDate.text = context.getString(R.string.bill_due_prefix, dateFormat.format(Date(bill.dueDate)))

                // Category
                if (bill.category.isNotEmpty()) {
                    tvBillCategory.text = bill.category
                    tvBillCategory.visibility = View.VISIBLE
                } else {
                    tvBillCategory.visibility = View.GONE
                }

                if (bill.invoiceNumber.isNotEmpty()) {
                    tvBillInvoiceNumber.text = "Nr. ${bill.invoiceNumber}"
                    tvBillInvoiceNumber.visibility = View.VISIBLE
                } else {
                    tvBillInvoiceNumber.visibility = View.GONE
                }

                // Recurring indicator
                ivRecurring.visibility = if (bill.isRecurring) View.VISIBLE else View.GONE
                // Reminder indicator
                ivReminder.visibility = if (bill.reminderEnabled) View.VISIBLE else View.GONE

                // Status styling
                val now = System.currentTimeMillis()
                when {
                    bill.isPaid -> {
                        tvBillStatus.text = context.getString(R.string.bill_paid)
                        tvBillStatus.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                        tvBillAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                        ivBillStatus.setImageResource(R.drawable.ic_paid)
                        ivBillStatus.setColorFilter(ContextCompat.getColor(context, R.color.income_green))
                    }
                    bill.dueDate < now -> {
                        tvBillStatus.text = context.getString(R.string.bill_overdue)
                        tvBillStatus.setTextColor(ContextCompat.getColor(context, R.color.bill_overdue))
                        tvBillAmount.setTextColor(ContextCompat.getColor(context, R.color.bill_overdue))
                        ivBillStatus.setImageResource(R.drawable.ic_overdue)
                        ivBillStatus.setColorFilter(ContextCompat.getColor(context, R.color.bill_overdue))
                    }
                    else -> {
                        tvBillStatus.text = context.getString(R.string.bill_unpaid)
                        tvBillStatus.setTextColor(ContextCompat.getColor(context, R.color.bill_blue))
                        tvBillAmount.setTextColor(ContextCompat.getColor(context, R.color.bill_blue))
                        ivBillStatus.setImageResource(R.drawable.ic_bill)
                        ivBillStatus.setColorFilter(ContextCompat.getColor(context, R.color.bill_blue))
                    }
                }
            }
        }
    }

    class BillDiffCallback : DiffUtil.ItemCallback<Bill>() {
        override fun areItemsTheSame(oldItem: Bill, newItem: Bill) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bill, newItem: Bill) = oldItem == newItem
    }
}
