package com.example.billingapp

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>
    
    private val _dateFilter = MutableLiveData<Pair<Long, Long>?>(null)
    private val _searchQuery = MutableLiveData<String?>("")

    val filteredTransactions: LiveData<List<Transaction>>

    init {
        val dao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(dao)
        allTransactions = repository.allTransactions
        
        val combinedFilter = MediatorLiveData<Pair<Pair<Long, Long>?, String?>>().apply {
            addSource(_dateFilter) { value = Pair(it, _searchQuery.value) }
            addSource(_searchQuery) { value = Pair(_dateFilter.value, it) }
        }

        filteredTransactions = combinedFilter.switchMap { (dateRange, query) ->
            if (!query.isNullOrBlank()) {
                repository.searchTransactions(query)
            } else if (dateRange != null) {
                repository.getTransactionsBetweenDates(dateRange.first, dateRange.second)
            } else {
                repository.allTransactions
            }
        }
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun setDateFilter(start: Long, end: Long) {
        _searchQuery.value = ""
        _dateFilter.value = Pair(start, end)
    }

    fun clearFilter() {
        _dateFilter.value = null
        _searchQuery.value = ""
    }

    fun search(query: String) {
        _dateFilter.value = null
        _searchQuery.value = query
    }

    fun getTransactionById(id: Int): LiveData<Transaction?> {
        return repository.getTransactionById(id)
    }
}
