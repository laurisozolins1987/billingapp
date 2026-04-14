package com.example.billingapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isArchived = 0 ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isArchived = 0 ORDER BY date DESC")
    fun getAllTransactionsSync(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isArchived = 0 AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isArchived = 0 AND (note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY date DESC")
    fun searchTransactions(query: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): LiveData<Transaction?>

    // Bookmark queries
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isArchived = 0 AND isBookmarked = 1 ORDER BY date DESC")
    fun getBookmarkedTransactions(): LiveData<List<Transaction>>

    @Query("UPDATE transactions SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: Int, bookmarked: Boolean)

    // Archive queries
    @Query("SELECT * FROM transactions WHERE isArchived = 1 AND isDeleted = 0 ORDER BY date DESC")
    fun getArchivedTransactions(): LiveData<List<Transaction>>

    @Query("UPDATE transactions SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Int)

    @Query("UPDATE transactions SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: Int)

    // Folder queries
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isArchived = 0 AND folderId = :folderId ORDER BY date DESC")
    fun getTransactionsByFolder(folderId: Int): LiveData<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0 AND folderId = :folderId")
    fun getTransactionCountByFolder(folderId: Int): LiveData<Int>

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
