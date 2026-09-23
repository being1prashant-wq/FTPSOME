package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FtpServer
import com.example.ui.components.TvButton
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvGoldTertiary
import com.example.ui.theme.TvStatusRed
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextSecondary

@Composable
fun SettingsScreen(
    savedServers: List<FtpServer>,
    onDeleteServer: (FtpServer) -> Unit,
    onClearHistory: () -> Unit,
    bufferSeconds: Int,
    onBufferSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var resumeEnabled by remember { mutableStateOf(true) }
    var stereoDownmixEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // Section 1: Streaming Buffer & RAM Optimization
        item {
            SettingsSection(title = "Network Streaming & Memory Buffer") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Controls the size of the rolling memory buffer. A 5GB movie streams with zero disk writes, keeping TV storage free.",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BufferOptionCard(
                            title = "Low-RAM TV",
                            duration = "15 Seconds",
                            memory = "~12 MB RAM",
                            isSelected = bufferSeconds == 15,
                            onClick = { onBufferSecondsChange(15) },
                            modifier = Modifier.weight(1f)
                        )
                        BufferOptionCard(
                            title = "Standard (Recommended)",
                            duration = "30 Seconds",
                            memory = "~24 MB RAM",
                            isSelected = bufferSeconds == 30,
                            onClick = { onBufferSecondsChange(30) },
                            modifier = Modifier.weight(1f)
                        )
                        BufferOptionCard(
                            title = "High Network",
                            duration = "60 Seconds",
                            memory = "~48 MB RAM",
                            isSelected = bufferSeconds == 60,
                            onClick = { onBufferSecondsChange(60) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 2: Audio & Codec Configuration
        item {
            SettingsSection(title = "Audio & AC3 Fallback") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AC3 / E-AC3 Stereo PCM Downmix",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Downmixes multichannel 5.1/7.1 Dolby surround audio to 2-channel stereo for TV speakers without clipping dialogue.",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = stereoDownmixEnabled,
                            onCheckedChange = { stereoDownmixEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = TvCyanPrimary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Resume Playback Prompt",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Remember where you left off and prompt to resume videos.",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = resumeEnabled,
                            onCheckedChange = { resumeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = TvCyanPrimary
                            )
                        )
                    }
                }
            }
        }

        // Section 3: Saved Servers Management
        item {
            SettingsSection(title = "Saved FTP Phone Servers (${savedServers.size})") {
                if (savedServers.isEmpty()) {
                    Text("No saved FTP servers.", color = TvTextSecondary, fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedServers.forEach { server ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TvSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Router, contentDescription = null, tint = TvCyanPrimary)
                                    Column {
                                        Text(server.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(server.displayAddress, color = TvTextSecondary, fontSize = 12.sp)
                                    }
                                }

                                TvButton(
                                    onClick = { onDeleteServer(server) },
                                    isPrimary = false,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TvStatusRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove", color = TvStatusRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Data Management & About
        item {
            SettingsSection(title = "Data Management & Information") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Clear Continue Watching History", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Deletes saved timestamps and playback bookmarks from TV.", color = TvTextSecondary, fontSize = 12.sp)
                    }

                    TvButton(
                        onClick = onClearHistory,
                        isPrimary = false,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear History", fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "FTP TV Player • Android TV 9 Edition",
                            color = TvCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Engineered specifically for low-end Android TV hardware. True progressive FTP streaming over local Wi-Fi with zero full-file storage consumption. Software fallback pipeline for AC3/E-AC3 audio.",
                            color = TvTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TvSurface)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            content()
        }
    }
}

@Composable
private fun BufferOptionCard(
    title: String,
    duration: String,
    memory: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        backgroundColor = if (isSelected) TvCyanPrimary.copy(alpha = 0.2f) else TvSurfaceVariant
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = if (isSelected) TvCyanPrimary else Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = duration,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = memory,
                color = TvTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
