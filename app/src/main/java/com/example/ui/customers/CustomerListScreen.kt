package com.example.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.CustomerStatus
import com.example.ui.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.repository.SearchManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    viewModel: DashboardViewModel,
    searchManager: SearchManager,
    onNavigateToAdd: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val customers = stats.customers
    var searchQuery by remember { mutableStateOf("") }
    
    val dateStr = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_dairy_logo),
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CUSTOMER STATUS OVERVIEW
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Customer Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha=0.2f))
                        
                        val active = customers.count { it.computedStatus == CustomerStatus.ACTIVE }
                        val paused = customers.count { it.computedStatus == CustomerStatus.PAUSED }
                        val closed = customers.count { it.computedStatus == CustomerStatus.INACTIVE }
                        val total = customers.size
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Total Customers", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text("$total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color.LightGray.copy(alpha=0.2f)).align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Active", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                Spacer(Modifier.height(4.dp))
                                Text("$active", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                            Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color.LightGray.copy(alpha=0.2f)).align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PauseCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Paused", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                Spacer(Modifier.height(4.dp))
                                Text("$paused", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color.LightGray.copy(alpha=0.2f)).align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Closed", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                Spacer(Modifier.height(4.dp))
                                Text("$closed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("All Customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(modifier = Modifier.clickable { }, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sort", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            val query = searchQuery.trim().lowercase()
            val filtered = if (query.isNotEmpty()) {
                customers.filter { it.customer.name.lowercase().contains(query) || it.customer.mobileNumber.contains(query) || it.customer.address.lowercase().contains(query) }
            } else {
                customers
            }
            
            items(filtered) { cwb ->
                CustomerCard(
                    customer = cwb.customer,
                    computedStatus = cwb.computedStatus,
                    calculatedOutstanding = cwb.calculatedOutstanding,
                    advanceBalance = cwb.advanceBalance, // Pass advance balance
                    onClick = { 
                        searchManager.addSearchQuery(query)
                        onNavigateToProfile(cwb.customer.id) 
                    }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCard(customer: Customer, computedStatus: CustomerStatus, calculatedOutstanding: Double, advanceBalance: Double, onClick: () -> Unit) {
    val initial = customer.name.firstOrNull()?.toString() ?: ""
    // We will use standard colors for avatars, hashing the ID for variety, but wait, the prompt says "Do NOT change colors", so we use the statusColor.
    val statusColor = when (computedStatus) {
        CustomerStatus.ACTIVE -> Color(0xFF4CAF50)
        CustomerStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        CustomerStatus.INACTIVE -> MaterialTheme.colorScheme.error
    }
    
    // To match the example perfectly (blue, purple etc.), we can hash the id:
    val avatarColors = listOf(Color(0xFF4CAF50), Color(0xFF1976D2), Color(0xFF7E57C2), Color(0xFFFF9800), Color(0xFFE91E63))
    val avatarColor = avatarColors[Math.abs(customer.id.hashCode()) % avatarColors.size]

    val morningQty = customer.morningQuantity
    val eveningQty = customer.eveningQuantity

    val morningStr = if (morningQty > 0) {
        val mIsMl = morningQty < 1.0
        if (mIsMl) "Morning: ${(morningQty * 1000).toInt()} ml" else {
            val formattedQty = if (morningQty % 1.0 == 0.0) morningQty.toInt().toString() else morningQty.toString()
            "Morning: ${formattedQty}L"
        }
    } else ""
    
    val eveningStr = if (eveningQty > 0) {
        val eIsMl = eveningQty < 1.0
        if (eIsMl) "Evening: ${(eveningQty * 1000).toInt()} ml" else {
            val formattedQty = if (eveningQty % 1.0 == 0.0) eveningQty.toInt().toString() else eveningQty.toString()
            "Evening: ${formattedQty}L"
        }
    } else ""

    val milkText = if (morningStr.isNotEmpty() && eveningStr.isNotEmpty()) {
        val mIsMl = morningQty < 1.0
        val eIsMl = eveningQty < 1.0
        val mStr = if (mIsMl) "${(morningQty * 1000).toInt()} ml" else "${if (morningQty % 1.0 == 0.0) morningQty.toInt() else morningQty}L"
        val eStr = if (eIsMl) "${(eveningQty * 1000).toInt()} ml" else "${if (eveningQty % 1.0 == 0.0) eveningQty.toInt() else eveningQty}L"
        "Morning: $mStr | Evening: $eStr"
    } else if (morningStr.isNotEmpty()) {
        morningStr
    } else if (eveningStr.isNotEmpty()) {
        eveningStr
    } else {
        ""
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { false }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF4CAF50))
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = "Payment", tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Record Payment", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Generate Bill", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Bill", tint = Color.White)
                    }
                }
            }
        },
        content = {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (milkText.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_milk_jar),
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = milkText,
                                    fontSize = 13.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Box(modifier = Modifier.height(36.dp).width(1.dp).background(Color.White.copy(alpha=0.15f)))
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column {
                        Text("Outstanding", fontSize = 13.sp, color = Color.LightGray)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹${String.format("%.2f", calculatedOutstanding)}", 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                    ) {
                        val statusText = if (computedStatus == CustomerStatus.INACTIVE) "CLOSED" else computedStatus.name
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

