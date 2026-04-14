package com.example.billingapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val note: String,
    val date: Long,
    val isIncome: Boolean,
    val category: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0,
    val folderId: Int = 0,
    val imagePath: String = "",
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false
)
