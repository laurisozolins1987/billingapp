package com.example.billingapp

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>
    val allCategories: LiveData<List<Category>>
    val deletedTransactions: LiveData<List<Transaction>>

    // Filter states
    private val _dateFilter = MutableLiveData<Pair<Long, Long>?>(null)
    private val _searchQuery = MutableLiveData("")
    private val _categoryFilter = MutableLiveData<String?>(null)
    private val _typeFilter = MutableLiveData<Boolean?>(null) // null=all, true=income, false=expense

    val dateFilter: LiveData<Pair<Long, Long>?> = _dateFilter
    val categoryFilter: LiveData<String?> = _categoryFilter
    val typeFilter: LiveData<Boolean?> = _typeFilter

    val filteredTransactions: LiveData<List<Transaction>>

    data class FilterState(
        val dateRange: Pair<Long, Long>? = null,
        val search: String = "",
        val category: String? = null,
        val type: Boolean? = null
    )

    private val _filterState = MediatorLiveData<FilterState>().apply {
        value = FilterState()
        addSource(_dateFilter) { value = value?.copy(dateRange = it) ?: FilterState(dateRange = it) }
        addSource(_searchQuery) { value = value?.copy(search = it) ?: FilterState(search = it) }
        addSource(_categoryFilter) { value = value?.copy(category = it) ?: FilterState(category = it) }
        addSource(_typeFilter) { value = value?.copy(type = it) ?: FilterState(type = it) }
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db.transactionDao(), db.categoryDao())
        allTransactions = repository.allTransactions
        allCategories = repository.allCategories
        deletedTransactions = repository.deletedTransactions

        filteredTransactions = MediatorLiveData<List<Transaction>>().apply {
            addSource(allTransactions) { applyFilters(this, it, _filterState.value) }
            addSource(_filterState) { applyFilters(this, allTransactions.value, it) }
        }
    }

    private fun applyFilters(
        result: MediatorLiveData<List<Transaction>>,
        transactions: List<Transaction>?,
        filter: FilterState?
    ) {
        val list = transactions ?: emptyList()
        val f = filter ?: FilterState()

        result.value = list.filter { t ->
            // Date range filter
            val dateOk = f.dateRange?.let { t.date in it.first..it.second } ?: true
            // Search filter (note, category, description)
            val searchOk = if (f.search.isNotBlank()) {
                val q = f.search.lowercase()
                t.note.lowercase().contains(q) ||
                        t.category.lowercase().contains(q) ||
                        t.description.lowercase().contains(q)
            } else true
            // Category filter
            val catOk = f.category?.let { t.category == it } ?: true
            // Type filter
            val typeOk = f.type?.let { t.isIncome == it } ?: true

            dateOk && searchOk && catOk && typeOk
        }
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.softDelete(transaction.id)
    }

    fun permanentDelete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun restore(transaction: Transaction) = viewModelScope.launch {
        repository.restore(transaction.id)
    }

    fun emptyTrash() = viewModelScope.launch {
        repository.emptyTrash()
    }

    fun setDateFilter(start: Long, end: Long) {
        _dateFilter.value = Pair(start, end)
    }

    fun clearDateFilter() {
        _dateFilter.value = null
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun setTypeFilter(type: Boolean?) {
        _typeFilter.value = type
    }

    fun clearAllFilters() {
        _dateFilter.value = null
        _searchQuery.value = ""
        _categoryFilter.value = null
        _typeFilter.value = null
    }

    fun getTransactionById(id: Int): LiveData<Transaction?> {
        return repository.getTransactionById(id)
    }

    fun insertCategory(name: String) = viewModelScope.launch {
        repository.insertCategory(Category(name = name))
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
    }
}
