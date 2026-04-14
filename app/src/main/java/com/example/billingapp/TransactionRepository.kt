package com.example.billingapp

import androidx.lifecycle.LiveData

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
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
}
