package com.example.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.ui.components.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.PaymentMode
import com.example.data.model.DeliveryEntry
import com.example.data.model.TransactionEntry
import com.example.data.model.EntryType
import com.example.data.model.DeliverySession
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    viewModel: CustomerViewModel,
    customerId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToAddEntry: (Int) -> Unit,
    onNavigateToEditEntry: (Int, Int) -> Unit,
    onNavigateToEditCustomer: (Int) -> Unit,
    onNavigateToSettlements: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
    }

    val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val entries by viewModel.deliveryEntries.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val stats = remember(customer, entries, transactions) { viewModel.calculateStats(customer, entries, transactions) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showWhatsAppSheet by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf<com.example.data.model.TransactionType?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val sheetState = rememberModalBottomSheetState()

    if (showTransactionDialog != null) {
        TransactionDialog(
            type = showTransactionDialog!!,
            onDismiss = { showTransactionDialog = null },
            onSave = { amount, notes ->
                viewModel.saveTransaction(
                    com.example.data.model.TransactionEntry(
                        customerId = customerId,
                        amount = amount,
                        dateMillis = System.currentTimeMillis(),
                        type = showTransactionDialog!!,
                        notes = notes
                    )
                )
                showTransactionDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Edit Customer") }, onClick = { showMoreMenu = false; onNavigateToEditCustomer(customerId) })
                            DropdownMenuItem(text = { Text("Add Payment") }, onClick = { showMoreMenu = false; showTransactionDialog = com.example.data.model.TransactionType.PAYMENT })
                            DropdownMenuItem(text = { Text("Add Advance") }, onClick = { showMoreMenu = false; showTransactionDialog = com.example.data.model.TransactionType.ADVANCE })
                            DropdownMenuItem(text = { Text("Use Advance") }, onClick = { showMoreMenu = false; showTransactionDialog = com.example.data.model.TransactionType.ADVANCE_USED })
                            DropdownMenuItem(
                                text = { Text("Generate Bill (PDF)") },
                                onClick = {
                                    showMoreMenu = false
                                    if (customer != null) viewModel.generatePdfBill(context, customer!!, entries, stats)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Bill") },
                                onClick = {
                                    showMoreMenu = false
                                    if (customer != null) {
                                        val billText = viewModel.generateBillText(customer!!, entries, stats)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Milk Bill for ${customer!!.name}")
                                            putExtra(Intent.EXTRA_TEXT, billText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Bill"))
                                    }
                                }
                            )
                            if (customer != null) {
                                if (customer!!.status == com.example.data.model.CustomerStatus.ACTIVE) {
                                    DropdownMenuItem(text = { Text("Pause Customer") }, onClick = { showMoreMenu = false; viewModel.updateCustomerStatus(customer!!, com.example.data.model.CustomerStatus.PAUSED) })
                                    DropdownMenuItem(text = { Text("Close Customer") }, onClick = { showMoreMenu = false; viewModel.updateCustomerStatus(customer!!, com.example.data.model.CustomerStatus.INACTIVE) })
                                } else if (customer!!.status == com.example.data.model.CustomerStatus.PAUSED) {
                                    DropdownMenuItem(text = { Text("Resume Customer") }, onClick = { showMoreMenu = false; viewModel.updateCustomerStatus(customer!!, com.example.data.model.CustomerStatus.ACTIVE) })
                                    DropdownMenuItem(text = { Text("Close Customer") }, onClick = { showMoreMenu = false; viewModel.updateCustomerStatus(customer!!, com.example.data.model.CustomerStatus.INACTIVE) })
                                } else if (customer!!.status == com.example.data.model.CustomerStatus.INACTIVE) {
                                    DropdownMenuItem(text = { Text("Reopen Customer") }, onClick = { showMoreMenu = false; viewModel.updateCustomerStatus(customer!!, com.example.data.model.CustomerStatus.ACTIVE) })
                                }
                            }
                            DropdownMenuItem(text = { Text("Delete Customer") }, onClick = { showMoreMenu = false; showDeleteConfirmation = true })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddEntry(customerId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        if (customer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val todayMillis = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            var computedStatusString = when (customer!!.status) {
                com.example.data.model.CustomerStatus.ACTIVE -> "🟢 Active"
                com.example.data.model.CustomerStatus.PAUSED -> "⏸ Paused"
                com.example.data.model.CustomerStatus.INACTIVE -> "🔴 Closed"
            }
            entries.forEach { entry ->
                val isActiveToday = todayMillis >= entry.fromDateMillis && (todayMillis <= entry.toDateMillis || (entry.entryType == com.example.data.model.EntryType.PAUSE && !entry.autoResume))
                if (isActiveToday) {
                    if (entry.entryType == com.example.data.model.EntryType.PAUSE) {
                        computedStatusString = "⏸ Paused"
                    } else if (entry.entryType == com.example.data.model.EntryType.GAP) {
                        computedStatusString = "⚪ Gap"
                    }
                }
            }
            
            var showSettlementPopup by remember { mutableStateOf(false) }
            var isSecurityDepositDeducted by remember { mutableStateOf(false) }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Customer Information
                CustomerHeader(
                    customer = customer!!,
                    computedStatusString = computedStatusString,    
                    onCall = { 
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer!!.mobileNumber}"))
                        context.startActivity(intent)
                    },
                    onWhatsApp = { showWhatsAppSheet = true },
                    onGenerateBill = { viewModel.generatePdfBill(context, customer!!, entries, stats) }
                )
                
                // Section 2: Billing Cycle
                BillingCycleCard(customer!!, onEdit = { onNavigateToEditCustomer(customerId) })

                // Section 3: Delivery Timeline
                DeliveryHistoryTimeline(
                    entries = entries,
                    onEdit = { entryId -> onNavigateToEditEntry(customerId, entryId) },
                    onDelete = { viewModel.deleteDeliveryEntry(it) },
                    onAdd = { onNavigateToAddEntry(customerId) }
                )

                // Section 4: Financial Summary
                FinancialSummarySection(
                    customer = customer!!,
                    stats = stats,
                    isSecurityDepositDeducted = isSecurityDepositDeducted,
                    onDeductChanged = { isSecurityDepositDeducted = it },
                    onMarkSettled = { showSettlementPopup = true }
                )

                // Section 5: Settlement History
                SettlementHistorySection(transactions, onNavigateToSettlements)

                // Section 6: Advance Summary
                AdvanceSection(
                    customer = customer!!,
                    stats = stats,
                    transactions = transactions,
                    onEditCustomer = { onNavigateToEditCustomer(customerId) },
                    onAddDeposit = { showTransactionDialog = com.example.data.model.TransactionType.ADVANCE },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                )
                
                // Section 7: Monthly Statements
                MonthlyStatementsSection(customer = customer!!, entries = entries, transactions = transactions)
            }

            if (showSettlementPopup) {
                SettlementDialog(
                    defaultAmount = if (isSecurityDepositDeducted) {
                        (customer!!.outstandingBalance + stats.currentBillAmount - stats.totalAmountPaid - stats.totalAdvanceAvailable).coerceAtLeast(0.0)
                    } else {
                        (customer!!.outstandingBalance + stats.currentBillAmount - stats.totalAmountPaid).coerceAtLeast(0.0)
                    },
                    onDismiss = { showSettlementPopup = false },
                    onSave = { amount, mode ->
                        viewModel.saveTransaction(
                            com.example.data.model.TransactionEntry(
                                customerId = customerId,
                                amount = amount,
                                dateMillis = System.currentTimeMillis(),
                                type = com.example.data.model.TransactionType.PAYMENT,
                                paymentMode = mode
                            )
                        )
                        if (isSecurityDepositDeducted && stats.totalAdvanceAvailable > 0) {
                            val amountToDeduct = minOf(stats.totalAdvanceAvailable, (customer!!.outstandingBalance + stats.currentBillAmount - stats.totalAmountPaid).coerceAtLeast(0.0))
                            if (amountToDeduct > 0) {
                                viewModel.saveTransaction(
                                    com.example.data.model.TransactionEntry(
                                        customerId = customerId,
                                        amount = amountToDeduct,
                                        dateMillis = System.currentTimeMillis(),
                                        type = com.example.data.model.TransactionType.ADVANCE_USED
                                    )
                                )
                            }
                        }
                        showSettlementPopup = false
                    }
                )
            }

            if (showWhatsAppSheet) {
                WhatsAppBottomSheet(
                    customer = customer!!,
                    stats = stats,
                    onDismiss = { showWhatsAppSheet = false },
                    onSend = { text ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        val url = "https://api.whatsapp.com/send?phone=+91${customer!!.mobileNumber}&text=${Uri.encode(text)}"
                        intent.data = Uri.parse(url)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showWhatsAppSheet = false
                    }
                )
            }

            if (showDeleteConfirmation && customer != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Delete Customer?", color = androidx.compose.ui.graphics.Color.White) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    text = { Text("Are you sure you want to permanently delete customer \"${customer!!.name}\" and all their deliveries/transactions? This cannot be undone.", color = androidx.compose.ui.graphics.Color.LightGray) },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteCustomer(customerId)
                                showDeleteConfirmation = false
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancel", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DeliveryHistoryTimeline(entries: List<com.example.data.model.DeliveryEntry>, onEdit: (Int) -> Unit, onDelete: (com.example.data.model.DeliveryEntry) -> Unit, onAdd: () -> Unit) {
    if (entries.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delivery Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            }
            TextButton(onClick = onAdd, contentPadding = PaddingValues(0.dp)) {
                Text("+ Add Delivery", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            }
        }
        
        val sorted = entries.sortedByDescending { it.fromDateMillis }
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                sorted.forEachIndexed { index, entry ->
                    DeliveryEntryCard(entry, onEdit = { onEdit(entry.id) }, onDelete = { onDelete(entry) })
                    if (index < sorted.size - 1) {
                        HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryEntryCard(entry: com.example.data.model.DeliveryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    val from = sdf.format(java.util.Date(entry.fromDateMillis))
    val to = sdf.format(java.util.Date(entry.toDateMillis))
    val days = ((entry.toDateMillis - entry.fromDateMillis) / 86400000L).toInt() + 1
    val dateStr = if (from == to) from else "$from  —  $to"

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(dateStr, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            // Milk Qty
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (entry.entryType == com.example.data.model.EntryType.GAP || entry.entryType == com.example.data.model.EntryType.PAUSE) {
                    Text("Reason", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.LightGray)
                    Text(if (!entry.reason.isNullOrEmpty()) entry.reason!! else "None", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                } else {
                    val hasMorning = entry.session == com.example.data.model.DeliverySession.MORNING || entry.session == com.example.data.model.DeliverySession.BOTH
                    val hasEvening = entry.session == com.example.data.model.DeliverySession.EVENING || entry.session == com.example.data.model.DeliverySession.BOTH
                    
                    if (hasMorning) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Morning", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.LightGray)
                        }
                        Text("${entry.morningQuantity} L", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp))
                        if (hasEvening) Spacer(Modifier.height(4.dp))
                    }
                    if (hasEvening) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF7986CB), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Evening", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.LightGray)
                        }
                        Text("${entry.eveningQuantity} L", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp))
                    }
                }
            }
            
            Box(modifier = Modifier.height(40.dp).width(1.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f)))
            
            // Type
            Column(modifier = Modifier.weight(1.2f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Type", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.LightGray)
                Text(entry.entryType.name.replace("_", " "), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            
            Box(modifier = Modifier.height(40.dp).width(1.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f)))
            
            // Days
            Column(modifier = Modifier.weight(0.8f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = androidx.compose.ui.graphics.Color.LightGray, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Days", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.LightGray)
                }
                Text("$days", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onEdit() }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(8.dp).size(16.dp))
                }
                Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onDelete() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp).size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CustomerHeader(customer: Customer, computedStatusString: String, onCall: () -> Unit, onWhatsApp: () -> Unit, onGenerateBill: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val initial = customer.name.firstOrNull()?.toString() ?: ""
            val avatarColor = when (customer.status) {
                com.example.data.model.CustomerStatus.ACTIVE -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                com.example.data.model.CustomerStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                com.example.data.model.CustomerStatus.INACTIVE -> MaterialTheme.colorScheme.error
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(androidx.compose.foundation.shape.CircleShape).background(avatarColor), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(initial, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = androidx.compose.ui.graphics.Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = customer.mobileNumber, color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = androidx.compose.ui.graphics.Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = customer.address, color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Surface(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Circle, contentDescription = null, tint = avatarColor, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = customer.status.name.lowercase().replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCall, 
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), contentColor = androidx.compose.ui.graphics.Color.White),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Button(
                    onClick = onWhatsApp, 
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), contentColor = androidx.compose.ui.graphics.Color.White),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_whatsapp), contentDescription = "WhatsApp", modifier = Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color.Unspecified)
                    Spacer(Modifier.width(4.dp))
                    Text("Text", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Button(
                    onClick = onGenerateBill, 
                    modifier = Modifier.weight(1f).height(48.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Bill", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Bill", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MilkSummary(stats: CustomerStats) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Milk Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Delivery Days", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("${stats.totalDeliveryDays} days", fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Gap Days", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("${stats.totalGapDays} days", fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Pause Days", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("${stats.totalPauseDays} days", fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Morning Milk", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("${String.format("%.2f", stats.totalMorningMilkDelivered)} L", fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Evening Milk", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("${String.format("%.2f", stats.totalEveningMilkDelivered)} L", fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Milk Delivered", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("${String.format("%.2f", stats.totalMilkDelivered)} L", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun FinancialSummaryParams(customer: Customer, stats: CustomerStats) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Financial Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Current Bill Amount", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("₹${String.format("%.2f", stats.currentBillAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Previous Due Amount", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("₹${customer.outstandingBalance}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Security Deposit", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("₹${stats.totalAdvanceAvailable}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount Paid", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("₹${stats.totalAmountPaid}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Outstanding Balance", color = androidx.compose.ui.graphics.Color.LightGray)
                val total = customer.outstandingBalance + stats.currentBillAmount - stats.totalAmountPaid
                Text("₹${String.format("%.2f", total)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppBottomSheet(customer: Customer, stats: CustomerStats, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    val totalOutstanding = customer.outstandingBalance + stats.currentBillAmount - stats.totalAmountPaid
    val currentBill = stats.currentBillAmount
    val previousDue = customer.outstandingBalance
    val advance = stats.totalAdvanceAvailable

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("WhatsApp Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            ListItem(
                headlineContent = { Text("Open Chat") },
                modifier = Modifier.clickable { onSend("") }
            )
            ListItem(
                headlineContent = { Text("Send Bill") },
                supportingContent = { Text("Sends current bill details") },
                modifier = Modifier.clickable {
                    val msg = "Namaste ${customer.name} Ji,\n\nAapka Singh Milk Dairy bill tayyar hai.\n\nCurrent Bill: ₹${String.format("%.2f", currentBill)}\nPrevious Due: ₹${previousDue}\nSecurity Deposit Available: ₹${advance}\nOutstanding: ₹${String.format("%.2f", totalOutstanding)}\n\nDhanyavaad.\nSingh Milk Dairy"
                    onSend(msg)
                }
            )
            ListItem(
                headlineContent = { Text("Send Payment Reminder") },
                modifier = Modifier.clickable {
                     val msg = "Namaste ${customer.name} Ji,\n\nAapka doodh payment pending hai.\n\nOutstanding Amount: ₹${String.format("%.2f", totalOutstanding)}\n\nKripya payment kar dein.\n\nDhanyavaad.\nSingh Milk Dairy"
                     onSend(msg)
                }
            )
            ListItem(
                headlineContent = { Text("Send Outstanding Reminder") },
                modifier = Modifier.clickable {
                    val msg = "Namaste ${customer.name} Ji,\n\nAapke account mein purana due available hai.\n\nPrevious Due: ₹${previousDue}\nCurrent Bill: ₹${String.format("%.2f", currentBill)}\nTotal Outstanding: ₹${String.format("%.2f", totalOutstanding)}\n\nDhanyavaad.\nSingh Milk Dairy"
                    onSend(msg)
                }
            )
        }
    }
}

@Composable
fun TransactionDialog(type: com.example.data.model.TransactionType, onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val title = if (type == com.example.data.model.TransactionType.PAYMENT) "Add Payment" else "Add Advance"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onSave(amt, notes)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdvanceSection(customer: Customer, stats: CustomerStats, transactions: List<com.example.data.model.TransactionEntry>, onEditCustomer: () -> Unit, onAddDeposit: () -> Unit, onDeleteTransaction: (com.example.data.model.TransactionEntry) -> Unit) {
    val advanceTransactions = transactions.filter { it.type == com.example.data.model.TransactionType.ADVANCE || it.type == com.example.data.model.TransactionType.ADVANCE_USED }.sortedByDescending { it.dateMillis }
    if (advanceTransactions.isEmpty() && customer.advanceBalance <= 0) return

    val totalAdvanceAdded = advanceTransactions.filter { it.type == com.example.data.model.TransactionType.ADVANCE }.sumOf { it.amount } + customer.advanceBalance
    val totalAdvanceUsed = advanceTransactions.filter { it.type == com.example.data.model.TransactionType.ADVANCE_USED }.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Security Deposit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            }
            Surface(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onEditCustomer() }
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.Top) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Total Security Deposit", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("₹${String.format("%.0f", totalAdvanceAdded)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                    }
                    Box(modifier = Modifier.height(40.dp).width(1.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f)))
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Deposit Used", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("₹${String.format("%.0f", totalAdvanceUsed)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                    }
                    Box(modifier = Modifier.height(40.dp).width(1.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f)))
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Available Deposit", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("₹${String.format("%.0f", stats.totalAdvanceAvailable)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Text("This amount is NOT deducted automatically from bills.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        
        if (advanceTransactions.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Security Deposit History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                }
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xFF7E57C2).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onAddDeposit() }
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("+ Add", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    advanceTransactions.forEachIndexed { index, t ->
                        val isUsed = t.type == com.example.data.model.TransactionType.ADVANCE_USED
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = androidx.compose.ui.graphics.Color(0xFF7E57C2).copy(alpha = 0.3f), modifier = Modifier.size(36.dp)) {
                                    Icon(if(isUsed) Icons.Default.Remove else Icons.Default.Add, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(if (isUsed) "Deposit Used" else "Deposit Added", fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
                                    Text(sdf.format(java.util.Date(t.dateMillis)), style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.LightGray)
                                }
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(if(isUsed) "- ₹${String.format("%.0f", t.amount)}" else "+ ₹${String.format("%.0f", t.amount)}", fontWeight = FontWeight.Bold, color = if(isUsed) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                Spacer(Modifier.width(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { /* mock edit */ }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(6.dp).size(14.dp))
                                    }
                                    Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onDeleteTransaction(t) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(6.dp).size(14.dp))
                                    }
                                }
                            }
                        }
                        if (index < advanceTransactions.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillingCycleCard(customer: Customer, onEdit: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Billing Cycle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                }
                Surface(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onEdit() }
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.1f))
            
            val cal = Calendar.getInstance()
            val startDay = customer.cycleStartDay
            val endDay = customer.cycleEndDay
            
            cal.set(Calendar.DAY_OF_MONTH, startDay)
            val startDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
            
            cal.set(Calendar.DAY_OF_MONTH, endDay)
            val endDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
            
            var cycleDays = endDay - startDay + 1
            if (cycleDays <= 0) {
                cycleDays += cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("Start Date", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(startDateStr, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                }
                Surface(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.CircleShape) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("End Date", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(endDateStr, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                }
            }
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.1f))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = androidx.compose.ui.graphics.Color.LightGray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cycle Days: $cycleDays days", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.LightGray)
            }
        }
    }
}

