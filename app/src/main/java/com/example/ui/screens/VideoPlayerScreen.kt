package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.PlaybackBookmark
import com.example.player.FtpPlayerManager
import com.example.player.TrackInfo
import com.example.ui.components.TvButton
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvGoldTertiary
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerManager: FtpPlayerManager,
    initialBookmark: PlaybackBookmark? = null,
    onExitPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by playerManager.uiState.collectAsState()
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showResumeDialog by remember {
        mutableStateOf(initialBookmark != null && initialBookmark.positionMs > 10_000 && !initialBookmark.isCompleted)
    }

    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Auto-hide controls after 5 seconds of inactivity
    LaunchedEffect(areControlsVisible, lastInteractionTime, uiState.isPlaying) {
        if (areControlsVisible && uiState.isPlaying && !showResumeDialog && !showAudioDialog && !showSubtitleDialog) {
            delay(5000)
            areControlsVisible = false
        }
    }

    fun pingInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (!areControlsVisible) {
            areControlsVisible = true
        }
    }

    BackHandler {
        when {
            showAudioDialog -> showAudioDialog = false
            showSubtitleDialog -> showSubtitleDialog = false
            showResumeDialog -> showResumeDialog = false
            areControlsVisible -> areControlsVisible = false
            else -> onExitPlayer()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    pingInteraction()
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (!areControlsVisible) {
                                playerManager.seekRelative(-10_000)
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (!areControlsVisible) {
                                playerManager.seekRelative(10_000)
                                true
                            } else false
                        }
                        Key.MediaPlayPause -> {
                            playerManager.togglePlayPause()
                            true
                        }
                        Key.MediaPlay -> {
                            playerManager.play()
                            true
                        }
                        Key.MediaPause -> {
                            playerManager.pause()
                            true
                        }
                        Key.MediaFastForward -> {
                            playerManager.seekRelative(15_000)
                            true
                        }
                        Key.MediaRewind -> {
                            playerManager.seekRelative(-15_000)
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            if (!areControlsVisible) {
                                areControlsVisible = true
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { pingInteraction() }
            )
    ) {
        // 1. AndroidView ExoPlayer PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // We use our custom modern TV OSD
                    setResizeMode(resizeMode)
                    player = playerManager.getPlayer()
                }
            },
            update = { playerView ->
                playerView.player = playerManager.getPlayer()
                playerView.setResizeMode(resizeMode)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Buffering Spinner
        if (uiState.isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = TvCyanPrimary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Buffering stream...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Error Banner (Non-fatal)
        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TvGoldTertiary)
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 4. TV OSD Controls Overlay
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                // Top Header: Back Button & Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TvButton(
                            onClick = onExitPlayer,
                            isPrimary = false,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Player", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back", fontSize = 13.sp)
                        }

                        Text(
                            text = uiState.currentTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Top action buttons: Audio, Subtitles, Aspect Ratio
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvButton(
                            onClick = { showAudioDialog = true },
                            isPrimary = false,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Audio (${uiState.audioTracks.size})", fontSize = 12.sp)
                        }

                        TvButton(
                            onClick = { showSubtitleDialog = true },
                            isPrimary = false,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            val subLabel = if (uiState.selectedSubtitleTrackIndex >= 0) "Sub: ON" else "Sub: OFF"
                            Text(subLabel, fontSize = 12.sp)
                        }

                        TvButton(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            isPrimary = false,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            val modeLabel = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                else -> "Fit"
                            }
                            Text(modeLabel, fontSize = 12.sp)
                        }
                    }
                }

                // Bottom Controls: Progress & Action Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Timeline Slider & Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = PlaybackBookmark.formatTime(uiState.currentPositionMs),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = PlaybackBookmark.formatTime(uiState.durationMs),
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Slider(
                        value = if (uiState.durationMs > 0) (uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            pingInteraction()
                            val newPosition = (fraction * uiState.durationMs).toLong()
                            playerManager.seekTo(newPosition)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = TvCyanPrimary,
                            activeTrackColor = TvCyanPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action Buttons Row (Play, Pause, Prev, Next, Rewind, Fast Forward)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvButton(
                            onClick = { playerManager.playPrevious() },
                            enabled = uiState.hasPrevious,
                            isPrimary = false,
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                        }
                        Spacer(Modifier.width(12.dp))

                        TvButton(
                            onClick = { playerManager.seekRelative(-10_000) },
                            isPrimary = false,
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s")
                        }
                        Spacer(Modifier.width(12.dp))

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
                        Spacer(Modifier.width(12.dp))

                        TvButton(
                            onClick = { playerManager.seekRelative(10_000) },
                            isPrimary = false,
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Forward 10s")
                        }
                        Spacer(Modifier.width(12.dp))

                        TvButton(
                            onClick = { playerManager.playNext() },
                            enabled = uiState.hasNext,
                            isPrimary = false,
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next")
                        }
                    }
                }
            }
        }

        // Resume Playback Modal
        if (showResumeDialog && initialBookmark != null) {
            Dialog(onDismissRequest = { showResumeDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TvSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TvCyanPrimary),
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Resume Playback?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "You were watching '${initialBookmark.title}' at ${initialBookmark.formattedPosition}.",
                            color = TvTextSecondary,
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TvButton(
                                onClick = {
                                    showResumeDialog = false
                                    playerManager.seekTo(0L)
                                },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Start Over")
                            }
                            TvButton(
                                onClick = {
                                    showResumeDialog = false
                                    playerManager.seekTo(initialBookmark.positionMs)
                                },
                                isPrimary = true,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resume")
                            }
                        }
                    }
                }
            }
        }

        // Audio Track Selection Dialog
        if (showAudioDialog) {
            Dialog(onDismissRequest = { showAudioDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TvSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TvCyanPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.widthIn(max = 440.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Audio Track",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            TvButton(
                                onClick = { showAudioDialog = false },
                                isPrimary = false,
                                contentPadding = PaddingValues(6.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        if (uiState.audioTracks.isEmpty()) {
                            Text("No additional audio tracks found.", color = TvTextSecondary, fontSize = 13.sp)
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(200.dp)
                            ) {
                                items(uiState.audioTracks) { track ->
                                    TvFocusableCard(
                                        onClick = {
                                            playerManager.selectAudioTrack(track)
                                            showAudioDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) { isFocused ->
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${track.name} [${track.language}] - ${track.formatLabel}",
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            if (track.isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TvCyanPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subtitle Track Selection Dialog
        if (showSubtitleDialog) {
            Dialog(onDismissRequest = { showSubtitleDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TvSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TvCyanPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.widthIn(max = 440.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subtitles",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            TvButton(
                                onClick = { showSubtitleDialog = false },
                                isPrimary = false,
                                contentPadding = PaddingValues(6.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Disable option
                        TvFocusableCard(
                            onClick = {
                                playerManager.selectSubtitleTrack(null)
                                showSubtitleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Off (Disable Subtitles)", color = Color.White, fontSize = 13.sp)
                                if (uiState.selectedSubtitleTrackIndex == -1) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TvCyanPrimary)
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(180.dp)
                        ) {
                            items(uiState.subtitleTracks) { track ->
                                TvFocusableCard(
                                    onClick = {
                                        playerManager.selectSubtitleTrack(track)
                                        showSubtitleDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${track.name} (${track.language})", color = Color.White, fontSize = 13.sp)
                                        if (track.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = TvCyanPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
