package com.example.billingapp

import androidx.lifecycle.LiveData

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val folderDao: FolderDao
) {
    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()
    val allFolders: LiveData<List<Folder>> = folderDao.getAllFolders()
    val deletedTransactions: LiveData<List<Transaction>> = transactionDao.getDeletedTransactions()
    val bookmarkedTransactions: LiveData<List<Transaction>> = transactionDao.getBookmarkedTransactions()
    val archivedTransactions: LiveData<List<Transaction>> = transactionDao.getArchivedTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun softDelete(id: Int) {
        transactionDao.softDelete(id)
    }

    suspend fun restore(id: Int) {
        transactionDao.restore(id)
    }

    suspend fun emptyTrash() {
        transactionDao.emptyTrash()
    }

    suspend fun setBookmarked(id: Int, bookmarked: Boolean) {
        transactionDao.setBookmarked(id, bookmarked)
    }

    suspend fun archive(id: Int) {
        transactionDao.archive(id)
    }

    suspend fun unarchive(id: Int) {
        transactionDao.unarchive(id)
    }

    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }

    fun searchTransactions(query: String): LiveData<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }

    fun getTransactionById(id: Int): LiveData<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsByFolder(folderId: Int): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByFolder(folderId)
    }

    fun getTransactionCountByFolder(folderId: Int): LiveData<Int> {
        return transactionDao.getTransactionCountByFolder(folderId)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun insertFolder(folder: Folder) {
        folderDao.insert(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.update(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        folderDao.delete(folder)
    }
}
