package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.CloudBackendConfig
import com.example.data.remote.NetworkStatus
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedBurgundy
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftIvory
import com.example.ui.viewmodel.DashboardStats
import com.example.ui.viewmodel.ThemePreference

@Composable
fun ProfileScreen(
    isAdmin: Boolean,
    adminEmail: String?,
    isSupabaseAvailable: Boolean = true,
    themePreference: ThemePreference,
    stats: DashboardStats,
    networkStatus: NetworkStatus,
    cloudConfig: CloudBackendConfig,
    onLoginAdminWithEmail: (email: String, password: String, onResult: ((Boolean, String?) -> Unit)?) -> Unit,
    onSendPasswordReset: (email: String) -> Unit,
    onLogoutAdmin: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onSaveCloudConfig: (CloudBackendConfig) -> Unit,
    onRefreshCloudSync: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit
) {
    var showLoginDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showCloudSettingsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var loginEmail by remember { mutableStateOf("Winter") }
    var loginPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    var resetEmail by remember { mutableStateOf(adminEmail ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_user_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ElevatedSurface)
                                .border(1.dp, if (isAdmin) ChampagneGold else BorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isAdmin) ChampagneGold else MutedTaupe,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAdmin) "Enterprise Administrator" else "Private Member",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SoftIvory
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isAdmin) (adminEmail ?: "Winter")
                                else "Read & Download Access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedTaupe
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Status Pill & Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isAdmin) ChampagneGold else MutedTaupe
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdmin) "Full Platform Authority" else "Verified Read Access",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAdmin) ChampagneGold else MutedTaupe
                            )
                        }

                        if (isAdmin) {
                            TextButton(
                                onClick = onLogoutAdmin,
                                modifier = Modifier.testTag("admin_logout_button")
                            ) {
                                Text("Sign Out", color = MutedBurgundy, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    loginPassword = ""
                                    loginError = null
                                    showLoginDialog = true
                                },
                                modifier = Modifier.testTag("admin_login_trigger")
                            ) {
                                Text("Admin Sign In", color = ChampagneGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToAdminDashboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("open_admin_dashboard_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChampagneGold,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Admin Management Console", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Firebase & Cloud Backend Status Card
        item {
            Text(
                text = "Cloud Infrastructure",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftIvory
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (networkStatus) {
                                    NetworkStatus.ONLINE -> Icons.Default.CloudDone
                                    NetworkStatus.SYNCING -> Icons.Default.CloudSync
                                    NetworkStatus.OFFLINE -> Icons.Default.Cloud
                                },
                                contentDescription = null,
                                tint = ChampagneGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isSupabaseAvailable) "Supabase Cloud Connected" else "Cloud Synchronizer Active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftIvory
                                )
                                Text(
                                    text = if (isSupabaseAvailable) "PostgreSQL • Storage: sqplus-documents • Realtime Live" else "Encrypted Local Cache & Sync Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedTaupe
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshCloudSync,
                            modifier = Modifier
                                .testTag("refresh_cloud_sync_btn")
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Now", tint = ChampagneGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showCloudSettingsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("configure_cloud_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Cloud Architecture Details", color = SoftIvory)
                    }
                }
            }
        }

        // About & Guide
        item {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftIvory
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ClickableSettingsRow(
                        icon = Icons.Default.Info,
                        title = "About SQPlus Private Edition",
                        onClick = { showAboutDialog = true }
                    )
                    SettingsDivider()
                    ClickableSettingsRow(
                        icon = Icons.Default.Lock,
                        title = "Security & Encryption Protocol",
                        onClick = { showPrivacyDialog = true }
                    )
                    SettingsDivider()
                    ClickableSettingsRow(
                        icon = Icons.Default.Help,
                        title = "User & Admin Guide",
                        onClick = { showHelpDialog = true }
                    )
                }
            }
        }
    }

    // Admin Login Dialog
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ChampagneGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Authentication", color = SoftIvory)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Sign in as administrator (User: Winter) with the password configured in your environment keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTaupe
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = loginEmail,
                        onValueChange = { loginEmail = it; loginError = null },
                        label = { Text("Username or Email") },
                        placeholder = { Text("Winter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GraphiteCard,
                            unfocusedContainerColor = GraphiteCard,
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = SoftIvory,
                            unfocusedTextColor = SoftIvory
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it; loginError = null },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GraphiteCard,
                            unfocusedContainerColor = GraphiteCard,
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = SoftIvory,
                            unfocusedTextColor = SoftIvory
                        )
                    )

                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(loginError!!, color = MutedBurgundy, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            showLoginDialog = false
                            resetEmail = loginEmail
                            showResetDialog = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", color = ChampagneGold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (loginEmail.isBlank() || loginPassword.isBlank()) {
                            loginError = "Please enter both credentials."
                            return@Button
                        }
                        isAuthenticating = true
                        loginError = null
                        onLoginAdminWithEmail(loginEmail, loginPassword) { success, err ->
                            isAuthenticating = false
                            if (success) {
                                showLoginDialog = false
                            } else {
                                loginError = err ?: "Authentication failed."
                            }
                        }
                    },
                    enabled = !isAuthenticating,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack)
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ObsidianBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Authenticating...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Authenticate", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) { Text("Cancel", color = MutedTaupe) }
            },
            containerColor = ElevatedSurface
        )
    }

    // Password Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Admin Password", color = SoftIvory) },
            text = {
                Column {
                    Text(
                        text = "Enter your administrator email. A secure password reset link will be dispatched to your inbox via Supabase Authentication.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTaupe
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Admin Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GraphiteCard,
                            unfocusedContainerColor = GraphiteCard,
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = SoftIvory,
                            unfocusedTextColor = SoftIvory
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            onSendPasswordReset(resetEmail.trim())
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack)
                ) {
                    Text("Send Reset Link", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = MutedTaupe) }
            },
            containerColor = ElevatedSurface
        )
    }

    // Cloud Backend Settings Dialog
    if (showCloudSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showCloudSettingsDialog = false },
            title = { Text("Cloud Architecture Specifications", color = SoftIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Backend: Supabase PostgreSQL Database", color = SoftIvory, fontWeight = FontWeight.SemiBold)
                    Text("• Storage Bucket: sqplus-documents", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                    Text("• Admin Authentication: Supabase Auth (User: Winter)", color = ChampagneGold, style = MaterialTheme.typography.bodySmall)
                    Text("• Realtime Sync: Supabase WebSocket Engine (Phoenix Channels)", color = SoftIvory, style = MaterialTheme.typography.bodySmall)
                    Text("• Security: PostgreSQL Row Level Security (RLS) & Storage Policies", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloudSettingsDialog = false }) { Text("Close", color = ChampagneGold) }
            },
            containerColor = ElevatedSurface
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("SQPlus Help & User Guide", color = SoftIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Private Cloud Vault: Real-time synchronization of enterprise files and channels.", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                    Text("• Downloads: Downloaded assets are saved to phone storage with SHA-256 verification.", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                    Text("• Admin Governance: Only administrator 'Winter' can create channels and upload files.", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Close", color = ChampagneGold) }
            },
            containerColor = ElevatedSurface
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Security Architecture", color = SoftIvory) },
            text = {
                Text("SQPlus enforces TLS transport layer encryption and role-based access control. No third-party trackers or telemetry are embedded.", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close", color = ChampagneGold) }
            },
            containerColor = ElevatedSurface
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About SQPlus", color = SoftIvory) },
            text = {
                Column {
                    Text("SQPlus Private Vault Edition", fontWeight = FontWeight.Bold, color = ChampagneGold)
                    Text("Version 3.0.0 (Luxury Edition)", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Engineered with an Obsidian Black & Champagne Gold visual aesthetic, Supabase Cloud integration, and server-grade encryption.", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK", color = ChampagneGold) }
            },
            containerColor = ElevatedSurface
        )
    }
}

@Composable
private fun ClickableSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ChampagneGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftIvory
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MutedTaupe,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderSubtle.copy(alpha = 0.6f))
    )
}