@Composable
fun FinancialSummarySection(customer: Customer, stats: CustomerStats, isSecurityDepositDeducted: Boolean, onDeductChanged: (Boolean) -> Unit, onMarkSettled: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Financial Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            }
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.1f))
            
            val totalBeforeAdvance = (customer.outstandingBalance + stats.currentBillAmount - stats.totalAmountPaid).coerceAtLeast(0.0)
            val advanceToDeduct = if (isSecurityDepositDeducted) minOf(stats.totalAdvanceAvailable, totalBeforeAdvance) else 0.0
            val finalAmount = totalBeforeAdvance - advanceToDeduct

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Total Amount", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("₹${String.format("%.0f", totalBeforeAdvance)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                }
                Box(modifier = Modifier.height(40.dp).width(1.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f)))
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1.5f)) {
                    Text("Adv. / Security Dep.", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("₹${String.format("%.0f", advanceToDeduct)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                }
                Box(modifier = Modifier.height(40.dp).width(1.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f)))
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Final Amount", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("₹${String.format("%.0f", finalAmount)}", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50), style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (stats.totalAdvanceAvailable > 0) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.clickable { onDeductChanged(!isSecurityDepositDeducted) }) {
                    Checkbox(checked = isSecurityDepositDeducted, onCheckedChange = onDeductChanged, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                    Text("Deduct Security Deposit (Available: ₹${String.format("%.0f", stats.totalAdvanceAvailable)})", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E3A2F)),
                modifier = Modifier.fillMaxWidth().clickable { onMarkSettled() }
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Mark as Settled", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                            Text("Payment received from customer", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.LightGray)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = androidx.compose.ui.graphics.Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun SettlementHistorySection(
    transactions: List<com.example.data.model.TransactionEntry>,
    onNavigateToSettlements: () -> Unit = {}
) {
    val payments = transactions.filter { it.type == com.example.data.model.TransactionType.PAYMENT }.sortedByDescending { it.dateMillis }
    if (payments.isEmpty()) return

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Settled History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                Text("View All", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onNavigateToSettlements() })
            }
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.2f))
            
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            payments.take(3).forEachIndexed { index, t ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(sdf.format(Date(t.dateMillis)), color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("₹${String.format("%.0f", t.amount)}", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        Surface(color = androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(t.paymentMode?.name ?: "CASH", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.LightGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Icon(Icons.Default.Circle, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50), modifier = Modifier.size(8.dp))
                    }
                }
                if (index < minOf(payments.size, 3) - 1) {
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.1f))
                }
            }
        }
    }
}

