package com.example.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.DailyDeliveryStatus
import com.example.data.repository.DairyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlannerItem(
    val customer: Customer,
    val morningQuantity: Double,
    val eveningQuantity: Double,
    val morningStatus: String, // "PENDING", "DELIVERED", "SHIFTED"
    val eveningStatus: String  // "PENDING", "DELIVERED", "SHIFTED"
)

class DeliveryPlannerViewModel(private val repository: DairyRepository) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateStr = dateFormat.format(Date())

    val plannerItems: StateFlow<List<PlannerItem>> = combine(
        repository.allCustomers,
        repository.allDeliveryEntries,
        repository.getDailyStatuses(todayDateStr)
    ) { customers, entries, statuses ->
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayMillis = cal.timeInMillis

        customers.map { customer ->
            val morningStatusRow = statuses.find { it.customerId == customer.id && it.session == "MORNING" }
            val eveningStatusRow = statuses.find { it.customerId == customer.id && it.session == "EVENING" }

            var computedMorningQty = customer.morningQuantity
            var computedEveningQty = customer.eveningQuantity

            val todayEntries = entries.filter {
                it.customerId == customer.id && todayMillis >= it.fromDateMillis && (todayMillis <= it.toDateMillis || (it.entryType == com.example.data.model.EntryType.PAUSE && !it.autoResume))
            }

            // Apply overrides based on entries for today
            for (entry in todayEntries) {
                val isMorning = entry.session == com.example.data.model.DeliverySession.MORNING || entry.session == com.example.data.model.DeliverySession.BOTH
                val isEvening = entry.session == com.example.data.model.DeliverySession.EVENING || entry.session == com.example.data.model.DeliverySession.BOTH
                
                when (entry.entryType) {
                    com.example.data.model.EntryType.GAP, com.example.data.model.EntryType.PAUSE -> {
                        if (isMorning) computedMorningQty = 0.0
                        if (isEvening) computedEveningQty = 0.0
                    }
                    com.example.data.model.EntryType.QUANTITY_CHANGE, com.example.data.model.EntryType.EXTRA_DELIVERY -> {
                        if (todayMillis <= entry.toDateMillis) {
                            if (isMorning) computedMorningQty = entry.morningQuantity
                            if (isEvening) computedEveningQty = entry.eveningQuantity
                        }
                    }
                    else -> {}
                }
            }

            PlannerItem(
                customer = customer,
                morningQuantity = computedMorningQty,
                eveningQuantity = computedEveningQty,
                morningStatus = morningStatusRow?.status ?: "PENDING",
                eveningStatus = eveningStatusRow?.status ?: "PENDING"
            )
        }.filter { it.morningQuantity > 0 || it.eveningQuantity > 0 || it.eveningStatus == "PENDING_FROM_MORNING_SHIFTED" } 
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateStatus(customerId: Int, session: String, newStatus: String) {
        viewModelScope.launch {
            val existing = repository.getDailyStatus(customerId, todayDateStr, session)
            val updated = existing?.copy(status = newStatus) 
                ?: DailyDeliveryStatus(
                    customerId = customerId,
                    dateString = todayDateStr,
                    session = session,
                    status = newStatus
                )
            repository.updateDailyStatus(updated)
        }
    }

    companion object {
        fun provideFactory(repository: DairyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DeliveryPlannerViewModel::class.java)) {
                        return DeliveryPlannerViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
