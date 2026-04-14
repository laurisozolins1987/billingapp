package com.example.billingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.billingapp.databinding.ItemCalendarDayBinding

data class CalendarDay(
    val day: Int,           // 0 = empty cell
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val transactionCount: Int = 0,
    val billCount: Int = 0
)

class CalendarDayAdapter(
    private val onDayClick: (Int) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun submitList(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class DayViewHolder(private val binding: ItemCalendarDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: CalendarDay) {
            val context = binding.root.context

            if (day.day == 0) {
                binding.tvDay.text = ""
                binding.tvBadge.visibility = View.GONE
                binding.dayBackground.visibility = View.GONE
                binding.root.isClickable = false
                return
            }

            binding.tvDay.text = day.day.toString()
            binding.root.isClickable = true
            binding.root.setOnClickListener { onDayClick(day.day) }

            // Selected state
            if (day.isSelected) {
                binding.dayBackground.visibility = View.VISIBLE
                binding.dayBackground.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                binding.tvDay.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else if (day.isToday) {
                binding.dayBackground.visibility = View.VISIBLE
                binding.dayBackground.setBackgroundResource(R.drawable.bg_calendar_today)
                binding.tvDay.setTextColor(ContextCompat.getColor(context, R.color.primary))
            } else {
                binding.dayBackground.visibility = View.GONE
                binding.tvDay.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            }

            // Transaction count badge
            val totalCount = day.transactionCount + day.billCount
            if (totalCount > 0 && !day.isSelected) {
                binding.tvBadge.visibility = View.VISIBLE
                binding.tvBadge.text = if (totalCount > 9) "9+" else totalCount.toString()
                // Use different badge color if there are bills
                if (day.billCount > 0 && day.transactionCount == 0) {
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_bill_badge_calendar)
                } else {
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_badge)
                }
            } else {
                binding.tvBadge.visibility = View.GONE
            }
        }
    }
}
