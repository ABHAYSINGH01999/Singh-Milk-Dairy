package com.example.data.sync

import com.example.data.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.DailyDeliveryStatus
import com.example.data.model.DeliveryEntry
import com.example.data.model.TransactionEntry
import com.example.data.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncManager(private val database: AppDatabase) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun syncToCloud() = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val userId = user.uid

        val userDoc = firestore.collection("users").document(userId)

        // Sync Customers
        val customers = database.customerDao().getAllCustomersSync()
        val customerBatch = firestore.batch()
        customers.forEach { customer ->
            val docRef = userDoc.collection("customers").document(customer.id.toString())
            customerBatch.set(docRef, customer, SetOptions.merge())
        }
        if (customers.isNotEmpty()) customerBatch.commit().await()

        // Sync Deliveries
        val deliveries = database.deliveryEntryDao().getAllDeliveriesSync()
        val deliveryBatch = firestore.batch()
        deliveries.forEach { delivery ->
            val docRef = userDoc.collection("delivery_entries").document(delivery.id.toString())
            deliveryBatch.set(docRef, delivery, SetOptions.merge())
        }
        if (deliveries.isNotEmpty()) deliveryBatch.commit().await()

        // Sync Daily Delivery Status
        val dailyStatuses = database.dailyDeliveryDao().getAllDailyStatusSync()
        val statusBatch = firestore.batch()
        dailyStatuses.forEach { status ->
            val docRef = userDoc.collection("daily_deliveries").document("${status.customerId}_${status.dateString}_${status.session}")
            statusBatch.set(docRef, status, SetOptions.merge())
        }
        if (dailyStatuses.isNotEmpty()) statusBatch.commit().await()

        // Sync Transactions
        val transactions = database.transactionDao().getAllTransactionsSync()
        val txBatch = firestore.batch()
        transactions.forEach { tx ->
            val docRef = userDoc.collection("transactions").document(tx.id.toString())
            txBatch.set(docRef, tx, SetOptions.merge())
        }
        if (transactions.isNotEmpty()) txBatch.commit().await()

        // Sync Notes
        val notes = database.noteDao().getAllNotesSync()
        val noteBatch = firestore.batch()
        notes.forEach { note ->
            val docRef = userDoc.collection("notes").document(note.id.toString())
            noteBatch.set(docRef, note, SetOptions.merge())
        }
        if (notes.isNotEmpty()) noteBatch.commit().await()
    }

    suspend fun restoreFromCloud() = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val userId = user.uid

        val userDoc = firestore.collection("users").document(userId)

        try {
            val customersSnapshot = userDoc.collection("customers").get().await()
            val customers = customersSnapshot.documents.mapNotNull { it.toObject(Customer::class.java) }
            customers.forEach { database.customerDao().insertCustomer(it) }

            val deliveriesSnapshot = userDoc.collection("delivery_entries").get().await()
            val deliveries = deliveriesSnapshot.documents.mapNotNull { it.toObject(DeliveryEntry::class.java) }
            deliveries.forEach { database.deliveryEntryDao().insertDelivery(it) }

            val dailyStatusSnapshot = userDoc.collection("daily_deliveries").get().await()
            val dailyStatuses = dailyStatusSnapshot.documents.mapNotNull { it.toObject(DailyDeliveryStatus::class.java) }
            dailyStatuses.forEach { database.dailyDeliveryDao().updateDailyStatus(it) }

            val txSnapshot = userDoc.collection("transactions").get().await()
            val transactions = txSnapshot.documents.mapNotNull { it.toObject(TransactionEntry::class.java) }
            transactions.forEach { database.transactionDao().insertTransaction(it) }

            val notesSnapshot = userDoc.collection("notes").get().await()
            val notesList = notesSnapshot.documents.mapNotNull { it.toObject(Note::class.java) }
            notesList.forEach { database.noteDao().insertNote(it) }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
