package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val mobileNumber: String = "",
    val alternateNumber: String = "",
    val address: String = "",
    val notes: String = "",
    val customerSince: Long = System.currentTimeMillis(),
    val status: CustomerStatus = CustomerStatus.ACTIVE,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val defaultRate: Double = 60.0,
    val morningQuantity: Double = 0.0,
    val eveningQuantity: Double = 0.0,
    val advanceBalance: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val cycleStartDay: Int = 1,
    val cycleEndDay: Int = 31
)

enum class CustomerStatus {
    ACTIVE, PAUSED, INACTIVE
}

enum class BillingCycle {
    MONTHLY, DAYS_7, DAYS_10, DAYS_15, CUSTOM
}