@Composable
fun SettlementDialog(defaultAmount: Double, onDismiss: () -> Unit, onSave: (Double, PaymentMode) -> Unit) {
    var amount by remember { mutableStateOf(if (defaultAmount > 0) String.format("%.0f", defaultAmount) else "") }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                Text("Mark as Settled")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select Payment Mode", fontWeight = FontWeight.SemiBold)
                
                // UPI Option
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedMode = PaymentMode.UPI }.border(1.dp, if(selectedMode == PaymentMode.UPI) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("UPI", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                            Text("Received via UPI / Online", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.LightGray)
                        }
                    }
                    RadioButton(selected = selectedMode == PaymentMode.UPI, onClick = { selectedMode = PaymentMode.UPI })
                }
                
                // CASH Option
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedMode = PaymentMode.CASH }.border(1.dp, if(selectedMode == PaymentMode.CASH) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Money, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Cash", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                            Text("Received via Cash", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.LightGray)
                        }
                    }
                    RadioButton(selected = selectedMode == PaymentMode.CASH, onClick = { selectedMode = PaymentMode.CASH })
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Enter Amount") },
                    leadingIcon = { Text("₹", modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(amt, selectedMode)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32))
            ) {
                Text("Mark as Settled")
            }
        },
        dismissButton = {}
    )
}

