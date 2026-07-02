package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyDeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyDeliveryDao {
    @Query("SELECT * FROM daily_deliveries WHERE dateString = :dateStr AND isDeleted = 0")
    fun getStatusesForDate(dateStr: String): Flow<List<DailyDeliveryStatus>>

    @Query("SELECT * FROM daily_deliveries WHERE customerId = :customerId AND dateString = :dateStr AND session = :session AND isDeleted = 0 LIMIT 1")
    suspend fun getStatus(customerId: Int, dateStr: String, session: String): DailyDeliveryStatus?

    @Query("SELECT * FROM daily_deliveries WHERE isDeleted = 0")
    suspend fun getAllDailyStatusSync(): List<DailyDeliveryStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: DailyDeliveryStatus)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDailyStatus(status: DailyDeliveryStatus)

    @Query("UPDATE daily_deliveries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteDailyStatus(id: Int, deletedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM daily_deliveries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedDailyStatuses(): Flow<List<DailyDeliveryStatus>>

    @Query("DELETE FROM daily_deliveries WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()
}
