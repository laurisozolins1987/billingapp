package com.example.billingapp

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>
    
    private val _dateFilter = MutableLiveData<Pair<Long, Long>?>(null)

    val filteredTransactions: LiveData<List<Transaction>>

    init {
        val dao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(dao)
        allTransactions = repository.allTransactions
        
        filteredTransactions = _dateFilter.switchMap { range ->
            if (range == null) {
                repository.allTransactions
            } else {
                repository.getTransactionsBetweenDates(range.first, range.second)
            }
        }
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun setDateFilter(start: Long, end: Long) {
        _dateFilter.value = Pair(start, end)
    }

    fun clearFilter() {
        _dateFilter.value = null
    }
}
