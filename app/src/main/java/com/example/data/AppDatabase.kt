package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.CustomerDao
import com.example.data.dao.DailyDeliveryDao
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.DailyDeliveryStatus

import com.example.data.model.TransactionEntry
import com.example.data.model.Note
import com.example.data.dao.NoteDao

@Database(entities = [Customer::class, DeliveryEntry::class, DailyDeliveryStatus::class, TransactionEntry::class, Note::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryEntryDao(): com.example.data.dao.DeliveryEntryDao
    abstract fun dailyDeliveryDao(): DailyDeliveryDao
    abstract fun transactionDao(): com.example.data.dao.TransactionDao
    abstract fun noteDao(): NoteDao
}
