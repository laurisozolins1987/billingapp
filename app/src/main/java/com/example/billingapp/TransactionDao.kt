package com.example.billingapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactionsSync(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): LiveData<Transaction?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}
