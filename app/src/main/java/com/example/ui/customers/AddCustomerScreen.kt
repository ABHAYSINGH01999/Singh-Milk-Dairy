package com.example.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import com.example.ui.components.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingCycle
import com.example.data.model.Customer
import com.example.data.model.CustomerStatus
import java.util.Calendar

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Contacts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkQuantityDropdown(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val options = listOf("0L", "0.5L", "1L", "1.5L", "2L", "2.5L", "3L", "3.5L", "4L", "Custom")
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember {
        mutableStateOf(
            if (options.contains("${value}L") || value == "0.0") {
                if (value == "0.0") "0L" else "${value}L"
            } else if (value.isNotEmpty()) "Custom" else "0L"
        )
    }

    LaunchedEffect(value) {
        val mappedOption = if (value == "0.0") "0L" else "${value}L"
        if (options.contains(mappedOption)) {
            selectedOption = mappedOption
        } else if (value.isNotEmpty() && value != "0") {
            selectedOption = "Custom"
        }
    }

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedOption = option
                            expanded = false
                            if (option != "Custom") {
                                onValueChange(option.removeSuffix("L"))
                            } else {
                                onValueChange("")
                            }
                        }
                    )
                }
            }
        }
        if (selectedOption == "Custom") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("$label (Custom)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    viewModel: CustomerViewModel,
    customerId: Int = -1,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(customerId) {
        if (customerId != -1) {
            viewModel.loadCustomer(customerId)
        }
    }
    
    val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var altMobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("60.0") }
    var morningQty by remember { mutableStateOf("0.0") }
    var eveningQty by remember { mutableStateOf("0.0") }
    
    var selectedBillingCycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var cycleStartDay by remember { mutableStateOf("1") }
    var cycleEndDay by remember { mutableStateOf("31") }
    
    var isInitialized by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            if (uri != null) {
                // Query the contact Details
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    if (idIndex != -1 && nameIndex != -1) {
                        val contactId = cursor.getString(idIndex)
                        val contactName = cursor.getString(nameIndex)
                        name = contactName ?: ""

                        if (hasPhoneIndex != -1 && cursor.getInt(hasPhoneIndex) > 0) {
                            val pCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(contactId),
                                null
                            )
                            if (pCursor != null && pCursor.moveToFirst()) {
                                val phoneIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneIndex != -1) {
                                    val phone = pCursor.getString(phoneIndex)
                                    mobile = phone?.replace("[^0-9]".toRegex(), "") ?: ""
                                    if(mobile.length > 10) {
                                        mobile = mobile.takeLast(10)
                                    }
                                }
                                pCursor.close()
                            }
                        }
                        
                        // Address
                        val aCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        if (aCursor != null && aCursor.moveToFirst()) {
                            val addressIndex = aCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                            if (addressIndex != -1) {
                                val contactAddress = aCursor.getString(addressIndex)
                                address = contactAddress ?: ""
                            }
                            aCursor.close()
                        }
                    }
                    cursor.close()
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            }
        }
    )
    
    LaunchedEffect(customer, customerId) {
        if (customerId != -1 && customer != null && !isInitialized) {
            name = customer!!.name
            mobile = customer!!.mobileNumber
            altMobile = customer!!.alternateNumber ?: ""
            address = customer!!.address
            rate = customer!!.defaultRate.toString()
            morningQty = customer!!.morningQuantity.toString()
            eveningQty = customer!!.eveningQuantity.toString()
            selectedBillingCycle = customer!!.billingCycle
            cycleStartDay = customer!!.cycleStartDay.toString()
            cycleEndDay = customer!!.cycleEndDay.toString()
            isInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (customerId != -1) "Edit Customer" else "Add Customer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            if (customerId == -1) {
                Button(
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                                contactPickerLauncher.launch(null)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = "Import From Contacts")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import From Contacts")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = altMobile,
                    onValueChange = { altMobile = it },
                    label = { Text("Alternate Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Full Address") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text("Billing Cycle", style = MaterialTheme.typography.titleSmall)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedBillingCycle.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    BillingCycle.values().forEach { cycle ->
                        DropdownMenuItem(
                            text = { Text(cycle.name) },
                            onClick = {
                                selectedBillingCycle = cycle
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedBillingCycle == BillingCycle.CUSTOM) {
                var showDateRangePicker by remember { mutableStateOf(false) }
                val dateRangePickerState = rememberDateRangePickerState()

                if (showDateRangePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDateRangePicker = false },
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        confirmButton = {
                            TextButton(onClick = {
                                val start = dateRangePickerState.selectedStartDateMillis
                                val end = dateRangePickerState.selectedEndDateMillis
                                if (start != null && end != null) {
                                    val calStart = java.util.Calendar.getInstance().apply { timeInMillis = start }
                                    val calEnd = java.util.Calendar.getInstance().apply { timeInMillis = end }
                                    cycleStartDay = calStart.get(java.util.Calendar.DAY_OF_MONTH).toString()
                                    cycleEndDay = calEnd.get(java.util.Calendar.DAY_OF_MONTH).toString()
                                }
                                showDateRangePicker = false
                            }) {
                                Text("OK", color = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDateRangePicker = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    ) {
                        DateRangePicker(
                            state = dateRangePickerState,
                            modifier = Modifier.weight(1f),
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
                                dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSurface,
                                dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                                todayContentColor = MaterialTheme.colorScheme.secondary,
                                todayDateBorderColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = cycleStartDay,
                            onValueChange = {},
                            label = { Text("Start Day") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showDateRangePicker = true })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = cycleEndDay,
                            onValueChange = {},
                            label = { Text("End Day") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showDateRangePicker = true })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Rate per Litre") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    MilkQuantityDropdown(
                        label = "Morning Qty",
                        value = morningQty,
                        onValueChange = { morningQty = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MilkQuantityDropdown(
                        label = "Evening Qty",
                        value = eveningQty,
                        onValueChange = { eveningQty = it }
                    )
                }
            }
            
            var advanceAmount by remember { mutableStateOf("") }
            
            if (customerId == -1) {
                OutlinedTextField(
                    value = advanceAmount,
                    onValueChange = { advanceAmount = it },
                    label = { Text("Advance Security Amount (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    val newCustomer = Customer(
                        id = if (customerId != -1) customerId else 0,
                        name = name,
                        mobileNumber = mobile,
                        alternateNumber = altMobile,
                        address = address,
                        defaultRate = rate.toDoubleOrNull() ?: 60.0,
                        morningQuantity = morningQty.toDoubleOrNull() ?: 0.0,
                        eveningQuantity = eveningQty.toDoubleOrNull() ?: 0.0,
                        notes = customer?.notes ?: "",
                        billingCycle = selectedBillingCycle,
                        cycleStartDay = cycleStartDay.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                        cycleEndDay = cycleEndDay.toIntOrNull()?.coerceIn(1, 31) ?: 31,
                        status = customer?.status ?: CustomerStatus.ACTIVE,
                        customerSince = customer?.customerSince ?: cal.timeInMillis,
                        advanceBalance = customer?.advanceBalance ?: 0.0,
                        outstandingBalance = customer?.outstandingBalance ?: 0.0
                    )
                    viewModel.saveCustomer(newCustomer, advanceAmount.toDoubleOrNull() ?: 0.0)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Customer")
            }
        }
    }
}
