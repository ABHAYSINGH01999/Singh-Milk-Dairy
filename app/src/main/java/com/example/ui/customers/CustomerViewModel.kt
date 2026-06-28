package com.example.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.EntryType
import com.example.data.repository.DairyRepository
import com.example.data.model.DeliverySession
import com.example.data.model.TransactionEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerStats(
    val startDate: Long? = null,
    val lastDeliveryDate: Long? = null,
    val totalMorningDeliveryDays: Int = 0,
    val totalEveningDeliveryDays: Int = 0,
    val totalDeliveryDays: Int = 0,
    val totalGapDays: Int = 0,
    val totalPauseDays: Int = 0,
    val totalExtraDeliveryDays: Int = 0,
    val totalMorningMilkDelivered: Double = 0.0,
    val totalEveningMilkDelivered: Double = 0.0,
    val totalMilkDelivered: Double = 0.0,
    val extraMilkQuantity: Double = 0.0,
    val currentBillAmount: Double = 0.0,
    val totalAmountPaid: Double = 0.0,
    val totalAdvanceAvailable: Double = 0.0
)

class CustomerViewModel(private val repository: DairyRepository) : ViewModel() {

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _deliveryEntries = MutableStateFlow<List<DeliveryEntry>>(emptyList())
    val deliveryEntries: StateFlow<List<DeliveryEntry>> = _deliveryEntries.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntry>>(emptyList())
    val transactions: StateFlow<List<TransactionEntry>> = _transactions.asStateFlow()

    fun loadCustomer(id: Int) {
        viewModelScope.launch {
            repository.getCustomerById(id).collect { customer ->
                _selectedCustomer.value = customer
            }
        }
        viewModelScope.launch {
            repository.getDeliveryEntriesForCustomer(id).collect { entries ->
                _deliveryEntries.value = entries
            }
        }
        viewModelScope.launch {
            repository.getTransactionsForCustomer(id).collect { trans ->
                _transactions.value = trans
            }
        }
    }

    fun saveCustomer(customer: Customer, initialAdvance: Double = 0.0) {
        viewModelScope.launch {
            val isNew = customer.id == 0
            val rowId = repository.insertCustomer(customer)
            if (isNew && initialAdvance > 0) {
                // Determine the new customer ID. If rowId > 0, it's the id, otherwise we might have an issue.
                // Room insert returns rowId which is usually the generated primary key for autoincrement.
                val newId = rowId.toInt()
                val trans = TransactionEntry(
                    customerId = newId,
                    amount = initialAdvance,
                    dateMillis = System.currentTimeMillis(),
                    type = com.example.data.model.TransactionType.ADVANCE,
                    notes = "Initial Advance from Customer Creation"
                )
                repository.insertTransaction(trans)
            }
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
        }
    }

    fun deleteDeliveryEntry(entry: DeliveryEntry) {
        viewModelScope.launch {
            repository.deleteDeliveryEntry(entry)
        }
    }

    fun saveDeliveryEntry(entry: DeliveryEntry) {
        viewModelScope.launch {
            repository.insertDeliveryEntry(entry)
        }
    }

    fun saveTransaction(transaction: TransactionEntry) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            
            // Note: We might want to clear advanceBalance and outstandingBalance in Customer since we track it in transations now. 
            // Or we keep doing updates to customer? 
            // Let's rely on transactions for dynamic values.
            val c = _selectedCustomer.value ?: return@launch
            if (transaction.type == com.example.data.model.TransactionType.PAYMENT) {
                // If it's a payment, maybe adjust outstanding dynamically? 
                // We'll calculate it via stats.
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntry) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateCustomerStatus(customer: Customer, status: com.example.data.model.CustomerStatus) {
        viewModelScope.launch {
            val updated = customer.copy(status = status)
            repository.insertCustomer(updated)
        }
    }

