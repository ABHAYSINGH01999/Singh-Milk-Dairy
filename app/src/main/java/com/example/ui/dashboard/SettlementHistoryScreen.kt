package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.ui.components.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.data.model.PaymentMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementHistoryScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val transactions = stats.transactions
    val customers = stats.customers

    var searchQuery by remember { mutableStateOf("") }
    var transToEdit by remember { mutableStateOf<TransactionEntry?>(null) }
    var transToDelete by remember { mutableStateOf<TransactionEntry?>(null) }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Filter transactions to show only payments (settlements)
    val settlements = remember(transactions, searchQuery, customers) {
        transactions.filter { it.type == TransactionType.PAYMENT }.filter { t ->
            val customerName = customers.find { it.customer.id == t.customerId }?.customer?.name ?: "Unknown"
            customerName.contains(searchQuery, ignoreCase = true) ||
            t.paymentMode?.name?.contains(searchQuery, ignoreCase = true) == true
        }.sortedByDescending { it.dateMillis }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("✅ Settlement History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by customer name...", color = Color.LightGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                )
            )

            // Total Collection Header
            val totalCollection = settlements.sumOf { it.amount }
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Settled Amount", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                        Text("₹${String.format("%.2f", totalCollection)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "${settlements.size} payments",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (settlements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "No settlements found." else "No matching settlements found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(settlements) { trans ->
                        val customerName = customers.find { it.customer.id == trans.customerId }?.customer?.name ?: "Deleted Customer"
                        
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = sdf.format(Date(trans.dateMillis)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                        Surface(
                                            color = Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = trans.paymentMode?.name ?: "CASH",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "₹${String.format("%.0f", trans.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    
                                    IconButton(onClick = { transToEdit = trans }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Settlement", tint = Color.LightGray)
                                    }
                                    IconButton(onClick = { transToDelete = trans }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Settlement", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Settlement Dialog
    if (transToEdit != null) {
        var editAmount by remember { mutableStateOf(String.format("%.0f", transToEdit!!.amount)) }
        var selectedMode by remember { mutableStateOf(transToEdit!!.paymentMode ?: PaymentMode.CASH) }
        
        AlertDialog(
            onDismissRequest = { transToEdit = null },
            title = { Text("Edit Settlement Entry", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    Text("Payment Mode", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedMode == PaymentMode.UPI) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = PaymentMode.UPI }
                                .padding(4.dp),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text("UPI", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedMode == PaymentMode.CASH) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = PaymentMode.CASH }
                                .padding(4.dp),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Money, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text("CASH", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAmount = editAmount.toDoubleOrNull()
                        if (parsedAmount != null && parsedAmount > 0) {
                            val updated = transToEdit!!.copy(
                                amount = parsedAmount,
                                paymentMode = selectedMode
                            )
                            viewModel.updateTransaction(updated)
                        }
                        transToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Update", color = MaterialTheme.colorScheme.onSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { transToEdit = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Delete Settlement Confirmation Dialog
    if (transToDelete != null) {
        val deleteCustName = customers.find { it.customer.id == transToDelete!!.customerId }?.customer?.name ?: "Unknown"
        AlertDialog(
            onDismissRequest = { transToDelete = null },
            title = { Text("Delete Settlement Entry?", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this payment of ₹${String.format("%.0f", transToDelete!!.amount)} for $deleteCustName?\n\nThis will restore the outstanding balance for this billing period.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transToDelete!!)
                        transToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}
