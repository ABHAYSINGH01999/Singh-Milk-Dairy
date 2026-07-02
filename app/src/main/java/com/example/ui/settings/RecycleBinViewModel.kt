package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.Note
import com.example.data.model.TransactionEntry
import com.example.data.repository.DairyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecycleBinViewModel(private val repository: DairyRepository) : ViewModel() {

    val deletedCustomers: StateFlow<List<Customer>> = repository.getDeletedCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedDeliveryEntries: StateFlow<List<DeliveryEntry>> = repository.getDeletedDeliveryEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedTransactions: StateFlow<List<TransactionEntry>> = repository.getDeletedTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNotes: StateFlow<List<Note>> = repository.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
        }
    }

    fun restoreCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.restoreCustomer(customer)
        }
    }

    fun permanentlyDeleteCustomer(id: Int) {
        viewModelScope.launch {
            repository.permanentlyDeleteCustomer(id)
        }
    }

    fun restoreDeliveryEntry(entry: DeliveryEntry) {
        viewModelScope.launch {
            repository.restoreDeliveryEntry(entry)
        }
    }

    fun permanentlyDeleteDeliveryEntry(entry: DeliveryEntry) {
        viewModelScope.launch {
            repository.permanentlyDeleteDeliveryEntry(entry)
        }
    }

    fun restoreTransaction(transaction: TransactionEntry) {
        viewModelScope.launch {
            repository.restoreTransaction(transaction)
        }
    }

    fun permanentlyDeleteTransaction(transaction: TransactionEntry) {
        viewModelScope.launch {
            repository.permanentlyDeleteTransaction(transaction)
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.restoreNote(note)
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            repository.permanentlyDeleteNote(note)
        }
    }

    companion object {
        fun provideFactory(repository: DairyRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecycleBinViewModel(repository) as T
            }
        }
    }
}
