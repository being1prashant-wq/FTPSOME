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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FtpServer
import com.example.data.model.PlaybackBookmark
import com.example.ui.components.TvButton
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvCardBackground
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvGoldTertiary
import com.example.ui.theme.TvStatusGreen
import com.example.ui.theme.TvStatusRed
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextTertiary

@Composable
fun HomeScreen(
    connectedServer: FtpServer?,
    isConnecting: Boolean,
    connectionError: String?,
    savedServers: List<FtpServer>,
    recentBookmarks: List<PlaybackBookmark>,
    onConnectServer: (FtpServer) -> Unit,
    onDisconnect: () -> Unit,
    onOpenAddServerDialog: () -> Unit,
    onDeleteServer: (FtpServer) -> Unit,
    onNavigateToBrowser: () -> Unit,
    onResumeBookmark: (PlaybackBookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // 1. Hero Status Card
        item {
            HeroConnectionCard(
                connectedServer = connectedServer,
                isConnecting = isConnecting,
                connectionError = connectionError,
                onConnectNew = onOpenAddServerDialog,
                onBrowseFiles = onNavigateToBrowser,
                onDisconnect = onDisconnect,
                onRetry = {
                    connectedServer?.let { onConnectServer(it) }
                }
            )
        }

        // 2. Continue Watching / Recent Media (if available)
        if (recentBookmarks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Continue Watching",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(recentBookmarks, key = { it.fullPath }) { bookmark ->
                            RecentMediaCard(
                                bookmark = bookmark,
                                onClick = { onResumeBookmark(bookmark) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Saved FTP Servers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Phone Servers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TvButton(
                        onClick = onOpenAddServerDialog,
                        isPrimary = false,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Server", fontSize = 13.sp)
                    }
                }

                if (savedServers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvSurface, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved FTP servers yet. Tap 'Add Server' or use 'Quick Connect' above.",
                            color = TvTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(savedServers, key = { it.id }) { server ->
                            SavedServerCard(
                                server = server,
                                isCurrent = connectedServer?.id == server.id,
                                onConnect = { onConnectServer(server) },
                                onDelete = { onDeleteServer(server) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Phone Setup Guide Card
        item {
            PhoneSetupGuideCard()
        }
    }
}

@Composable
private fun HeroConnectionCard(
    connectedServer: FtpServer?,
    isConnecting: Boolean,
    connectionError: String?,
    onConnectNew: () -> Unit,
    onBrowseFiles: () -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when {
                                    isConnecting -> TvGoldTertiary
                                    connectedServer != null -> TvStatusGreen
                                    else -> TvStatusRed
                                },
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = when {
                            isConnecting -> "Connecting to Phone..."
                            connectedServer != null -> "Connected to ${connectedServer.name}"
                            else -> "No FTP Server Connected"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = when {
                        isConnecting -> "Establishing connection and querying phone file system..."
                        connectedServer != null -> "Live stream ready from ${connectedServer.displayAddress}. Stream 5GB+ media without downloading."
                        connectionError != null -> connectionError
                        else -> "Connect to an FTP server running on your phone to browse and stream movies, music, and photos directly."
                    },
                    color = if (connectionError != null) TvStatusRed else TvTextSecondary,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.width(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = TvCyanPrimary,
                        strokeWidth = 3.dp
                    )
                } else if (connectedServer != null) {
                    TvButton(
                        onClick = onBrowseFiles,
                        isPrimary = true
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Files")
                    }
                    TvButton(
                        onClick = onDisconnect,
                        isPrimary = false
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    if (connectionError != null) {
                        TvButton(
                            onClick = onRetry,
                            isPrimary = false
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                    TvButton(
                        onClick = onConnectNew,
                        isPrimary = true
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Connect to Phone")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMediaCard(
    bookmark: PlaybackBookmark,
    onClick: () -> Unit
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .height(140.dp)
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(TvCyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = TvCyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "${(bookmark.progressFraction * 100).toInt()}%",
                    color = TvCyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = bookmark.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${bookmark.formattedPosition} / ${bookmark.formattedDuration}",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }

            LinearProgressIndicator(
                progress = { bookmark.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = TvCyanPrimary,
                trackColor = TvSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavedServerCard(
    server: FtpServer,
    isCurrent: Boolean,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    TvFocusableCard(
        onClick = onConnect,
        modifier = Modifier
            .width(240.dp)
            .height(130.dp)
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(TvSurfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = if (isCurrent) TvStatusGreen else TvCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isCurrent) {
                    Text(
                        text = "ACTIVE",
                        color = TvStatusGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    text = server.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = server.displayAddress,
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = if (server.isAnonymous) "Anonymous" else "User: ${server.username}",
                color = TvTextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PhoneSetupGuideCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TvSurfaceVariant.copy(alpha = 0.6f))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = TvGoldTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "How to Stream Media from Any Phone to This TV",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GuideStepItem(
                    step = "1",
                    title = "Install FTP App on Phone",
                    desc = "Search 'WiFi FTP Server' or 'Primitive FTPd' on Google Play on your Android phone and install it."
                )
                GuideStepItem(
                    step = "2",
                    title = "Connect to Same Wi-Fi",
                    desc = "Ensure your phone and this TV are on the same local Wi-Fi router / network."
                )
                GuideStepItem(
                    step = "3",
                    title = "Tap Start & Connect",
                    desc = "Tap START in the phone app. Tap 'Connect to Phone' above and enter the IP (e.g. 192.168.1.5) & Port (e.g. 2121)."
                )
            }
        }
    }
}

@Composable
private fun GuideStepItem(
    step: String,
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier.width(220.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(TvCyanPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = step, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(text = desc, color = TvTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
