package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.example.data.model.FtpFileItem
import com.example.data.model.FtpServer
import com.example.data.model.PlaybackBookmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String?,
    val formatLabel: String,
    val isSelected: Boolean,
    val groupIndex: Int,
    val trackIndex: Int
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferPositionMs: Long = 0L,
    val currentTitle: String = "",
    val errorMessage: String? = null,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val selectedAudioTrackIndex: Int = 0,
    val selectedSubtitleTrackIndex: Int = -1, // -1 means disabled
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

@androidx.annotation.OptIn(UnstableApi::class)
class FtpPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "FtpPlayerManager"
    }

    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private var currentServer: FtpServer? = null
    private var playlist: List<FtpFileItem> = emptyList()
    private var currentPlaylistIndex: Int = -1

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var onBookmarkUpdate: ((PlaybackBookmark) -> Unit)? = null

    fun initializePlayer(server: FtpServer, bufferSeconds: Int = 30) {
        if (exoPlayer != null && currentServer == server) {
            return
        }
        release()
        currentServer = server

        // 1. Low-RAM Bounded Buffer Control (Zero disk usage, rolling memory buffer)
        val minBufferMs = (bufferSeconds * 1000).coerceIn(10_000, 60_000)
        val maxBufferMs = (minBufferMs * 2).coerceAtMost(90_000)
        val bufferForPlaybackMs = 2_000
        val bufferForRebufferMs = 4_000

        val allocator = DefaultAllocator(/* trimOnReset = */ true, /* individualAllocationSize = */ 32 * 1024)
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForRebufferMs)
            .setTargetBufferBytes(24 * 1024 * 1024) // Max 24MB RAM usage
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 2. AudioSink & AC3/E-AC3 Software Fallback Pipeline
        val audioCapabilities = AudioCapabilities.getCapabilities(context)
        val audioSink = DefaultAudioSink.Builder(context)
            .setAudioCapabilities(audioCapabilities)
            .setEnableFloatOutput(false) // Safe 16-bit PCM for low-end Android TV DACs
            .setAudioProcessors(arrayOf<AudioProcessor>())
            .build()

        // 3. RenderersFactory with software fallback and audio downmixing
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: androidx.media3.exoplayer.audio.AudioSink,
                eventHandler: android.os.Handler,
                eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
                out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
            ) {
                // Primary hardware & software codec renderer with fallback enabled
                out.add(
                    MediaCodecAudioRenderer(
                        context,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        audioSink
                    )
                )
                super.buildAudioRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    audioSink,
                    eventHandler,
                    eventListener,
                    out
                )
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }

        val dataSourceFactory = FtpDataSourceFactory(server)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                addListener(createPlayerListener())
            }

        startProgressTracking()
    }

    fun playMediaList(
        server: FtpServer,
        files: List<FtpFileItem>,
        startIndex: Int,
        startPositionMs: Long = 0L,
        externalSubtitles: List<FtpFileItem> = emptyList()
    ) {
        initializePlayer(server)
        val player = exoPlayer ?: return

        playlist = files
        currentPlaylistIndex = startIndex.coerceIn(0, files.size - 1)

        val targetFile = files[currentPlaylistIndex]
        val uri = buildFtpUri(server, targetFile.fullPath)

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(targetFile.fullPath)

        // Attach external subtitles if matching
        if (externalSubtitles.isNotEmpty()) {
            val subtitleConfigs = externalSubtitles.map { subFile ->
                val subUri = buildFtpUri(server, subFile.fullPath)
                val mimeType = when (subFile.extension) {
                    "vtt" -> MimeTypes.TEXT_VTT
                    "ass", "ssa" -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(mimeType)
                    .setLanguage("und")
                    .setLabel(subFile.name)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()
        player.setMediaItem(mediaItem)

        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }

        player.prepare()
        player.play()

        _uiState.value = _uiState.value.copy(
            currentTitle = targetFile.name,
            hasNext = currentPlaylistIndex < playlist.size - 1,
            hasPrevious = currentPlaylistIndex > 0,
            errorMessage = null
        )
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
        saveCurrentBookmark()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun seekRelative(deltaMs: Long) {
        val player = exoPlayer ?: return
        val current = player.currentPosition
        val duration = player.duration.coerceAtLeast(0L)
        val target = if (duration > 0) {
            (current + deltaMs).coerceIn(0L, duration)
        } else {
            (current + deltaMs).coerceAtLeast(0L)
        }
        player.seekTo(target)
    }

    fun playNext() {
        if (currentPlaylistIndex < playlist.size - 1) {
            currentServer?.let { server ->
                playMediaList(server, playlist, currentPlaylistIndex + 1)
            }
        }
    }

    fun playPrevious() {
        if (currentPlaylistIndex > 0) {
            currentServer?.let { server ->
                playMediaList(server, playlist, currentPlaylistIndex - 1)
            }
        }
    }

    fun selectAudioTrack(track: TrackInfo) {
        val player = exoPlayer ?: return
        val tracks = player.currentTracks
        val group = tracks.groups.getOrNull(track.groupIndex) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(track.trackIndex))
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(override)
            .build()
        updateTracks(tracks)
    }

    fun selectSubtitleTrack(track: TrackInfo?) {
        val player = exoPlayer ?: return
        if (track == null) {
            // Disable subtitles
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val tracks = player.currentTracks
            val group = tracks.groups.getOrNull(track.groupIndex) ?: return
            val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(track.trackIndex))
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
        }
        updateTracks(player.currentTracks)
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                _uiState.value = _uiState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                )
                if (playbackState == Player.STATE_ENDED) {
                    saveCurrentBookmark(completed = true)
                    // Auto-play next if available
                    playNext()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName}", error)
                val userMsg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED ->
                        "Audio/Video decoder failed on TV. Attempting audio fallback..."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Connection to phone FTP server interrupted. Check Wi-Fi."
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "File not found on remote FTP server."
                    else -> "Playback issue: ${error.localizedMessage ?: error.errorCodeName}"
                }

                _uiState.value = _uiState.value.copy(
                    errorMessage = userMsg,
                    isBuffering = false
                )

                // If AC3 or decoder issue, try to switch audio track automatically if available
                if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED) {
                    attemptAudioDecoderFallback()
                }
            }
        }
    }

    private fun attemptAudioDecoderFallback() {
        val player = exoPlayer ?: return
        val currentAudioTracks = _uiState.value.audioTracks
        // Find alternative audio track (e.g. AAC or MP3)
        val fallbackTrack = currentAudioTracks.firstOrNull {
            !it.isSelected && (it.formatLabel.contains("aac", ignoreCase = true) || it.formatLabel.contains("mp3", ignoreCase = true))
        }
        if (fallbackTrack != null) {
            selectAudioTrack(fallbackTrack)
            player.prepare()
            player.play()
            _uiState.value = _uiState.value.copy(errorMessage = "AC3 decoder failed; switched to AAC audio track.")
        }
    }

    private fun updateTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()

        for (gIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[gIndex]
            val trackType = group.type

            for (tIndex in 0 until group.length) {
                val format = group.getTrackFormat(tIndex)
                val isSelected = group.isTrackSelected(tIndex)
                val mime = format.sampleMimeType ?: "unknown"
                val lang = format.language ?: "und"
                val label = format.label ?: "Track ${tIndex + 1}"

                val info = TrackInfo(
                    id = format.id ?: "$gIndex-$tIndex",
                    name = label,
                    language = lang,
                    formatLabel = "$mime (${format.channelCount}ch)",
                    isSelected = isSelected,
                    groupIndex = gIndex,
                    trackIndex = tIndex
                )

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(info)
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subList.add(info)
                }
            }
        }

        val selectedSub = subList.indexOfFirst { it.isSelected }
        val selectedAudio = audioList.indexOfFirst { it.isSelected }.coerceAtLeast(0)

        _uiState.value = _uiState.value.copy(
            audioTracks = audioList,
            subtitleTracks = subList,
            selectedAudioTrackIndex = selectedAudio,
            selectedSubtitleTrackIndex = selectedSub
        )
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
                        val pos = player.currentPosition.coerceAtLeast(0L)
                        val dur = player.duration.coerceAtLeast(0L)
                        val buf = player.bufferedPosition.coerceAtLeast(0L)

                        _uiState.value = _uiState.value.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            bufferPositionMs = buf
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun saveCurrentBookmark(completed: Boolean = false) {
        val player = exoPlayer ?: return
        val server = currentServer ?: return
        val currentFile = playlist.getOrNull(currentPlaylistIndex) ?: return

        val pos = player.currentPosition
        val dur = player.duration
        if (dur > 30_000 && pos > 5_000) {
            val bookmark = PlaybackBookmark(
                fullPath = currentFile.fullPath,
                title = currentFile.name,
                serverHost = server.host,
                positionMs = if (completed) 0L else pos,
                durationMs = dur,
                isCompleted = completed
            )
            onBookmarkUpdate?.invoke(bookmark)
        }
    }

    private fun buildFtpUri(server: FtpServer, remotePath: String): Uri {
        val userInfo = if (!server.isAnonymous && server.username.isNotBlank()) {
            "${server.username}:${server.password}"
        } else {
            "anonymous"
        }
        val encodedUserInfo = Uri.encode(userInfo)
        val normalizedPath = if (remotePath.startsWith("/")) remotePath else "/$remotePath"
        return Uri.parse("ftp://$encodedUserInfo@${server.host}:${server.port}$normalizedPath")
    }

    fun release() {
        saveCurrentBookmark()
        progressJob?.cancel()
        progressJob = null
        exoPlayer?.release()
        exoPlayer = null
        _uiState.value = PlayerUiState()
    }
}
