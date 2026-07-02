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
        customerDao.softDeleteCustomer(id)
        scheduleSync()
    }
    
    fun getDeliveryEntriesForCustomer(customerId: Int): Flow<List<DeliveryEntry>> = deliveryEntryDao.getDeliveryEntriesForCustomer(customerId)
    
    suspend fun insertDeliveryEntry(entry: DeliveryEntry) {
        deliveryEntryDao.insertDeliveryEntry(entry)
        scheduleSync()
    }
    
    suspend fun deleteDeliveryEntry(entry: DeliveryEntry) {
        deliveryEntryDao.softDeleteDeliveryEntry(entry.id)
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
        transactionDao.softDeleteTransaction(transaction.id)
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
        noteDao.softDeleteNote(note.id)
        scheduleSync()
    }

    // Recycle Bin Operations
    fun getDeletedCustomers(): Flow<List<Customer>> = customerDao.getDeletedCustomers()
    fun getDeletedDeliveryEntries(): Flow<List<DeliveryEntry>> = deliveryEntryDao.getDeletedDeliveryEntries()
    fun getDeletedTransactions(): Flow<List<TransactionEntry>> = transactionDao.getDeletedTransactions()
    fun getDeletedNotes(): Flow<List<Note>> = noteDao.getDeletedNotes()

    suspend fun emptyRecycleBin() {
        customerDao.emptyRecycleBin()
        deliveryEntryDao.emptyRecycleBin()
        transactionDao.emptyRecycleBin()
        noteDao.emptyRecycleBin()
        dailyDeliveryDao.emptyRecycleBin()
        scheduleSync()
    }

    suspend fun restoreCustomer(customer: Customer) {
        customerDao.insertCustomer(customer.copy(isDeleted = false, deletedAt = null, deletedBy = null))
        scheduleSync()
    }

    suspend fun restoreDeliveryEntry(entry: DeliveryEntry) {
        deliveryEntryDao.insertDeliveryEntry(entry.copy(isDeleted = false, deletedAt = null, deletedBy = null))
        scheduleSync()
    }

    suspend fun restoreTransaction(transaction: TransactionEntry) {
        transactionDao.insertTransaction(transaction.copy(isDeleted = false, deletedAt = null, deletedBy = null))
        scheduleSync()
    }

    suspend fun restoreNote(note: Note) {
        noteDao.insertNote(note.copy(isDeleted = false, deletedAt = null, deletedBy = null))
        scheduleSync()
    }

    suspend fun permanentlyDeleteCustomer(id: Int) {
        customerDao.deleteCustomer(id)
        scheduleSync()
    }

    suspend fun permanentlyDeleteDeliveryEntry(entry: DeliveryEntry) {
        deliveryEntryDao.deleteDeliveryEntry(entry)
        scheduleSync()
    }

    suspend fun permanentlyDeleteTransaction(transaction: TransactionEntry) {
        transactionDao.deleteTransaction(transaction)
        scheduleSync()
    }

    suspend fun permanentlyDeleteNote(note: Note) {
        noteDao.deleteNote(note)
        scheduleSync()
    }
}

