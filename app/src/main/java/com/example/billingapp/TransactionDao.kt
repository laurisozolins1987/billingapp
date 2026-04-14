package com.example.billingapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactionsSync(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND (note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY date DESC")
    fun searchTransactions(query: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): LiveData<Transaction?>

    // Trash queries
    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTransactions(): LiveData<List<Transaction>>

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 0, deletedAt = 0 WHERE id = :id")
    suspend fun restore(id: Int)

    @Query("DELETE FROM transactions WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}
