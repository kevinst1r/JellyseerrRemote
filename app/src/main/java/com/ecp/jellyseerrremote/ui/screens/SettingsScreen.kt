package com.ecp.jellyseerrremote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecp.jellyseerrremote.R
import com.ecp.jellyseerrremote.data.RemoteMode
import com.ecp.jellyseerrremote.vm.DotStatus
import com.ecp.jellyseerrremote.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenLogin: () -> Unit
) {
    val s by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    StatusDot(dot = s.dot)
                    Spacer(modifier = Modifier.size(8.dp))
                    IconButton(
                        onClick = vm::requestCheck,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("↻", style = MaterialTheme.typography.titleMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = s.localUrl,
                onValueChange = vm::setLocalUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Local URL") },
                placeholder = { Text("http://192.168.1.50:5055") },
                singleLine = true
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Remote enabled", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = s.remoteEnabled,
                            onCheckedChange = vm::setRemoteEnabled
                        )
                    }

                    if (s.remoteEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Use Cloudflare Tunnel ID", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = s.remoteMode == RemoteMode.CLOUDFLARE,
                                onCheckedChange = { vm.setRemoteMode(if (it) RemoteMode.CLOUDFLARE else RemoteMode.CUSTOM) }
                            )
                        }

                        if (s.remoteMode == RemoteMode.CLOUDFLARE) {
                            OutlinedTextField(
                                value = s.tunnelId,
                                onValueChange = vm::setTunnelId,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focus ->
                                        if (focus.isFocused && (s.tunnelId.isEmpty() || s.tunnelId.contains("trycloudflare.com"))) {
                                            vm.setTunnelId("")
                                        }
                                    },
                                label = { Text("Tunnel ID") },
                                placeholder = { Text("e.g. abc123") },
                                singleLine = true
                            )
                            if (s.derivedRemoteUrl.isNotBlank()) {
                                Text(
                                    "Derived: ${s.derivedRemoteUrl}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = s.customRemoteUrl,
                                onValueChange = vm::setCustomRemoteUrl,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Custom Remote URL") },
                                placeholder = { Text("https://requests.mydomain.com") },
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Prefer local first", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = s.preferLocalFirst,
                    onCheckedChange = vm::setPreferLocalFirst
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusDot(dot = s.dot)
                        Text(
                            when (s.dot) {
                                DotStatus.GRAY -> "Not set"
                                DotStatus.YELLOW -> "Checking…"
                                DotStatus.GREEN -> "Connected"
                                DotStatus.RED -> "Disconnected"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (s.baseUsed.isNotBlank()) {
                        Text(
                            "Using: ${s.baseUsed}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (s.connectionDetail.isNotBlank()) {
                        Text(
                            s.connectionDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        "Login: ${if (s.isAuthed || vm.hasAuthCookie()) "Saved" else "Not saved"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (s.lastError.isNotBlank()) {
                        Text(
                            s.lastError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material3.Button(
                    onClick = onOpenLogin,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Login")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = vm::logout,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
private fun StatusDot(dot: DotStatus) {
    val color = when (dot) {
        DotStatus.GRAY -> MaterialTheme.colorScheme.outline
        DotStatus.YELLOW -> MaterialTheme.colorScheme.tertiary
        DotStatus.GREEN -> MaterialTheme.colorScheme.primary
        DotStatus.RED -> MaterialTheme.colorScheme.error
    }
    Spacer(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
    )
}
