package com.example.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPlannerScreen(viewModel: DeliveryPlannerViewModel) {
    val items by viewModel.plannerItems.collectAsStateWithLifecycle()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Morning", "Evening", "Total Today")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Delivery Planner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.primary, // Logo Blue
                contentColor = Color.White,
                indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        modifier = Modifier.background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                title, 
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> MorningTab(items, viewModel)
                1 -> EveningTab(items, viewModel)
                2 -> TotalTodayTab(items)
            }
        }
    }
}

@Composable
fun MorningTab(items: List<PlannerItem>, viewModel: DeliveryPlannerViewModel) {
    val morningItems = items.filter { it.morningQuantity > 0 }
    
    val totalCustomers = morningItems.size
    val totalMilk = morningItems.sumOf { it.morningQuantity }

    Column(modifier = Modifier.fillMaxSize()) {
        SummaryCard(title = "Morning Summary", customers = totalCustomers, milkTotal = totalMilk)
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(morningItems) { item ->
                DeliveryItemCard(
                    customerName = item.customer.name,
                    quantity = item.morningQuantity,
                    status = item.morningStatus,
                    onStatusChange = { newStatus -> viewModel.updateStatus(item.customer.id, "MORNING", newStatus) },
                    allowShift = true
                )
            }
        }
    }
}

@Composable
fun EveningTab(items: List<PlannerItem>, viewModel: DeliveryPlannerViewModel) {
    val eveningRegular = items.filter { it.eveningQuantity > 0 }
    val pendingFromMorning = items.filter { it.morningQuantity > 0 && (it.morningStatus == "PENDING" || it.morningStatus == "SHIFTED_TO_EVENING") }

    val totalCustomers = eveningRegular.size + pendingFromMorning.size
    val totalMilk = eveningRegular.sumOf { it.eveningQuantity } + pendingFromMorning.sumOf { it.morningQuantity }

    Column(modifier = Modifier.fillMaxSize()) {
        SummaryCard(title = "Evening Summary", customers = totalCustomers, milkTotal = totalMilk)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (pendingFromMorning.isNotEmpty()) {
                item {
                    Text(
                        text = "⚠ Pending From Morning",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                    )
                }
                items(pendingFromMorning) { item ->
                    DeliveryItemCard(
                        customerName = item.customer.name,
                        quantity = item.morningQuantity,
                        status = item.morningStatus,
                        onStatusChange = { newStatus -> viewModel.updateStatus(item.customer.id, "MORNING", newStatus) },
                        allowShift = false
                    )
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Evening Deliveries",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            items(eveningRegular) { item ->
                DeliveryItemCard(
                    customerName = item.customer.name,
                    quantity = item.eveningQuantity,
                    status = item.eveningStatus,
                    onStatusChange = { newStatus -> viewModel.updateStatus(item.customer.id, "EVENING", newStatus) },
                    allowShift = false
                )
            }
        }
    }
}

@Composable
fun TotalTodayTab(items: List<PlannerItem>) {
    val totalCustomers = items.count { it.morningQuantity > 0 || it.eveningQuantity > 0 }
    val totalMorning = items.sumOf { it.morningQuantity }
    val totalEvening = items.sumOf { it.eveningQuantity }
    val totalMilk = totalMorning + totalEvening

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Today's Requirements", style = MaterialTheme.typography.titleLarge, color = Color.White)
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Customers:", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                    Text("$totalCustomers", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Morning Milk Total:", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                    Text("${totalMorning}L", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Evening Milk Total:", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                    Text("${totalEvening}L", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Milk Required:", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("${totalMilk}L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, customers: Int, milkTotal: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Customers: $customers", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
            }
            Text("${milkTotal}L", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun DeliveryItemCard(
    customerName: String,
    quantity: Double,
    status: String,
    onStatusChange: (String) -> Unit,
    allowShift: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customerName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("${quantity}L", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status == "SHIFTED_TO_EVENING") {
                    Text("🔄 Shifted", color = Color(0xFFE65100), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                }

                if (allowShift && status != "DELIVERED") {
                    IconButton(
                        onClick = { 
                            if (status == "SHIFTED_TO_EVENING") onStatusChange("PENDING") 
                            else onStatusChange("SHIFTED_TO_EVENING") 
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Shift", 
                            tint = if (status == "SHIFTED_TO_EVENING") Color(0xFFE65100) else Color.LightGray
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (status == "DELIVERED") onStatusChange("PENDING") else onStatusChange("DELIVERED")
                    }
                ) {
                    if (status == "DELIVERED") {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Delivered", tint = Color(0xFF4CAF50))
                    } else {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Pending", tint = Color.LightGray)
                    }
                }
            }
        }
    }
}
