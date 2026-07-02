package com.example.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    syncManager: SyncManager,
    onNavigateToAbout: () -> Unit,
    onNavigateToRecycleBin: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by authManager.currentUser.collectAsState()
    var isSyncing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Section
            if (currentUser != null) {
                AccountSection(
                    currentUser = currentUser,
                    isSyncing = isSyncing,
                    onSync = {
                        coroutineScope.launch {
                            isSyncing = true
                            syncManager.restoreFromCloud()
                            isSyncing = false
                            snackbarHostState.showSnackbar("Sync Completed")
                        }
                    },
                    onSignOut = {
                        coroutineScope.launch {
                            syncManager.clearLocalData()
                            authManager.signOut()
                        }
                    }
                )
            } else {
                Text("Sign in to enable Cloud Backup & Sync", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                // Simplified Auth UI for now since it's already implemented in AuthManager
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = authManager.signInWithGoogle()
                            if (success) {
                                isSyncing = true
                                syncManager.restoreFromCloud()
                                isSyncing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In with Google")
                }
            }

            HorizontalDivider()

            // Categories
            SettingsCategory("General") {
                SettingsItem(Icons.Default.Language, "Language", "System Default") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Language settings coming soon") }
                }
            }

            SettingsCategory("Appearance") {
                SettingsItem(Icons.Default.DarkMode, "Theme", "System Default") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Theme settings coming soon") }
                }
                SettingsItem(Icons.Default.ColorLens, "Dynamic Colors", "Enabled (Android 12+)") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Dynamic Colors settings coming soon") }
                }
            }

            SettingsCategory("Security") {
                SettingsItem(Icons.Default.Lock, "App Lock", "Coming Soon") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("App Lock coming soon") }
                }
                SettingsItem(Icons.Default.Fingerprint, "Fingerprint Lock", "Coming Soon") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Fingerprint Lock coming soon") }
                }
            }

            SettingsCategory("Data Management") {
                SettingsItem(Icons.Default.Backup, "Backup & Restore", "Coming Soon") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Backup & Restore coming soon") }
                }
                SettingsItem(Icons.Default.ImportExport, "Export Data", "Coming Soon") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Data Export coming soon") }
                }
                SettingsItem(Icons.Default.DeleteSweep, "Auto Delete", "Never") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Auto Delete settings coming soon") }
                }
                SettingsItem(Icons.Default.DeleteOutline, "Recycle Bin", "Manage deleted items", onClick = onNavigateToRecycleBin)
            }

            SettingsCategory("Support & About") {
                SettingsItem(Icons.Default.Help, "Help Center", "Coming Soon") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Help Center coming soon") }
                }
                SettingsItem(Icons.Default.BugReport, "Report Bug", "Coming Soon") {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Bug reporting coming soon") }
                }
                SettingsItem(Icons.Default.Info, "About", "Version, Developer Info", onClick = onNavigateToAbout)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AccountSection(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onSignOut: () -> Unit
) {
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
                        model = currentUser.photoUrl?.toString(),
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
                    Text(currentUser?.displayName ?: "User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!currentUser?.email.isNullOrEmpty()) {
                        Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!currentUser?.phoneNumber.isNullOrEmpty()) {
                        Text(currentUser?.phoneNumber ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sync Status", style = MaterialTheme.typography.bodyMedium)
                Text(if (isSyncing) "Syncing..." else "Up to date", color = if (isSyncing) Color(0xFFFF9800) else Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSync, modifier = Modifier.weight(1f)) {
                    Text("Sync Now")
                }
                Button(onClick = onSignOut, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
