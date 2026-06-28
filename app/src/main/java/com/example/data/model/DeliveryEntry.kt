package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_entries")
data class DeliveryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val fromDateMillis: Long,
    val toDateMillis: Long,
    val entryType: EntryType,
    val session: DeliverySession,
    val morningQuantity: Double,
    val eveningQuantity: Double,
    val rate: Double,
    val reason: String? = null,
    val autoResume: Boolean = true
)

enum class EntryType {
    NORMAL_DELIVERY,
    GAP,
    EXTRA_DELIVERY,
    QUANTITY_CHANGE,
    PAUSE
}

enum class DeliverySession {
    MORNING,
    EVENING,
    BOTH
}

