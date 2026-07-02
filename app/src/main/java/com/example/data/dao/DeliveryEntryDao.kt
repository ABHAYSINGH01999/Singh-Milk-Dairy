package com.example.data.dao

import androidx.room.*
import com.example.data.model.DeliveryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryEntryDao {
    @Query("SELECT * FROM delivery_entries WHERE customerId = :customerId AND isDeleted = 0 ORDER BY fromDateMillis ASC")
    fun getDeliveryEntriesForCustomer(customerId: Int): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries WHERE isDeleted = 0 ORDER BY fromDateMillis ASC")
    fun getAllDeliveryEntries(): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries WHERE isDeleted = 0")
    suspend fun getAllDeliveriesSync(): List<DeliveryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryEntry(entry: DeliveryEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(entry: DeliveryEntry)

    @Query("UPDATE delivery_entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteDeliveryEntry(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteDeliveryEntry(entry: DeliveryEntry)
    
    @Query("SELECT * FROM delivery_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedDeliveryEntries(): Flow<List<DeliveryEntry>>

    @Query("DELETE FROM delivery_entries WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()
}
