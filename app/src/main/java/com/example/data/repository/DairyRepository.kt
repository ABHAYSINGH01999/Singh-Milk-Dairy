package com.example.data.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.dao.CustomerDao
import com.example.data.dao.DailyDeliveryDao
import com.example.data.dao.DeliveryEntryDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.NoteDao
import com.example.data.model.Customer
import com.example.data.model.DailyDeliveryStatus
import com.example.data.model.DeliveryEntry
import com.example.data.model.TransactionEntry
import com.example.data.model.Note
import com.example.data.sync.SyncWorker
import kotlinx.coroutines.flow.Flow

class DairyRepository(
    private val context: Context,
    private val customerDao: CustomerDao,
    private val deliveryEntryDao: DeliveryEntryDao,
    private val dailyDeliveryDao: DailyDeliveryDao,
    private val transactionDao: TransactionDao,
    private val noteDao: NoteDao
) {
    private fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allDeliveryEntries: Flow<List<DeliveryEntry>> = deliveryEntryDao.getAllDeliveryEntries()
    val allTransactions: Flow<List<TransactionEntry>> = transactionDao.getAllTransactions()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getDailyStatuses(dateStr: String): Flow<List<DailyDeliveryStatus>> = dailyDeliveryDao.getStatusesForDate(dateStr)

    suspend fun updateDailyStatus(status: DailyDeliveryStatus) {
        dailyDeliveryDao.insertOrUpdate(status)
        scheduleSync()
    }

    suspend fun getDailyStatus(customerId: Int, dateStr: String, session: String): DailyDeliveryStatus? {
        return dailyDeliveryDao.getStatus(customerId, dateStr, session)
    }

    fun getCustomerById(id: Int): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun insertCustomer(customer: Customer): Long {
        val id = customerDao.insertCustomer(customer)
        scheduleSync()
        return id
    }
    
    suspend fun deleteCustomer(id: Int) {
        customerDao.deleteCustomer(id)
        scheduleSync()
    }
    
    fun getDeliveryEntriesForCustomer(customerId: Int): Flow<List<DeliveryEntry>> = deliveryEntryDao.getDeliveryEntriesForCustomer(customerId)
    
    suspend fun insertDeliveryEntry(entry: DeliveryEntry) {
        deliveryEntryDao.insertDeliveryEntry(entry)
        scheduleSync()
    }
    
    suspend fun deleteDeliveryEntry(entry: DeliveryEntry) {
        deliveryEntryDao.deleteDeliveryEntry(entry)
        scheduleSync()
    }

    fun getTransactionsForCustomer(customerId: Int): Flow<List<TransactionEntry>> = transactionDao.getTransactionsForCustomer(customerId)

    suspend fun insertTransaction(transaction: TransactionEntry) {
        transactionDao.insertTransaction(transaction)
        scheduleSync()
    }

    suspend fun updateTransaction(transaction: TransactionEntry) {
        transactionDao.updateTransaction(transaction)
        scheduleSync()
    }

    suspend fun deleteTransaction(transaction: TransactionEntry) {
        transactionDao.deleteTransaction(transaction)
        scheduleSync()
    }

    fun getNotesForCustomer(customerId: Int): Flow<List<Note>> = noteDao.getNotesForCustomer(customerId)

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
        scheduleSync()
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        scheduleSync()
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        scheduleSync()
    }
}

