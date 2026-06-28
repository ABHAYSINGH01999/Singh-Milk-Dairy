package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val title: String,
    val details: String,
    val reminderDateMillis: Long? = null,
    val priority: NotePriority = NotePriority.MEDIUM,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

enum class NotePriority {
    LOW, MEDIUM, HIGH
}