data class MonthlyStatementData(
    val monthYear: String,
    val period: String,
    val amount: Double,
    val isPaid: Boolean,
    val totalLiters: Double,
    val morningMilk: Double,
    val eveningMilk: Double,
    val extraMilk: Double,
    val gapDays: Int
)

fun calculateMonthlyStatements(customer: Customer, entries: List<DeliveryEntry>, transactions: List<TransactionEntry>): List<MonthlyStatementData> {
    if (entries.isEmpty()) return emptyList()
    
    val minTime = entries.minOf { it.fromDateMillis }
    val maxTime = System.currentTimeMillis() 
    
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = minTime
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    
    val maxCal = java.util.Calendar.getInstance()
    maxCal.timeInMillis = maxTime
    
    val list = mutableListOf<MonthlyStatementData>()
    
    while (cal.before(maxCal) || (cal.get(java.util.Calendar.MONTH) == maxCal.get(java.util.Calendar.MONTH) && cal.get(java.util.Calendar.YEAR) == maxCal.get(java.util.Calendar.YEAR))) {
        val monthStart = cal.timeInMillis
        val lastDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val calEnd = cal.clone() as java.util.Calendar
        calEnd.set(java.util.Calendar.DAY_OF_MONTH, lastDay)
        calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calEnd.set(java.util.Calendar.MINUTE, 59)
        calEnd.set(java.util.Calendar.SECOND, 59)
        val monthEnd = calEnd.timeInMillis
        
        var totalAmount = 0.0
        var totalLiters = 0.0
        var morningMilk = 0.0
        var eveningMilk = 0.0
        var extraMilk = 0.0
        var gapDays = 0
        
        for (e in entries) {
            val overlapStart = maxOf(monthStart, e.fromDateMillis)
            val overlapEnd = minOf(monthEnd, e.toDateMillis)
            
            if (overlapStart <= overlapEnd) {
                val d1 = java.util.Calendar.getInstance().apply { timeInMillis = overlapStart; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
                val d2 = java.util.Calendar.getInstance().apply { timeInMillis = overlapEnd; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
                val days = ((d2.timeInMillis - d1.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
                
                if (e.entryType == EntryType.NORMAL_DELIVERY || e.entryType == EntryType.QUANTITY_CHANGE) {
                    val mQty = if (e.session == DeliverySession.BOTH || e.session == DeliverySession.MORNING) e.morningQuantity else 0.0
                    val eQty = if (e.session == DeliverySession.BOTH || e.session == DeliverySession.EVENING) e.eveningQuantity else 0.0
                    morningMilk += mQty * days
                    eveningMilk += eQty * days
                    totalLiters += (mQty + eQty) * days
                    totalAmount += (mQty + eQty) * days * e.rate
                } else if (e.entryType == EntryType.EXTRA_DELIVERY) {
                    val mQty = if (e.session == DeliverySession.BOTH || e.session == DeliverySession.MORNING) e.morningQuantity else 0.0
                    val eQty = if (e.session == DeliverySession.BOTH || e.session == DeliverySession.EVENING) e.eveningQuantity else 0.0
                    extraMilk += (mQty + eQty) * days
                    totalLiters += (mQty + eQty) * days
                    totalAmount += (mQty + eQty) * days * e.rate
                } else if (e.entryType == EntryType.GAP || e.entryType == EntryType.PAUSE) {
                    gapDays += days
                }
            }
        }
        
        val monthPayments = transactions.filter { it.dateMillis in monthStart..monthEnd && it.type == TransactionType.PAYMENT }.sumOf { it.amount }
        val isPaid = totalAmount > 0 && monthPayments >= totalAmount 
        
        if (totalLiters > 0 || gapDays > 0) {
            list.add(MonthlyStatementData(
                monthYear = android.text.format.DateFormat.format("MMMM yyyy", monthStart).toString(),
                period = "${android.text.format.DateFormat.format("dd MMM yyyy", monthStart)} - ${android.text.format.DateFormat.format("dd MMM yyyy", monthEnd)}",
                amount = totalAmount,
                isPaid = isPaid,
                totalLiters = totalLiters,
                morningMilk = morningMilk,
                eveningMilk = eveningMilk,
                extraMilk = extraMilk,
                gapDays = gapDays
            ))
        }
        cal.add(java.util.Calendar.MONTH, 1)
    }
    return list.reversed()
}

@Composable
fun MonthlyStatementsSection(customer: Customer, entries: List<DeliveryEntry>, transactions: List<TransactionEntry>) {
    val statements = remember(entries, transactions) { calculateMonthlyStatements(customer, entries, transactions) }
    var selectedStatement by remember { mutableStateOf<MonthlyStatementData?>(null) }
    
    if (statements.isEmpty()) return
    
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Monthly Statements", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
                }
                Text("View All >", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { })
            }
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f))
            
            statements.forEachIndexed { index, stmt ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedStatement = stmt },
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha=0.03f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha=0.05f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("📅", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(stmt.monthYear, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Billing Period", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(stmt.period.replace(" - ", " → "), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Text("🥛 Total Milk", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("${stmt.totalLiters} L", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Text("💰 Total Amount", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("₹${String.format("%.0f", stmt.amount)}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Text(if (stmt.isPaid) "🟢 Status" else "✅ Status", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(if (stmt.isPaid) "Settled" else "Pending", color = if (stmt.isPaid) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End, verticalArrangement = Arrangement.Bottom, modifier = Modifier.align(androidx.compose.ui.Alignment.Bottom)) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.clickable { /* Generate Bill logic */ }.padding(8.dp)) {
                                    Text("Generate Bill", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                if (index < statements.size - 1) {
                    Spacer(Modifier.height(12.dp))
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = androidx.compose.ui.graphics.Color.LightGray)
                Spacer(Modifier.width(4.dp))
                Text("Each month statement is generated separately. Next month data is not included.", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color.LightGray)
            }
        }
    }
    
    if (selectedStatement != null) {
        FullStatementDialog(
            customer = customer,
            statement = selectedStatement!!,
            onDismiss = { selectedStatement = null }
        )
    }
}

@Composable
fun FullStatementDialog(customer: Customer, statement: MonthlyStatementData, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Statement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
                Spacer(Modifier.height(24.dp))
                
                Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text("Billing Month: ${statement.monthYear}", color = androidx.compose.ui.graphics.Color.LightGray)
                Text("Period: ${statement.period}", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.bodySmall)
                
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f))
                Spacer(Modifier.height(16.dp))
                
                Text("Milk Summary", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                Spacer(Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Morning Milk", color = androidx.compose.ui.graphics.Color.LightGray); Text("${statement.morningMilk} L") }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Evening Milk", color = androidx.compose.ui.graphics.Color.LightGray); Text("${statement.eveningMilk} L") }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Extra Milk", color = androidx.compose.ui.graphics.Color.LightGray); Text("${statement.extraMilk} L") }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Gap Days", color = androidx.compose.ui.graphics.Color.LightGray); Text("${statement.gapDays} Days") }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Delivered", fontWeight = FontWeight.SemiBold); Text("${statement.totalLiters} L", fontWeight = FontWeight.SemiBold) }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha=0.1f))
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Amount", color = androidx.compose.ui.graphics.Color.LightGray); Text("₹${String.format("%.2f", statement.amount)}") }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Security Deposit Used", color = androidx.compose.ui.graphics.Color.LightGray); Text("₹0.00") }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Final Amount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary); Text("₹${String.format("%.2f", statement.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) }
                
                Spacer(Modifier.height(24.dp))
                
                val statusColor = if (statement.isPaid) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFFF9800)
                Row(modifier = Modifier.fillMaxWidth().background(statusColor.copy(alpha=0.1f), RoundedCornerShape(8.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column {
                        Text("Payment Status", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text(if (statement.isPaid) "SETTLED" else "PENDING", fontWeight = FontWeight.Bold, color = statusColor)
                    }
                    if (statement.isPaid) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("Mode: UPI / Cash", color = androidx.compose.ui.graphics.Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Button(onClick = { /* Generate Bill logic */ }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                            Text("Generate Bill")
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { /* Share */ }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Share") }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { /* Print */ }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Print") }
                }
            }
        }
    }
}
