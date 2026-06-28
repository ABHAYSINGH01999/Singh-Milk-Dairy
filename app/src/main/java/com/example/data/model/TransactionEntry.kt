package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val amount: Double,
    val dateMillis: Long,
    val type: TransactionType, // PAYMENT or ADVANCE
    val paymentMode: PaymentMode? = null,
    val notes: String? = null
)

enum class PaymentMode {
    UPI, CASH
}

enum class TransactionType {
    PAYMENT,
    ADVANCE,
    ADVANCE_USED
}
