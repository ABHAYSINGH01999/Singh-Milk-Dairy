package com.example.ui.customers

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import com.example.ui.components.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeliveryEntry
import com.example.data.model.DeliverySession
import com.example.data.model.EntryType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeliveryEntryScreen(
    viewModel: CustomerViewModel,
    customerId: Int,
    entryId: Int = -1,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
    }

    val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val entries by viewModel.deliveryEntries.collectAsStateWithLifecycle()
    val entryToEdit = remember(entries, entryId) { entries.find { it.id == entryId } }

    var selectedType by remember { mutableStateOf(EntryType.NORMAL_DELIVERY) }
    var selectedSession by remember { mutableStateOf(DeliverySession.BOTH) }

    var morningQtyStr by remember { mutableStateOf("") }
    var eveningQtyStr by remember { mutableStateOf("") }

    val commonQuantities = listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "Custom")

    var expandedMorningQty by remember { mutableStateOf(false) }
    var expandedEveningQty by remember { mutableStateOf(false) }
    var isMorningCustom by remember { mutableStateOf(false) }
    var isEveningCustom by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val cal = Calendar.getInstance()
    var fromDateStr by remember { mutableStateOf(sdf.format(cal.time)) }
    var toDateStr by remember { mutableStateOf(sdf.format(cal.time)) }

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(customer, entryToEdit) {
        if (!isInitialized) {
            if (entryToEdit != null) {
                selectedType = entryToEdit.entryType
                selectedSession = entryToEdit.session
                morningQtyStr = entryToEdit.morningQuantity.toString()
                eveningQtyStr = entryToEdit.eveningQuantity.toString()
                fromDateStr = sdf.format(java.util.Date(entryToEdit.fromDateMillis))
                toDateStr = sdf.format(java.util.Date(entryToEdit.toDateMillis))
                if (morningQtyStr.isNotEmpty() && morningQtyStr !in commonQuantities) isMorningCustom = true
                if (eveningQtyStr.isNotEmpty() && eveningQtyStr !in commonQuantities) isEveningCustom = true
                isInitialized = true
            } else if (customer != null) {
                morningQtyStr = customer!!.morningQuantity.toString()
                eveningQtyStr = customer!!.eveningQuantity.toString()
                
                val mQty = customer!!.morningQuantity
                val eQty = customer!!.eveningQuantity
                selectedSession = when {
                    mQty > 0.0 && eQty == 0.0 -> DeliverySession.MORNING
                    eQty > 0.0 && mQty == 0.0 -> DeliverySession.EVENING
                    else -> DeliverySession.BOTH
                }
                
                if (morningQtyStr.isNotEmpty() && morningQtyStr !in commonQuantities && mQty > 0.0) isMorningCustom = true
                if (eveningQtyStr.isNotEmpty() && eveningQtyStr !in commonQuantities && eQty > 0.0) isEveningCustom = true
                
                isInitialized = true
            }
        }
    }

    fun showDatePicker(initialDateStr: String, onDateSelected: (String) -> Unit) {
        val dateParts = initialDateStr.split("/")
        var year = cal.get(Calendar.YEAR)
        var month = cal.get(Calendar.MONTH)
        var day = cal.get(Calendar.DAY_OF_MONTH)

        if (dateParts.size == 3) {
            try {
                day = dateParts[0].toInt()
                month = dateParts[1].toInt() - 1
                year = dateParts[2].toInt()
            } catch (e: Exception) {
                // Ignore parse errors, use today
            }
        }

        DatePickerDialog(context, { _, y, m, d ->
            val newCal = Calendar.getInstance()
            newCal.set(y, m, d)
            onDateSelected(sdf.format(newCal.time))
        }, year, month, day).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId > 0) "Edit Delivery Entry" else "Add Delivery Entry") },
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Entry Type", style = MaterialTheme.typography.titleSmall)
            var expandedType by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = selectedType.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    EntryType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ")) },
                            onClick = {
                                selectedType = type
                                expandedType = false
                            }
                        )
                    }
                }
            }

            Text("Session", style = MaterialTheme.typography.titleSmall)
            var expandedSession by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedSession,
                onExpandedChange = { expandedSession = !expandedSession }
            ) {
                OutlinedTextField(
                    value = selectedSession.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSession) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedSession,
                    onDismissRequest = { expandedSession = false }
                ) {
                    DeliverySession.values().forEach { session ->
                        DropdownMenuItem(
                            text = { Text(session.name) },
                            onClick = {
                                selectedSession = session
                                expandedSession = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = fromDateStr,
                    onValueChange = { fromDateStr = it },
                    label = { Text("From Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker(fromDateStr) { fromDateStr = it } }) {
                            Icon(Icons.Default.DateRange, "Select Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = toDateStr,
                    onValueChange = { toDateStr = it },
                    label = { Text("To Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker(toDateStr) { toDateStr = it } }) {
                            Icon(Icons.Default.DateRange, "Select Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            val fromDateMillis = try { sdf.parse(fromDateStr)?.time } catch (e: Exception) { null } ?: cal.timeInMillis
            val toDateMillis = try { sdf.parse(toDateStr)?.time } catch (e: Exception) { null } ?: fromDateMillis
            val daysDiff = ((toDateMillis - fromDateMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
            
            LaunchedEffect(daysDiff, selectedType) {
                // Smart auto-suggestion for Gap/Pause
                if (selectedType == EntryType.GAP || selectedType == EntryType.PAUSE) {
                    if (daysDiff in 1..7 && selectedType != EntryType.GAP) {
                        selectedType = EntryType.GAP
                    } else if (daysDiff > 7 && selectedType != EntryType.PAUSE) {
                        selectedType = EntryType.PAUSE
                    }
                }
            }

            var reasonStr by remember { mutableStateOf(entryToEdit?.reason ?: "") }
            var autoResume by remember { mutableStateOf(entryToEdit?.autoResume ?: true) }

            if (selectedType == EntryType.GAP || selectedType == EntryType.PAUSE) {
                OutlinedTextField(
                    value = reasonStr,
                    onValueChange = { reasonStr = it },
                    label = { Text("Reason (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (selectedType == EntryType.PAUSE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoResume = !autoResume }
                            .padding(vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoResume,
                            onCheckedChange = { autoResume = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto Resume After End Date")
                    }
                }
            }

            if (selectedType != EntryType.GAP && selectedType != EntryType.PAUSE) {
                if (selectedSession == DeliverySession.MORNING || selectedSession == DeliverySession.BOTH) {
                    Text("Morning Quantity", style = MaterialTheme.typography.titleSmall)
                    ExposedDropdownMenuBox(
                        expanded = expandedMorningQty,
                        onExpandedChange = { expandedMorningQty = !expandedMorningQty }
                    ) {
                        OutlinedTextField(
                            value = if (isMorningCustom) "Custom" else "$morningQtyStr L",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMorningQty) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMorningQty,
                            onDismissRequest = { expandedMorningQty = false }
                        ) {
                            commonQuantities.forEach { qty ->
                                DropdownMenuItem(
                                    text = { Text(if (qty == "Custom") "Custom" else "$qty L") },
                                    onClick = {
                                        if (qty == "Custom") {
                                            isMorningCustom = true
                                        } else {
                                            isMorningCustom = false
                                            morningQtyStr = qty
                                        }
                                        expandedMorningQty = false
                                    }
                                )
                            }
                        }
                    }
                    if (isMorningCustom) {
                        OutlinedTextField(
                            value = morningQtyStr,
                            onValueChange = { morningQtyStr = it },
                            label = { Text("Morning Quantity (Manual)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
                
                if (selectedSession == DeliverySession.BOTH) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (selectedSession == DeliverySession.EVENING || selectedSession == DeliverySession.BOTH) {
                    Text("Evening Quantity", style = MaterialTheme.typography.titleSmall)
                    ExposedDropdownMenuBox(
                        expanded = expandedEveningQty,
                        onExpandedChange = { expandedEveningQty = !expandedEveningQty }
                    ) {
                        OutlinedTextField(
                            value = if (isEveningCustom) "Custom" else "$eveningQtyStr L",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEveningQty) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedEveningQty,
                            onDismissRequest = { expandedEveningQty = false }
                        ) {
                            commonQuantities.forEach { qty ->
                                DropdownMenuItem(
                                    text = { Text(if (qty == "Custom") "Custom" else "$qty L") },
                                    onClick = {
                                        if (qty == "Custom") {
                                            isEveningCustom = true
                                        } else {
                                            isEveningCustom = false
                                            eveningQtyStr = qty
                                        }
                                        expandedEveningQty = false
                                    }
                                )
                            }
                        }
                    }
                    if (isEveningCustom) {
                        OutlinedTextField(
                            value = eveningQtyStr,
                            onValueChange = { eveningQtyStr = it },
                            label = { Text("Evening Quantity (Manual)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val rate = customer?.defaultRate ?: 60.0

                    val fromDate = try { sdf.parse(fromDateStr)?.time } catch (e: Exception) { null } ?: cal.timeInMillis
                    val toDate = try { sdf.parse(toDateStr)?.time } catch (e: Exception) { null } ?: fromDate
                    
                    val mQty = if (selectedType == EntryType.GAP || selectedType == EntryType.PAUSE) 0.0 else (morningQtyStr.toDoubleOrNull() ?: 0.0)
                    val eQty = if (selectedType == EntryType.GAP || selectedType == EntryType.PAUSE) 0.0 else (eveningQtyStr.toDoubleOrNull() ?: 0.0)

                    val entry = DeliveryEntry(
                        id = if (entryId > 0) entryId else 0,
                        customerId = customerId,
                        fromDateMillis = fromDate,
                        toDateMillis = toDate,
                        entryType = selectedType,
                        session = selectedSession,
                        morningQuantity = mQty,
                        eveningQuantity = eQty,
                        rate = rate,
                        reason = if (selectedType == EntryType.GAP || selectedType == EntryType.PAUSE) reasonStr else null,
                        autoResume = if (selectedType == EntryType.PAUSE) autoResume else true
                    )
                    viewModel.saveDeliveryEntry(entry)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (entryId > 0) "Update Entry" else "Save Entry")
            }
        }
    }
}
