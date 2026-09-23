package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FtpFileItem
import com.example.data.model.PlaybackBookmark
import com.example.player.FtpPlayerManager
import com.example.ui.components.TvButton
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvSkySecondary
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextSecondary

@Composable
fun MusicPlayerScreen(
    playerManager: FtpPlayerManager,
    playlist: List<FtpFileItem>,
    onExitPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by playerManager.uiState.collectAsState()

    BackHandler {
        onExitPlayer()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_angle"
    )

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface)
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        // Left Column: Vinyl Player & Controls
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvButton(
                    onClick = onExitPlayer,
                    isPrimary = false,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back to Files", fontSize = 13.sp)
                }
            }

            // Spinning Vinyl Record
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF222634), Color(0xFF0F131D), Color(0xFF07090E))
                        )
                    )
                    .border(3.dp, TvCyanPrimary.copy(alpha = 0.5f), CircleShape)
                    .rotate(if (uiState.isPlaying) discRotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(TvCyanPrimary.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Track Title & Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = uiState.currentTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Streaming over FTP from phone",
                    color = TvTextSecondary,
                    fontSize = 13.sp
                )
            }

            // Slider & Timers
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Slider(
                    value = if (uiState.durationMs > 0) (uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        val target = (fraction * uiState.durationMs).toLong()
                        playerManager.seekTo(target)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = TvCyanPrimary,
                        activeTrackColor = TvCyanPrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = PlaybackBookmark.formatTime(uiState.currentPositionMs),
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = PlaybackBookmark.formatTime(uiState.durationMs),
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Transport Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvButton(
                    onClick = { playerManager.playPrevious() },
                    enabled = uiState.hasPrevious,
                    isPrimary = false
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }

                TvButton(
                    onClick = { playerManager.seekRelative(-10_000) },
                    isPrimary = false
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s")
                }

                TvButton(
                    onClick = { playerManager.togglePlayPause() },
                    isPrimary = true,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }

                TvButton(
                    onClick = { playerManager.seekRelative(10_000) },
                    isPrimary = false
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Forward 10s")
                }

                TvButton(
                    onClick = { playerManager.playNext() },
                    enabled = uiState.hasNext,
                    isPrimary = false
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }

        // Right Column: Folder Playback Queue
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(TvSurfaceVariant, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = TvCyanPrimary)
                Text(
                    text = "Folder Playlist (${playlist.size} tracks)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(playlist, key = { _, item -> item.fullPath }) { index, item ->
                    val isCurrent = item.name == uiState.currentTitle
                    TvFocusableCard(
                        onClick = {
                            playerManager.playNext()
                        },
                        backgroundColor = if (isCurrent) TvCyanPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (isCurrent) TvCyanPrimary else TvTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = item.name,
                                    color = if (isCurrent) TvCyanPrimary else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = item.formattedSize,
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
