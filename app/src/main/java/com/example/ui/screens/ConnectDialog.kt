package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FtpServer
import com.example.ui.components.TvButton
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvGoldTertiary
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextSecondary

@Composable
fun ConnectDialog(
    initialServer: FtpServer? = null,
    onDismiss: () -> Unit,
    onConnect: (FtpServer) -> Unit
) {
    var name by remember { mutableStateOf(initialServer?.name ?: "Phone FTP") }
    var host by remember { mutableStateOf(initialServer?.host ?: "192.168.1.") }
    var port by remember { mutableStateOf(initialServer?.port?.toString() ?: "2121") }
    var isAnonymous by remember { mutableStateOf(initialServer?.isAnonymous ?: true) }
    var username by remember { mutableStateOf(initialServer?.username ?: "") }
    var password by remember { mutableStateOf(initialServer?.password ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = TvSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, TvCyanPrimary.copy(alpha = 0.5f)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (initialServer == null) "Connect to Phone FTP Server" else "Edit FTP Server",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Helpful instructions for low-end TV users
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TvGoldTertiary
                    )
                    Text(
                        text = "Open any FTP Server app on your phone (e.g. WiFi FTP Server), start it, and enter the IP & Port shown on your phone.",
                        fontSize = 13.sp,
                        color = TvTextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Server IP Address") },
                        placeholder = { Text("e.g. 192.168.1.10") },
                        modifier = Modifier.weight(2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = tvTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { Text("2121") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = tvTextFieldColors(),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anonymous Login (No password required)",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = TvCyanPrimary
                        )
                    )
                }

                if (!isAnonymous) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.weight(1f),
                            colors = tvTextFieldColors(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f),
                            colors = tvTextFieldColors(),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name / Label (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tvTextFieldColors(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvButton(
                        onClick = onDismiss,
                        isPrimary = false
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TvButton(
                        onClick = {
                            val cleanHost = host.trim()
                            val cleanPort = port.trim().toIntOrNull() ?: 2121
                            if (cleanHost.isBlank()) {
                                errorMessage = "Please enter an IP address"
                                return@TvButton
                            }
                            val server = FtpServer(
                                id = initialServer?.id ?: 0L,
                                name = if (name.isBlank()) "Phone ($cleanHost)" else name.trim(),
                                host = cleanHost,
                                port = cleanPort,
                                username = username.trim(),
                                password = password,
                                isAnonymous = isAnonymous,
                                lastConnectedAt = System.currentTimeMillis()
                            )
                            onConnect(server)
                        },
                        isPrimary = true
                    ) {
                        Text("Connect & Save")
                    }
                }
            }
        }
    }
}

@Composable
fun tvTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TvCyanPrimary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = TvCyanPrimary,
    unfocusedLabelColor = TvTextSecondary,
    cursorColor = TvCyanPrimary
)
