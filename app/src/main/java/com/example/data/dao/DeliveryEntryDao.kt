package com.example.data.dao

import androidx.room.*
import com.example.data.model.DeliveryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryEntryDao {
    @Query("SELECT * FROM delivery_entries WHERE customerId = :customerId ORDER BY fromDateMillis ASC")
    fun getDeliveryEntriesForCustomer(customerId: Int): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries ORDER BY fromDateMillis ASC")
    fun getAllDeliveryEntries(): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries")
    suspend fun getAllDeliveriesSync(): List<DeliveryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryEntry(entry: DeliveryEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(entry: DeliveryEntry)

    @Delete
    suspend fun deleteDeliveryEntry(entry: DeliveryEntry)
}
