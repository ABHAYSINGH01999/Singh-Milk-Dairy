package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_deliveries")
data class DailyDeliveryStatus(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val dateString: String,
    val session: String, // "MORNING" or "EVENING"
    val status: String   // "PENDING", "DELIVERED", "SHIFTED"
)