    fun calculateStats(customer: Customer?, entries: List<DeliveryEntry>, trans: List<TransactionEntry>): CustomerStats {
        if (customer == null) return CustomerStats()
        var startDate: Long? = null
        var lastDeliveryDate: Long? = null
        var totalMorningDeliveryDays = 0
        var totalEveningDeliveryDays = 0
        var totalDeliveryDays = 0
        var totalGapDays = 0
        var totalPauseDays = 0
        var totalExtraDeliveryDays = 0
        var totalMorningMilkDelivered = 0.0
        var totalEveningMilkDelivered = 0.0
        var totalMilkDelivered = 0.0
        var extraMilkQuantity = 0.0
        var currentBillAmount = 0.0

        for (entry in entries) {
            val days = ((entry.toDateMillis - entry.fromDateMillis) / 86400000L).toInt() + 1
            if (startDate == null || entry.fromDateMillis < startDate!!) startDate = entry.fromDateMillis
            if (lastDeliveryDate == null || entry.toDateMillis > lastDeliveryDate!!) lastDeliveryDate = entry.toDateMillis

            val isMorning = entry.session == DeliverySession.MORNING || entry.session == DeliverySession.BOTH
            val isEvening = entry.session == DeliverySession.EVENING || entry.session == DeliverySession.BOTH

            when (entry.entryType) {
                EntryType.NORMAL_DELIVERY, EntryType.QUANTITY_CHANGE -> {
                    totalDeliveryDays += days
                    if (isMorning) {
                        totalMorningDeliveryDays += days
                        val mQty = entry.morningQuantity * days
                        totalMorningMilkDelivered += mQty
                        totalMilkDelivered += mQty
                        currentBillAmount += mQty * entry.rate
                    }
                    if (isEvening) {
                        totalEveningDeliveryDays += days
                        val eQty = entry.eveningQuantity * days
                        totalEveningMilkDelivered += eQty
                        totalMilkDelivered += eQty
                        currentBillAmount += eQty * entry.rate
                    }
                }
                EntryType.EXTRA_DELIVERY -> {
                    totalExtraDeliveryDays += days
                    if (isMorning) {
                        val mQty = entry.morningQuantity * days
                        totalMorningMilkDelivered += mQty
                        totalMilkDelivered += mQty
                        extraMilkQuantity += mQty
                        currentBillAmount += mQty * entry.rate
                    }
                    if (isEvening) {
                        val eQty = entry.eveningQuantity * days
                        totalEveningMilkDelivered += eQty
                        totalMilkDelivered += eQty
                        extraMilkQuantity += eQty
                        currentBillAmount += eQty * entry.rate
                    }
                }
                EntryType.GAP -> {
                    totalGapDays += days
                }
                EntryType.PAUSE -> {
                    totalPauseDays += days
                }
            }
        }

        var totalAmountPaid = 0.0
        var totalAdvanceAvailable = customer.advanceBalance // Base advance from customer setup

        for (t in trans) {
            if (t.type == com.example.data.model.TransactionType.PAYMENT) {
                totalAmountPaid += t.amount
            } else if (t.type == com.example.data.model.TransactionType.ADVANCE) {
                totalAdvanceAvailable += t.amount
            } else if (t.type == com.example.data.model.TransactionType.ADVANCE_USED) {
                totalAdvanceAvailable -= t.amount
                // Also conceptually, advance used might offset the bill, so... 
                // we'll just consider it effectively reduces totalAdvanceAvailable.
                // Depending on the logic, they might also track "advance used" separate, but we leave it.
            }
        }

        return CustomerStats(
            startDate = startDate,
            lastDeliveryDate = lastDeliveryDate,
            totalMorningDeliveryDays = totalMorningDeliveryDays,
            totalEveningDeliveryDays = totalEveningDeliveryDays,
            totalDeliveryDays = totalDeliveryDays,
            totalGapDays = totalGapDays,
            totalPauseDays = totalPauseDays,
            totalExtraDeliveryDays = totalExtraDeliveryDays,
            totalMorningMilkDelivered = totalMorningMilkDelivered,
            totalEveningMilkDelivered = totalEveningMilkDelivered,
            totalMilkDelivered = totalMilkDelivered,
            extraMilkQuantity = extraMilkQuantity,
            currentBillAmount = currentBillAmount,
            totalAmountPaid = totalAmountPaid,
            totalAdvanceAvailable = totalAdvanceAvailable
        )
    }

    fun generateBillText(customer: Customer, entries: List<DeliveryEntry>, stats: CustomerStats): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val isMorningOnly = customer.morningQuantity > 0 && customer.eveningQuantity == 0.0
        val isEveningOnly = customer.eveningQuantity > 0 && customer.morningQuantity == 0.0

        val startDateStr = stats.startDate?.let { sdf.format(java.util.Date(it)) } ?: "N/A"
        val lastDateStr = stats.lastDeliveryDate?.let { sdf.format(java.util.Date(it)) } ?: "N/A"

        val sb = StringBuilder()
        sb.append("Customer: ${customer.name}\n")
        sb.append("Billing Period: $startDateStr - $lastDateStr\n")
        sb.append("Rate: ₹${customer.defaultRate} Per Litre\n\n")

        sb.append("=================\n")
        sb.append("DELIVERY BREAKDOWN\n")
        sb.append("=================\n")

        val sortedEntries = entries.sortedBy { it.fromDateMillis }
        val mergedEntries = mutableListOf<DeliveryEntry>()

