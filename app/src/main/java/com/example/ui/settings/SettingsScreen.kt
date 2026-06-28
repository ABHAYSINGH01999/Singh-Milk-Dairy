package com.example.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.auth.AuthManager
import com.example.data.sync.SyncManager
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authManager: AuthManager,
    syncManager: SyncManager
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by authManager.currentUser.collectAsState()
    var isSyncing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var authMode by remember { mutableStateOf("SELECT") } // SELECT, PHONE_INPUT, OTP_INPUT, PROFILE_SETUP
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    // Auto-restore effect for new logins who already have a profile
    LaunchedEffect(currentUser) {
        if (currentUser != null && authMode != "PROFILE_SETUP") {
            val hasProfile = authManager.hasProfile()
            if (!hasProfile && currentUser?.phoneNumber != null) {
                authMode = "PROFILE_SETUP"
            } else {
                authMode = "SELECT"
                // Auto restore
                isSyncing = true
                syncManager.restoreFromCloud()
                isSyncing = false
            }
        } else if (currentUser == null) {
            authMode = "SELECT"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentUser == null) {
                Text("Sign in to enable Cloud Backup & Sync", style = MaterialTheme.typography.titleMedium)
                
                when (authMode) {
                    "SELECT" -> {
                        Button(
                            onClick = { authMode = "PHONE_INPUT" },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Continue with Mobile Number", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val success = authManager.signInWithGoogle()
                                    if (success) {
                                        val hasProfile = authManager.hasProfile()
                                        if (!hasProfile) {
                                            authMode = "PROFILE_SETUP"
                                        } else {
                                            isSyncing = true
                                            syncManager.restoreFromCloud()
                                            isSyncing = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Continue with Google", color = Color.White)
                        }
                    }
                    "PHONE_INPUT" -> {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Mobile Number (with country code e.g. +91)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null && phoneNumber.isNotEmpty()) {
                                    authManager.startPhoneAuth(phoneNumber, activity, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                            coroutineScope.launch {
                                                authManager.signInWithPhoneAuthCredential(credential)
                                            }
                                        }
                                        override fun onVerificationFailed(e: FirebaseException) {
                                            android.widget.Toast.makeText(context, "Verification Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                        override fun onCodeSent(verificationIdStr: String, token: PhoneAuthProvider.ForceResendingToken) {
                                            verificationId = verificationIdStr
                                            authMode = "OTP_INPUT"
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Send OTP", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { authMode = "SELECT" }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Cancel", color = Color.White) }
                    }
                    "OTP_INPUT" -> {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("Enter OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                                coroutineScope.launch {
                                    authManager.signInWithPhoneAuthCredential(credential)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Verify OTP", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { authMode = "PHONE_INPUT" }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Back", color = Color.White) }
                    }
                }
            } else if (authMode == "PROFILE_SETUP") {
                Text("Complete Profile Setup", style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name (Required)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = currentUser?.phoneNumber ?: currentUser?.email ?: "",
                    onValueChange = { },
                    label = { Text(if (currentUser?.phoneNumber != null) "Mobile Number" else "Email") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (currentUser?.phoneNumber != null) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Button(
                    onClick = {
                        if (fullName.isNotBlank()) {
                            coroutineScope.launch {
                                authManager.updateProfile(fullName, email)
                                authMode = "SELECT"
                                // Auto sync for the new profile
                                isSyncing = true
                                syncManager.restoreFromCloud()
                                isSyncing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = fullName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save & Continue", color = if (fullName.isNotBlank()) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (currentUser?.photoUrl != null) {
                                coil.compose.AsyncImage(
                                    model = currentUser?.photoUrl?.toString(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Default Profile Photo",
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text("Account Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(currentUser?.displayName ?: "User", style = MaterialTheme.typography.titleSmall)
                                if (!currentUser?.email.isNullOrEmpty()) {
                                    Text("Email: ${currentUser?.email}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (!currentUser?.phoneNumber.isNullOrEmpty()) {
                                    Text("Phone: ${currentUser?.phoneNumber}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sign In Status: ", style = MaterialTheme.typography.bodyMedium)
                                    Text("✅ Connected", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sync Status", fontWeight = FontWeight.Bold)
                        Text(if (isSyncing) "Syncing..." else "Up to date", color = if (isSyncing) Color(0xFFFF9800) else Color(0xFF4CAF50))
                    }
                    Text("Data is automatically backed up to the cloud. You can also manually sync if needed.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    // Sign Out Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                syncManager.clearLocalData()
                                authManager.signOut()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("🚪 Logout")
                    }
                }
            }
        }
    }
}
