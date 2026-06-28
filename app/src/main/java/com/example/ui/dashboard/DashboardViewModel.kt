package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.repository.DairyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.data.model.DeliveryEntry
import com.example.data.model.EntryType
import com.example.data.model.DeliverySession
import com.example.data.model.TransactionEntry
import com.example.data.model.PaymentMode
import com.example.data.model.Note
import java.util.Calendar

data class CustomerWithBalance(
    val customer: Customer,
    val calculatedOutstanding: Double,
    val computedStatus: com.example.data.model.CustomerStatus,
    val todayMorningReq: Double,
    val todayEveningReq: Double,
    val currentBillAmount: Double,
    val cycleStartDate: Long,
    val cycleEndDate: Long,
    val advanceBalance: Double
)

data class DashboardStats(
    val customers: List<CustomerWithBalance> = emptyList(),
    val todayMilkValue: Double = 0.0,
    val thisMonthMilkValue: Double = 0.0,
    val previousMonthMilkValue: Double = 0.0,
    val currentMonthCollection: Double = 0.0,
    val todayCollection: Double = 0.0,
    val todayUPIToday: Double = 0.0,
    val todayCashToday: Double = 0.0,
    val todaySettledCount: Int = 0,
    val monthlySettledCount: Int = 0,
    val totalAdvanceAvailable: Double = 0.0,
    val advanceCustomersCount: Int = 0,
    val upcomingEvents: List<UpcomingEvent> = emptyList(),
    val notes: List<Note> = emptyList(),
    val transactions: List<TransactionEntry> = emptyList()
)

data class UpcomingEvent(
    val customerName: String,
    val eventType: String,
    val dateStr: String,
    val timeMillis: Long
)

class DashboardViewModel(private val repository: DairyRepository) : ViewModel() {

    val stats: StateFlow<DashboardStats> = combine(
        repository.allCustomers,
        repository.allDeliveryEntries,
        repository.allTransactions,
        repository.allNotes
    ) { customersList, entriesList, transList, notesList ->
        
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMillis = todayCal.timeInMillis

        val monthStartCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val thisMonthMillis = monthStartCal.timeInMillis

        val prevMonthStartCal = (monthStartCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonthMillis = prevMonthStartCal.timeInMillis

        var todayMilkValue = 0.0
        var thisMonthMilkValue = 0.0
        var prevMonthMilkValue = 0.0

        val customerWithBalanceList = mutableListOf<CustomerWithBalance>()
        var totalAdvanceAvailable = 0.0
        var advanceCustomersCount = 0

        val upcomingEvents = mutableListOf<UpcomingEvent>()

        for (customer in customersList) {
            val customerEntries = entriesList.filter { it.customerId == customer.id }
            val customerTrans = transList.filter { it.customerId == customer.id }
            var currentBillAmount = 0.0
            
            var cStatus = customer.status
            var tMorning = customer.morningQuantity
            var tEvening = customer.eveningQuantity

            // Basic billing cycle calculation for demonstration
            // Real logic might need sophisticated tracking, we do simple boundary
            val cycleDays = when (customer.billingCycle) {
                com.example.data.model.BillingCycle.DAYS_7 -> 7
                com.example.data.model.BillingCycle.DAYS_10 -> 10
                com.example.data.model.BillingCycle.DAYS_15 -> 15
                else -> 30
            }
            // Mock cycle end for UI
            val mockCycleEndCal = (todayCal.clone() as Calendar).apply {
                if (customer.id % 2 == 0) add(Calendar.DAY_OF_YEAR, 0) // Some due today
                else add(Calendar.DAY_OF_YEAR, -5) // Some overdue
            }
            val cycleEndDate = mockCycleEndCal.timeInMillis
            val cycleStartDate = cycleEndDate - (cycleDays * 86400000L)

            for (entry in customerEntries) {
                val days = ((entry.toDateMillis - entry.fromDateMillis) / 86400000L).toInt() + 1
                val isMorning = entry.session == DeliverySession.MORNING || entry.session == DeliverySession.BOTH
                val isEvening = entry.session == DeliverySession.EVENING || entry.session == DeliverySession.BOTH
                
                when (entry.entryType) {
                    EntryType.NORMAL_DELIVERY, EntryType.QUANTITY_CHANGE, EntryType.EXTRA_DELIVERY -> {
                        val vM = if(isMorning) entry.morningQuantity * entry.rate else 0.0
                        val vE = if(isEvening) entry.eveningQuantity * entry.rate else 0.0
                        val dailyVal = vM + vE

                        if (isMorning) currentBillAmount += (entry.morningQuantity * days) * entry.rate
                        if (isEvening) currentBillAmount += (entry.eveningQuantity * days) * entry.rate

                        // Very rough approximation of overlap for milk value today, this month, prev month
                        if (todayMillis >= entry.fromDateMillis && todayMillis <= entry.toDateMillis) {
                            todayMilkValue += dailyVal
                        }
                        if (entry.toDateMillis >= thisMonthMillis) {
                            val overlapDays = minOf(entry.toDateMillis, todayMillis) - maxOf(entry.fromDateMillis, thisMonthMillis)
                            if (overlapDays >= 0) thisMonthMilkValue += ((overlapDays / 86400000L).toInt() + 1) * dailyVal
                        }
                        if (entry.toDateMillis >= prevMonthMillis && entry.fromDateMillis < thisMonthMillis) {
                            val overlapDays = minOf(entry.toDateMillis, thisMonthMillis - 86400000L) - maxOf(entry.fromDateMillis, prevMonthMillis)
                            if (overlapDays >= 0) prevMonthMilkValue += ((overlapDays / 86400000L).toInt() + 1) * dailyVal
                        }
                    }
                    else -> {}
                }
                
                val isActiveToday = todayMillis >= entry.fromDateMillis && (todayMillis <= entry.toDateMillis || (entry.entryType == EntryType.PAUSE && !entry.autoResume))

                if (isActiveToday) {
                    if (entry.entryType == EntryType.PAUSE) cStatus = com.example.data.model.CustomerStatus.PAUSED
                    else if (entry.entryType == EntryType.GAP) cStatus = com.example.data.model.CustomerStatus.INACTIVE // Mapping Gap

                    when (entry.entryType) {
                        EntryType.GAP, EntryType.PAUSE -> {
                            if (isMorning) tMorning = 0.0
                            if (isEvening) tEvening = 0.0
                        }
                        EntryType.QUANTITY_CHANGE, EntryType.EXTRA_DELIVERY -> {
                            if (todayMillis <= entry.toDateMillis) {
                                if (isMorning) tMorning = entry.morningQuantity
                                if (isEvening) tEvening = entry.eveningQuantity
                            }
                        }
                        else -> {}
                    }
                }

                // Check for upcoming events
                if (entry.entryType == EntryType.PAUSE && entry.fromDateMillis > todayMillis) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    upcomingEvents.add(UpcomingEvent(customer.name, "Pause Starts", sdf.format(java.util.Date(entry.fromDateMillis)), entry.fromDateMillis))
                }
                if (entry.entryType == EntryType.PAUSE && entry.autoResume && entry.toDateMillis >= todayMillis) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val resumeDate = entry.toDateMillis + 86400000L
                    upcomingEvents.add(UpcomingEvent(customer.name, "Pause Ends", sdf.format(java.util.Date(resumeDate)), resumeDate))
                }
            }
            
            var totalPaid = 0.0
            var totalAdvance = customer.advanceBalance

            for (t in customerTrans) {
                if (t.type == com.example.data.model.TransactionType.PAYMENT) {
                    totalPaid += t.amount
                } else if (t.type == com.example.data.model.TransactionType.ADVANCE) {
                    totalAdvance += t.amount
                } else if (t.type == com.example.data.model.TransactionType.ADVANCE_USED) {
                    totalAdvance -= t.amount
                }
            }

            if (totalAdvance > 0) {
                totalAdvanceAvailable += totalAdvance
                advanceCustomersCount++
            }
            
            val totalOut = customer.outstandingBalance + currentBillAmount - totalPaid
            customerWithBalanceList.add(CustomerWithBalance(customer, totalOut, cStatus, tMorning, tEvening, currentBillAmount, cycleStartDate, cycleEndDate, totalAdvance))
        }

