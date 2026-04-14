package com.example.billingapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY isPaid ASC, dueDate ASC")
    fun getAllBills(): LiveData<List<Bill>>

    @Query("SELECT * FROM bills ORDER BY isPaid ASC, dueDate ASC")
    fun getAllBillsSync(): List<Bill>

    @Query("SELECT * FROM bills WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidBills(): LiveData<List<Bill>>

    @Query("SELECT * FROM bills WHERE isPaid = 0 AND dueDate < :now ORDER BY dueDate ASC")
    fun getOverdueBills(now: Long = System.currentTimeMillis()): LiveData<List<Bill>>

    @Query("SELECT * FROM bills WHERE isPaid = 1 ORDER BY paidAt DESC")
    fun getPaidBills(): LiveData<List<Bill>>

    @Query("SELECT * FROM bills WHERE isPaid = 0 AND dueDate >= :startOfDay AND dueDate <= :endOfDay")
    fun getBillsDueOnDate(startOfDay: Long, endOfDay: Long): LiveData<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    fun getBillById(id: Int): LiveData<Bill?>

    @Query("SELECT COUNT(*) FROM bills WHERE isPaid = 0")
    fun getUnpaidCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM bills WHERE isPaid = 0 AND dueDate < :now")
    fun getOverdueCount(now: Long = System.currentTimeMillis()): LiveData<Int>

    @Query("UPDATE bills SET isPaid = 1, paidAt = :paidAt, paidLocation = :location WHERE id = :id")
    suspend fun markAsPaid(id: Int, paidAt: Long = System.currentTimeMillis(), location: String = "")

    @Query("UPDATE bills SET isPaid = 0, paidAt = 0, paidLocation = '' WHERE id = :id")
    suspend fun markAsUnpaid(id: Int)

    @Query("SELECT * FROM bills WHERE reminderEnabled = 1 AND isPaid = 0")
    fun getBillsWithReminders(): List<Bill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: Bill): Long

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)
}
