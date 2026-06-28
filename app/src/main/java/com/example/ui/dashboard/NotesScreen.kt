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
import com.example.data.model.Note
import com.example.data.model.NotePriority
import com.example.data.model.Customer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val notes = stats.notes
    val customers = stats.customers.map { it.customer }

    var searchQuery by remember { mutableStateOf("") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToView by remember { mutableStateOf<Note?>(null) }

    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    val filteredNotes = remember(notes, searchQuery, stats.customers) {
        notes.filter { note ->
            val custName = stats.customers.find { it.customer.id == note.customerId }?.customer?.name ?: ""
            note.title.contains(searchQuery, ignoreCase = true) ||
            note.details.contains(searchQuery, ignoreCase = true) ||
            custName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("📝 Milk Diary Notes") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
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
                placeholder = { Text("Search by customer, title or content...", color = Color.LightGray) },
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

            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "No notes added yet.\nTap + to add a note." else "No matching notes found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes) { note ->
                        val customerName = stats.customers.find { it.customer.id == note.customerId }?.customer?.name ?: "General Note"
                        
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { noteToView = note },
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
                                            text = customerName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { noteToEdit = note }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = Color.LightGray)
                                        }
                                        IconButton(onClick = { noteToDelete = note }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = note.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Created: ${sdf.format(Date(note.createdAtMillis))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray.copy(alpha = 0.7f)
                                    )
                                    if (note.lastUpdatedMillis > note.createdAtMillis) {
                                        Text(
                                            text = "Updated: ${sdf.format(Date(note.lastUpdatedMillis))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Note
    if (showAddNoteDialog) {
        AddNoteDialog(
            customers = customers,
            onDismiss = { showAddNoteDialog = false },
            onSave = { note ->
                viewModel.addNote(note)
                showAddNoteDialog = false
            }
        )
    }

    // Dialog: Edit Note
    if (noteToEdit != null) {
        var editTitle by remember { mutableStateOf(noteToEdit!!.title) }
        var editDetails by remember { mutableStateOf(noteToEdit!!.details) }
        
        AlertDialog(
            onDismissRequest = { noteToEdit = null },
            title = { Text("Edit Note", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Note Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = editDetails,
                        onValueChange = { editDetails = it },
                        label = { Text("Note Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = noteToEdit!!.copy(
                            title = editTitle,
                            details = editDetails,
                            lastUpdatedMillis = System.currentTimeMillis()
                        )
                        viewModel.updateNote(updated)
                        noteToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToEdit = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Dialog: View Note Details
    if (noteToView != null) {
        val viewCustName = stats.customers.find { it.customer.id == noteToView!!.customerId }?.customer?.name ?: "General Note"
        AlertDialog(
            onDismissRequest = { noteToView = null },
            title = {
                Column {
                    Text(viewCustName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(noteToView!!.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(noteToView!!.details, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    Text("Created: ${sdf.format(Date(noteToView!!.createdAtMillis))}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    if (noteToView!!.lastUpdatedMillis > noteToView!!.createdAtMillis) {
                        Text("Last Updated: ${sdf.format(Date(noteToView!!.lastUpdatedMillis))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { noteToView = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("Close", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
        )
    }

    // Dialog: Delete Note Confirmation
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?", color = Color.White) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = { Text("Are you sure you want to permanently delete this note? This action cannot be undone.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(noteToDelete!!)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}
