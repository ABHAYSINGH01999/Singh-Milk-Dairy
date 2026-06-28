package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyDeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyDeliveryDao {
    @Query("SELECT * FROM daily_deliveries WHERE dateString = :dateStr")
    fun getStatusesForDate(dateStr: String): Flow<List<DailyDeliveryStatus>>

    @Query("SELECT * FROM daily_deliveries WHERE customerId = :customerId AND dateString = :dateStr AND session = :session LIMIT 1")
    suspend fun getStatus(customerId: Int, dateStr: String, session: String): DailyDeliveryStatus?

    @Query("SELECT * FROM daily_deliveries")
    suspend fun getAllDailyStatusSync(): List<DailyDeliveryStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: DailyDeliveryStatus)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDailyStatus(status: DailyDeliveryStatus)
}
