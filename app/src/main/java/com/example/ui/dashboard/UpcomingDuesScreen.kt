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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.data.model.PaymentMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingDuesScreen(
    viewModel: DashboardViewModel,
    onNavigateToCustomer: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val customers = stats.customers

    var searchQuery by remember { mutableStateOf("") }
    var customerToSettle by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var showSettlementDialog by remember { mutableStateOf(false) }

    val todayMillis = System.currentTimeMillis()
    val daysRemaining = { endDate: Long -> 
        val diff = endDate - todayMillis
        if (diff < 0) 0 else (diff / 86400000L).toInt()
    }

    val dueCustomers = remember(customers, searchQuery) {
        customers.filter { it.calculatedOutstanding > 0 }
            .filter { it.customer.name.contains(searchQuery, ignoreCase = true) }
            .sortedBy { daysRemaining(it.cycleEndDate) }
    }

    val totalDuesExpected = remember(dueCustomers) {
        dueCustomers.sumOf { it.calculatedOutstanding }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("🔔 Upcoming Dues List") },
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
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer...", color = Color.LightGray) },
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

            // Dynamic dues Summary card
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
                        Text("Total Outstanding Dues", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                        Text("₹${String.format("%.2f", totalDuesExpected)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "${dueCustomers.size} pending",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (dueCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "Great! All clear. No outstanding dues." else "No matching dues found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(dueCustomers) { item ->
                        val daysLeft = daysRemaining(item.cycleEndDate)
                        val daysLeftColor = when {
                            daysLeft == 0 -> MaterialTheme.colorScheme.error
                            daysLeft <= 3 -> Color(0xFFFFC107)
                            else -> Color(0xFF4CAF50)
                        }
                        
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCustomer(item.customer.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.customer.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Mob: ${item.customer.mobileNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${String.format("%.0f", item.calculatedOutstanding)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Circle, contentDescription = null, tint = daysLeftColor, modifier = Modifier.size(8.dp))
                                            Text(
                                                text = if (daysLeft == 0) "Ends Today" else "Ends in $daysLeft Days",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = daysLeftColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.LightGray.copy(alpha = 0.15f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val startStr = sdf.format(Date(item.cycleStartDate))
                                    val endStr = sdf.format(Date(item.cycleEndDate))
                                    Column {
                                        Text("Billing Period", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                        Text("$startStr → $endStr", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            customerToSettle = item
                                            showSettlementDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Settle", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Settlement Dialog
    if (showSettlementDialog && customerToSettle != null) {
        var settleAmount by remember { mutableStateOf(String.format("%.0f", customerToSettle!!.calculatedOutstanding)) }
        var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }

        AlertDialog(
            onDismissRequest = { showSettlementDialog = false },
            title = { Text("Quick Settlement: ${customerToSettle!!.customer.name}", color = Color.White, style = MaterialTheme.typography.titleMedium) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = settleAmount,
                        onValueChange = { settleAmount = it },
                        label = { Text("Amount Received (₹)") },
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
                        val parsedAmount = settleAmount.toDoubleOrNull()
                        if (parsedAmount != null && parsedAmount > 0) {
                            val transaction = TransactionEntry(
                                customerId = customerToSettle!!.customer.id,
                                amount = parsedAmount,
                                type = TransactionType.PAYMENT,
                                paymentMode = selectedMode,
                                dateMillis = System.currentTimeMillis(),
                                notes = "Settle via Upcoming Dues"
                            )
                            viewModel.updateTransaction(transaction)
                        }
                        showSettlementDialog = false
                        customerToSettle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Settle Up", color = MaterialTheme.colorScheme.onSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSettlementDialog = false
                    customerToSettle = null
                }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}