        var todayCol = 0.0
        var todayUPIToday = 0.0
        var todayCashToday = 0.0
        var thisMonthCol = 0.0
        var todaySetCount = 0
        val monthSetCustomers = mutableSetOf<Int>()

        for (t in transList) {
            if (t.type == com.example.data.model.TransactionType.PAYMENT) {
                if (t.dateMillis >= todayMillis) {
                    todayCol += t.amount
                    if (t.paymentMode == PaymentMode.UPI) todayUPIToday += t.amount
                    else if (t.paymentMode == PaymentMode.CASH) todayCashToday += t.amount
                    else todayCashToday += t.amount // Default to cash if null
                    todaySetCount++ // Rough proxy 
                }
                if (t.dateMillis >= thisMonthMillis) {
                    thisMonthCol += t.amount
                    monthSetCustomers.add(t.customerId)
                }
            }
        }

        upcomingEvents.sortBy { it.timeMillis }

        DashboardStats(
            customers = customerWithBalanceList,
            todayMilkValue = todayMilkValue,
            thisMonthMilkValue = thisMonthMilkValue,
            previousMonthMilkValue = prevMonthMilkValue,
            currentMonthCollection = thisMonthCol,
            todayCollection = todayCol,
            todayUPIToday = todayUPIToday,
            todayCashToday = todayCashToday,
            todaySettledCount = todaySetCount,
            monthlySettledCount = monthSetCustomers.size,
            totalAdvanceAvailable = totalAdvanceAvailable,
            advanceCustomersCount = advanceCustomersCount,
            upcomingEvents = upcomingEvents.take(5),
            notes = notesList,
            transactions = transList
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    fun addNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun deleteTransaction(transaction: TransactionEntry) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionEntry) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    companion object {
        fun provideFactory(repository: DairyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(repository) as T
                }
            }
    }
}