        if (sortedEntries.isNotEmpty()) {
            var current = sortedEntries[0]
            for (i in 1 until sortedEntries.size) {
                val next = sortedEntries[i]
                
                val toDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = current.toDateMillis
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val nextFromDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = next.fromDateMillis
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val diffDays = (nextFromDate - toDate) / 86400000L
                val isConsecutive = diffDays in 0..1L

                if (isConsecutive &&
                    current.entryType == next.entryType &&
                    current.session == next.session &&
                    current.morningQuantity == next.morningQuantity &&
                    current.eveningQuantity == next.eveningQuantity &&
                    current.rate == next.rate
                ) {
                    current = current.copy(toDateMillis = maxOf(current.toDateMillis, next.toDateMillis))
                } else {
                    mergedEntries.add(current)
                    current = next
                }
            }
            mergedEntries.add(current)
        }

        mergedEntries.forEach { entry ->
            val from = sdf.format(java.util.Date(entry.fromDateMillis))
            val to = sdf.format(java.util.Date(entry.toDateMillis))
            val days = ((entry.toDateMillis - entry.fromDateMillis) / 86400000L).toInt() + 1
            
            sb.append(if (from == to) "$from\n" else "$from - $to\n")
            
            val isMorn = entry.session == DeliverySession.MORNING || entry.session == DeliverySession.BOTH
            val isEve = entry.session == DeliverySession.EVENING || entry.session == DeliverySession.BOTH

            val mStr = if (isMorn) "${entry.morningQuantity}L" else "0L"
            val eStr = if (isEve) "${entry.eveningQuantity}L" else "0L"

            if (entry.entryType == EntryType.GAP || entry.entryType == EntryType.PAUSE) {
                sb.append("${entry.entryType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} ($days Days)\n")
                if (!entry.reason.isNullOrEmpty()) sb.append("Reason: ${entry.reason}\n")
            } else {
                if (!isEveningOnly) sb.append("Morning: $mStr ")
                if (!isEveningOnly && !isMorningOnly) sb.append("| ")
                if (!isMorningOnly) sb.append("Evening: $eStr\n")
                if (isMorningOnly || isEveningOnly) sb.append("\n")
                sb.append("Days: $days\n")
                sb.append("Type: ${entry.entryType.name.replace("_", " ")}\n")
            }
            sb.append("---\n")
        }

        sb.append("\n=================\n")
        sb.append("SUMMARY\n")
        sb.append("=================\n")
        if (!isEveningOnly) sb.append("Total Morning Milk: ${String.format("%.2f", stats.totalMorningMilkDelivered)} L\n")
        if (!isMorningOnly) sb.append("Total Evening Milk: ${String.format("%.2f", stats.totalEveningMilkDelivered)} L\n")
        sb.append("Total Milk Delivered: ${String.format("%.2f", stats.totalMilkDelivered)} L\n")
        sb.append("Total Delivery Days: ${stats.totalDeliveryDays}\n")
        sb.append("Total Gap Days: ${stats.totalGapDays}\n")
        sb.append("Total Pause Days: ${stats.totalPauseDays}\n")
        
        sb.append("\n=================\n")
        sb.append("FINANCIALS\n")
        sb.append("=================\n")
        sb.append("Current Bill: ₹${String.format("%.2f", stats.currentBillAmount)}\n")
        sb.append("Previous Due: ₹${customer.outstandingBalance}\n")
        sb.append("Security Deposit: ₹${stats.totalAdvanceAvailable}\n")
        sb.append("Amount Paid: ₹${stats.totalAmountPaid}\n")
        val total = customer.outstandingBalance + stats.currentBillAmount - stats.totalAmountPaid
        sb.append("Outstanding: ₹${String.format("%.2f", total)}\n")

        return sb.toString()
    }

    fun generatePdfBill(context: android.content.Context, customer: Customer, entries: List<DeliveryEntry>, stats: CustomerStats) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        paint.textSize = 12f
        paint.color = android.graphics.Color.BLACK
        
        var y = 50f
        val billText = generateBillText(customer, entries, stats)
        for (line in billText.split("\n")) {
            canvas.drawText(line, 50f, y, paint)
            y += 18f
        }
        
        pdfDocument.finishPage(page)
        
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Milk_Bill_${customer.name}.pdf")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        try {
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    pdfDocument.writeTo(out)
                }
            }
            android.widget.Toast.makeText(context, "PDF saved to Downloads", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to save PDF", android.widget.Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    companion object {
        fun provideFactory(repository: DairyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CustomerViewModel(repository) as T
                }
            }
    }
}
