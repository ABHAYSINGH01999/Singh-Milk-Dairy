package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.TransactionEntry
import com.example.data.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: RecycleBinViewModel,
    onNavigateBack: () -> Unit
) {
    val deletedCustomers by viewModel.deletedCustomers.collectAsState()
    val deletedDeliveryEntries by viewModel.deletedDeliveryEntries.collectAsState()
    val deletedTransactions by viewModel.deletedTransactions.collectAsState()
    val deletedNotes by viewModel.deletedNotes.collectAsState()
    
    var showEmptyConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val totalItems = deletedCustomers.size + deletedDeliveryEntries.size + deletedTransactions.size + deletedNotes.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (totalItems > 0) {
                        IconButton(onClick = { showEmptyConfirm = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Empty Recycle Bin")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (totalItems == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Recycle Bin is Empty", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (deletedCustomers.isNotEmpty()) {
                    item { Text("Customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(deletedCustomers) { customer ->
                        RecycleBinItem(
                            title = customer.name,
                            subtitle = "Customer - ${customer.mobileNumber}",
                            deletedAt = customer.deletedAt ?: 0L,
                            onRestore = { 
                                viewModel.restoreCustomer(customer)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Customer Restored") }
                            },
                            onPermanentDelete = { viewModel.permanentlyDeleteCustomer(customer.id) }
                        )
                    }
                }
                
                if (deletedDeliveryEntries.isNotEmpty()) {
                    item { Text("Delivery Entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(deletedDeliveryEntries) { entry ->
                        RecycleBinItem(
                            title = "Delivery Entry ID: ${entry.id}",
                            subtitle = "Entry - ${entry.session}",
                            deletedAt = entry.deletedAt ?: 0L,
                            onRestore = { 
                                viewModel.restoreDeliveryEntry(entry)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Entry Restored") }
                            },
                            onPermanentDelete = { viewModel.permanentlyDeleteDeliveryEntry(entry) }
                        )
                    }
                }

                if (deletedTransactions.isNotEmpty()) {
                    item { Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(deletedTransactions) { tx ->
                        RecycleBinItem(
                            title = "${tx.type}: ₹${tx.amount}",
                            subtitle = "Transaction - ${tx.paymentMode}",
                            deletedAt = tx.deletedAt ?: 0L,
                            onRestore = { 
                                viewModel.restoreTransaction(tx)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Transaction Restored") }
                            },
                            onPermanentDelete = { viewModel.permanentlyDeleteTransaction(tx) }
                        )
                    }
                }
                
                if (deletedNotes.isNotEmpty()) {
                    item { Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(deletedNotes) { note ->
                        RecycleBinItem(
                            title = note.title,
                            subtitle = "Note - Priority: ${note.priority}",
                            deletedAt = note.deletedAt ?: 0L,
                            onRestore = { 
                                viewModel.restoreNote(note)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Note Restored") }
                            },
                            onPermanentDelete = { viewModel.permanentlyDeleteNote(note) }
                        )
                    }
                }
            }
        }
        
        if (showEmptyConfirm) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirm = false },
                title = { Text("Empty Recycle Bin?") },
                text = { Text("All items in the recycle bin will be permanently deleted. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.emptyRecycleBin()
                            showEmptyConfirm = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Recycle bin emptied") }
                        }
                    ) {
                        Text("Delete Forever", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RecycleBinItem(
    title: String,
    subtitle: String,
    deletedAt: Long,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (deletedAt > 0) {
                    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(deletedAt))
                    Text(text = "Deleted: $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Permanently?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPermanentDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
