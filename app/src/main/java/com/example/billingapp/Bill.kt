package com.example.billingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val category: String = "",
    val description: String = "",
    val isPaid: Boolean = false,
    val paidAt: Long = 0,
    val paidLocation: String = "",
    val isRecurring: Boolean = false,
    val recurringInterval: String = "",
    val reminderEnabled: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String = "",
    val invoiceNumber: String = "",
    val receivedDate: Long = 0
)
