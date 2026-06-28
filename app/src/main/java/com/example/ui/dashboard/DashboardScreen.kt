package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.ui.components.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Note
import com.example.data.model.NotePriority
import com.example.data.model.Customer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged

import com.example.data.repository.SearchManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    searchManager: SearchManager,
    modifier: Modifier = Modifier,
    onNavigateToCustomer: (Int) -> Unit = {},
    onNavigateToAddCustomer: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToSettlements: () -> Unit = {},
    onNavigateToDues: () -> Unit = {}
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    
    val dateStr = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_dairy_logo),
                            contentDescription = "Singh Milk Dairy Logo",
                            modifier = Modifier.size(56.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("SINGH MILK DAIRY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Since 1992", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text("Synced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text("Today 10:25 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                com.example.ui.components.UnifiedSearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    searchManager = searchManager
                )
            }
        }
    ) { padding ->
        var showAddNoteDialog by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val query = searchQuery.trim().lowercase()
            if (query.isNotEmpty()) {
                val filtered = stats.customers.filter { 
                    it.customer.name.lowercase().contains(query) || 
                    it.customer.mobileNumber.contains(query) ||
                    it.customer.address.lowercase().contains(query)
                }
                item {
                    Text("Search Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                }
                items(filtered.size) { i ->
                    val c = filtered[i]
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { 
                            searchManager.addSearchQuery(query)
                            onNavigateToCustomer(c.customer.id) 
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        ListItem(
                            headlineContent = { Text(c.customer.name, color = Color.White) },
                            supportingContent = { Text(c.customer.mobileNumber, color = Color.LightGray) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            } else {
                
                // CARD 1: TODAY'S DELIVERY OVERVIEW
                item {
                    var mC = 0
                    var mM = 0.0
                    var eC = 0
                    var eM = 0.0
                    stats.customers.forEach { 
                        if(it.todayMorningReq > 0) { mC++; mM += it.todayMorningReq }
                        if(it.todayEveningReq > 0) { eC++; eM += it.todayEveningReq }
                    }
                    val tC = stats.customers.count { it.todayMorningReq > 0 || it.todayEveningReq > 0 }
                    
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚚 Today's Delivery Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha=0.2f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("☀️ ", fontSize = 16.sp)
                                        Text("Morning", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("$mC", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("Customers", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${String.format("%.1f", mM)} L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("Milk", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                }
                                
                                Box(modifier = Modifier.width(1.dp).height(80.dp).background(Color.LightGray.copy(alpha=0.2f)))
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌙 ", fontSize = 16.sp)
                                        Text("Evening", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("$eC", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("Customers", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${String.format("%.1f", eM)} L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("Milk", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                }
                                
                                Box(modifier = Modifier.width(1.dp).height(80.dp).background(Color.LightGray.copy(alpha=0.2f)))
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🥛 ", fontSize = 16.sp)
                                        Text("Total Today", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("$tC", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("Customers", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${String.format("%.1f", mM+eM)} L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("Milk", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }

                // CARD 2: UPCOMING DUES
                item {
                    val todayMillis = System.currentTimeMillis()
                    val daysRemaining = { endDate: Long -> 
                        val diff = endDate - todayMillis
                        if (diff < 0) 0 else (diff / 86400000L).toInt()
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    var alertRange by remember { mutableIntStateOf(7) }
                    
                    val allDue = stats.customers.filter { it.calculatedOutstanding > 0 && daysRemaining(it.cycleEndDate) <= alertRange }
                    
                    val dueToday = allDue.filter { daysRemaining(it.cycleEndDate) == 0 }
                    val due3Days = allDue.filter { daysRemaining(it.cycleEndDate) in 1..3 }
                    val due7Days = allDue.filter { daysRemaining(it.cycleEndDate) in 4..7 }
                    
                    val expectedCollection = allDue.sumOf { it.calculatedOutstanding }
                    
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔔 Upcoming Dues", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color.LightGray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("View All >", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onNavigateToDues() })
                                    Box {
                                        var showMenu by remember { mutableStateOf(false) }
                                        Text("$alertRange Days ▼", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { showMenu = true })
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                            listOf(3, 5, 7, 10).forEach { days ->
                                                DropdownMenuItem(text = { Text("$days Days") }, onClick = { alertRange = days; showMenu = false })
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha=0.2f))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Today", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("${dueToday.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Next 3", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("${due3Days.size}", fontWeight = FontWeight.Bold, color = Color(0xFFFFC107), style = MaterialTheme.typography.titleMedium)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Next 7", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("${due7Days.size}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Expected Collection", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                                Text("₹${String.format("%.0f", expectedCollection)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium)
                            }
                            
                            if (expanded && allDue.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha=0.2f))
                                val sortedDue = allDue.sortedBy { daysRemaining(it.cycleEndDate) }
                                sortedDue.forEach { c ->
                                    val daysLeft = daysRemaining(c.cycleEndDate)
                                    val statusColor = when {
                                        daysLeft == 0 -> MaterialTheme.colorScheme.error
                                        daysLeft <= 3 -> Color(0xFFFFC107)
                                        else -> Color(0xFF4CAF50)
                                    }
                                    val statusText = when {
                                        daysLeft == 0 -> "Cycle Ends Today"
                                        else -> "Due in $daysLeft Days"
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onNavigateToCustomer(c.customer.id) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Circle, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(c.customer.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(statusText, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                            }
                                        }
                                        Text("₹${String.format("%.0f", c.calculatedOutstanding)}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECURITY DEPOSIT CUSTOMERS
                item {
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔒 Security Deposits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha=0.2f))
                            
                            val advanceCustomers = stats.customers.filter { it.advanceBalance > 0 }.take(3)
                            advanceCustomers.forEach { c ->
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(c.customer.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    Text("₹${String.format("%.0f", c.advanceBalance)}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if(advanceCustomers.isEmpty()) {
                                Text("No deposits yet", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha=0.2f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Active Deposits", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                                Text("₹${String.format("%.0f", stats.totalAdvanceAvailable)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                // ROW 2: MILK SUPPLY & BILLING OVERVIEW
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // MILK SUPPLY OVERVIEW
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(150.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                                Text("🥛 Milk Supply", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.height(8.dp))
                                
                                var mM = 0.0; var eM = 0.0
                                stats.customers.forEach { 
                                    if(it.todayMorningReq > 0) { mM += it.todayMorningReq }
                                    if(it.todayEveningReq > 0) { eM += it.todayEveningReq }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Today", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("${String.format("%.1f", mM+eM)} L", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("This Mth", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("~ L", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Last Mth", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("~ L", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        // BILLING OVERVIEW
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(150.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            val pending = stats.customers.sumOf { if(it.calculatedOutstanding > 0) it.calculatedOutstanding else 0.0 }
                            val expected = stats.currentMonthCollection + pending
                            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                                Text("📄 Billing", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.height(8.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cur Mth Value", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("₹${String.format("%.0f", stats.thisMonthMilkValue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pending", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("₹${String.format("%.0f", pending)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Expected", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Text("₹${String.format("%.0f", expected)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // CARD 6: TODAY'S SETTLEMENT
                item {
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("✅ Today's Settlement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("View All >", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onNavigateToSettlements() })
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha=0.2f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Settled Customers", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                                        Text("${stats.todaySettledCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Pending Customers", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                                        val pendingCustomers = stats.customers.count { it.calculatedOutstanding > 0 }
                                        Text("${pendingCustomers}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha=0.2f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Total UPI Collection", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    Text("₹${String.format("%.0f", stats.todayUPIToday)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.2f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("Total Cash Collection", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    Text("₹${String.format("%.0f", stats.todayCashToday)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }

                // CARD 7: NOTES
                item {
                    NotesCard(stats, onAddNoteClick = { showAddNoteDialog = true }, onViewAllNotes = onNavigateToNotes)
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        
        if (showAddNoteDialog) {
            AddNoteDialog(
                customers = stats.customers.map { it.customer },
                onDismiss = { showAddNoteDialog = false },
                onSave = { note ->
                    viewModel.addNote(note)
                    showAddNoteDialog = false
                }
            )
        }
    }
}

@Composable
fun NotesCard(stats: DashboardStats, onAddNoteClick: () -> Unit, onViewAllNotes: () -> Unit = {}) {
    val totalNotes = stats.notes.size
    val todayMillis = System.currentTimeMillis()
    val pendingNotes = stats.notes.count { it.reminderDateMillis != null && it.reminderDateMillis <= todayMillis }

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("📝 Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("View All >", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onViewAllNotes() })
                    Text("Add Note >", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onAddNoteClick() })
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.2f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Notes", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                    Text("$totalNotes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Column {
                    Text("Pending Notes", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
                    Text("$pendingNotes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFFFC107).copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
            }
            
            if (stats.notes.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.2f))
                val recentNotes = stats.notes.take(3)
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                
                recentNotes.forEach { note ->
                    val customer = stats.customers.find { it.customer.id == note.customerId }?.customer?.name ?: "Unknown"
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(customer, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(note.title, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        }
                        if (note.reminderDateMillis != null) {
                            Text(sdf.format(Date(note.reminderDateMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(customers: List<Customer>, onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<Int?>(null) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(NotePriority.MEDIUM) }
    var reminderDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState()
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            confirmButton = {
                TextButton(onClick = { 
                    reminderDateMillis = datePickerState.selectedDateMillis 
                    showDatePicker = false
                }) { Text("OK", color = MaterialTheme.colorScheme.onSurface) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurface,
                    subheadContentColor = MaterialTheme.colorScheme.onSurface,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.secondary,
                    todayDateBorderColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Customer Dropdown
                ExposedDropdownMenuBox(
                    expanded = showCustomerDropdown,
                    onExpandedChange = { showCustomerDropdown = it }
                ) {
                    val customerName = customers.find { it.id == selectedCustomerId }?.name ?: "Select Customer *"
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCustomerDropdown,
                        onDismissRequest = { showCustomerDropdown = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text(cust.name) },
                                onClick = {
                                    selectedCustomerId = cust.id
                                    showCustomerDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Note Details") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
                
                OutlinedTextField(
                    value = if (reminderDateMillis != null) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(reminderDateMillis!!)) else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reminder Date (Optional)") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )
                
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NotePriority.values().forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = priority == p, onClick = { priority = p })
                            Text(p.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedCustomerId != null && title.isNotBlank()) {
                    onSave(Note(
                        customerId = selectedCustomerId!!,
                        title = title,
                        details = details,
                        reminderDateMillis = reminderDateMillis,
                        priority = priority
                    ))
                }
            }) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

