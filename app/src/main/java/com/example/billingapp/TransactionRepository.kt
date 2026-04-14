package com.example.billingapp

import androidx.lifecycle.LiveData

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()
    val deletedTransactions: LiveData<List<Transaction>> = transactionDao.getDeletedTransactions()

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

    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }

    fun searchTransactions(query: String): LiveData<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }

    fun getTransactionById(id: Int): LiveData<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }
}
