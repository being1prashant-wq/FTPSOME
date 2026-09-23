package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.ftp.FtpResult
import com.example.data.model.FtpFileItem
import com.example.data.model.FtpServer
import com.example.data.model.MediaCategory
import com.example.data.model.PlaybackBookmark
import com.example.player.FtpPlayerManager
import com.example.ui.components.TvNavTab
import com.example.ui.components.TvTopBar
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.ConnectDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageViewerScreen
import com.example.ui.screens.MusicPlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvBackground
import kotlinx.coroutines.launch

enum class ActiveScreenMode {
    MAIN_NAVIGATION,
    VIDEO_PLAYER,
    MUSIC_PLAYER,
    IMAGE_VIEWER
}

class MainActivity : ComponentActivity() {

    private lateinit var playerManager: FtpPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FtpTvApplication
        val repository = app.ftpRepository
        val ftpService = app.ftpService

        setContent {
            val scope = rememberCoroutineScope()

            DisposableEffect(Unit) {
                playerManager = FtpPlayerManager(this@MainActivity, scope).apply {
                    onBookmarkUpdate = { bookmark ->
                        scope.launch {
                            repository.saveBookmark(bookmark)
                        }
                    }
                }
                onDispose {
                    playerManager.release()
                }
            }

            MyApplicationTheme {
                MainTvApp(
                    repository = repository,
                    ftpService = ftpService,
                    playerManager = playerManager,
                    onKeepScreenOn = { enable ->
                        if (enable) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::playerManager.isInitialized) {
            playerManager.release()
        }
    }
}

@Composable
fun MainTvApp(
    repository: com.example.data.repository.FtpRepository,
    ftpService: com.example.data.ftp.FtpClientService,
    playerManager: FtpPlayerManager,
    onKeepScreenOn: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(TvNavTab.HOME) }
    var activeMode by remember { mutableStateOf(ActiveScreenMode.MAIN_NAVIGATION) }

    var connectedServer by remember { mutableStateOf<FtpServer?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    var currentPath by remember { mutableStateOf("/") }
    var browserFiles by remember { mutableStateOf<List<FtpFileItem>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(false) }
    var browserError by remember { mutableStateOf<String?>(null) }

    var showConnectDialog by remember { mutableStateOf(false) }
    var serverToEdit by remember { mutableStateOf<FtpServer?>(null) }

    var bufferSeconds by remember { mutableIntStateOf(30) }

    val savedServers by repository.allServers.collectAsState(initial = emptyList())
    val recentBookmarks by repository.recentBookmarks.collectAsState(initial = emptyList())

    // Active media queue states
    var videoPlaylist by remember { mutableStateOf<List<FtpFileItem>>(emptyList()) }
    var videoPlaylistIndex by remember { mutableIntStateOf(0) }
    var resumeBookmark by remember { mutableStateOf<PlaybackBookmark?>(null) }

    var audioPlaylist by remember { mutableStateOf<List<FtpFileItem>>(emptyList()) }
    var imagePlaylist by remember { mutableStateOf<List<FtpFileItem>>(emptyList()) }
    var imageIndex by remember { mutableIntStateOf(0) }

    // Screen-on management for video playback
    LaunchedEffect(activeMode) {
        onKeepScreenOn(activeMode == ActiveScreenMode.VIDEO_PLAYER)
    }

    fun loadDirectory(path: String) {
        scope.launch {
            isLoadingFiles = true
            browserError = null
            when (val result = ftpService.listFiles(path)) {
                is FtpResult.Success -> {
                    browserFiles = result.data
                    currentPath = ftpService.currentDirectory
                    isLoadingFiles = false
                }
                is FtpResult.Error -> {
                    browserError = result.message
                    isLoadingFiles = false
                }
            }
        }
    }

    fun connectToServer(server: FtpServer, autoBrowse: Boolean = true) {
        scope.launch {
            isConnecting = true
            connectionError = null
            when (val result = ftpService.connect(server)) {
                is FtpResult.Success -> {
                    connectedServer = server
                    isConnecting = false
                    repository.saveServer(server.copy(lastConnectedAt = System.currentTimeMillis()))
                    loadDirectory("/")
                    if (autoBrowse) {
                        currentTab = TvNavTab.BROWSER
                    }
                }
                is FtpResult.Error -> {
                    isConnecting = false
                    connectionError = result.message
                }
            }
        }
    }

    fun disconnectServer() {
        scope.launch {
            ftpService.disconnect()
            connectedServer = null
            browserFiles = emptyList()
            currentPath = "/"
            currentTab = TvNavTab.HOME
        }
    }

    fun launchVideoFile(item: FtpFileItem, startPositionMs: Long = 0L) {
        val server = connectedServer ?: return
        val videosInFolder = browserFiles.filter { it.category == MediaCategory.VIDEO }
        val index = videosInFolder.indexOfFirst { it.fullPath == item.fullPath }.coerceAtLeast(0)
        val subtitlesInFolder = browserFiles.filter { it.category == MediaCategory.SUBTITLE }

        videoPlaylist = if (videosInFolder.isNotEmpty()) videosInFolder else listOf(item)
        videoPlaylistIndex = index

        scope.launch {
            resumeBookmark = repository.getBookmark(item.fullPath)
            playerManager.playMediaList(
                server = server,
                files = videoPlaylist,
                startIndex = videoPlaylistIndex,
                startPositionMs = startPositionMs,
                externalSubtitles = subtitlesInFolder
            )
            activeMode = ActiveScreenMode.VIDEO_PLAYER
        }
    }

    fun launchAudioFile(item: FtpFileItem) {
        val server = connectedServer ?: return
        val audioInFolder = browserFiles.filter { it.category == MediaCategory.AUDIO }
        val index = audioInFolder.indexOfFirst { it.fullPath == item.fullPath }.coerceAtLeast(0)

        audioPlaylist = if (audioInFolder.isNotEmpty()) audioInFolder else listOf(item)

        playerManager.playMediaList(
            server = server,
            files = audioPlaylist,
            startIndex = index
        )
        activeMode = ActiveScreenMode.MUSIC_PLAYER
    }

    fun launchImageFile(item: FtpFileItem) {
        val imagesInFolder = browserFiles.filter { it.category == MediaCategory.IMAGE }
        val index = imagesInFolder.indexOfFirst { it.fullPath == item.fullPath }.coerceAtLeast(0)
        imagePlaylist = if (imagesInFolder.isNotEmpty()) imagesInFolder else listOf(item)
        imageIndex = index
        activeMode = ActiveScreenMode.IMAGE_VIEWER
    }

    // Main UI Router
    when (activeMode) {
        ActiveScreenMode.VIDEO_PLAYER -> {
            VideoPlayerScreen(
                playerManager = playerManager,
                initialBookmark = resumeBookmark,
                onExitPlayer = {
                    playerManager.pause()
                    activeMode = ActiveScreenMode.MAIN_NAVIGATION
                }
            )
        }

        ActiveScreenMode.MUSIC_PLAYER -> {
            MusicPlayerScreen(
                playerManager = playerManager,
                playlist = audioPlaylist,
                onExitPlayer = {
                    playerManager.pause()
                    activeMode = ActiveScreenMode.MAIN_NAVIGATION
                }
            )
        }

        ActiveScreenMode.IMAGE_VIEWER -> {
            connectedServer?.let { server ->
                ImageViewerScreen(
                    server = server,
                    images = imagePlaylist,
                    initialIndex = imageIndex,
                    onExit = {
                        activeMode = ActiveScreenMode.MAIN_NAVIGATION
                    }
                )
            } ?: run {
                activeMode = ActiveScreenMode.MAIN_NAVIGATION
            }
        }

        ActiveScreenMode.MAIN_NAVIGATION -> {
            BackHandler(enabled = currentTab != TvNavTab.HOME || currentPath != "/") {
                if (currentTab == TvNavTab.BROWSER && currentPath != "/") {
                    val parent = ftpService.getParentPath(currentPath)
                    loadDirectory(parent)
                } else if (currentTab != TvNavTab.HOME) {
                    currentTab = TvNavTab.HOME
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = TvBackground,
                topBar = {
                    TvTopBar(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            if (tab == TvNavTab.BROWSER && connectedServer == null) {
                                showConnectDialog = true
                            } else {
                                currentTab = tab
                            }
                        },
                        connectedServer = connectedServer
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        TvNavTab.HOME -> {
                            HomeScreen(
                                connectedServer = connectedServer,
                                isConnecting = isConnecting,
                                connectionError = connectionError,
                                savedServers = savedServers,
                                recentBookmarks = recentBookmarks,
                                onConnectServer = { server -> connectToServer(server, autoBrowse = true) },
                                onDisconnect = { disconnectServer() },
                                onOpenAddServerDialog = {
                                    serverToEdit = null
                                    showConnectDialog = true
                                },
                                onDeleteServer = { server ->
                                    scope.launch { repository.deleteServer(server) }
                                },
                                onNavigateToBrowser = {
                                    if (connectedServer != null) {
                                        currentTab = TvNavTab.BROWSER
                                    } else {
                                        showConnectDialog = true
                                    }
                                },
                                onResumeBookmark = { bookmark ->
                                    val matchingServer = savedServers.firstOrNull { it.host == bookmark.serverHost }
                                        ?: connectedServer
                                    if (matchingServer != null) {
                                        if (connectedServer == null || connectedServer?.host != matchingServer.host) {
                                            connectToServer(matchingServer, autoBrowse = false)
                                        }
                                        val item = FtpFileItem(
                                            name = bookmark.title,
                                            fullPath = bookmark.fullPath,
                                            isDirectory = false,
                                            sizeBytes = 0L,
                                            lastModifiedTime = bookmark.lastPlayedAt,
                                            category = MediaCategory.VIDEO
                                        )
                                        launchVideoFile(item, bookmark.positionMs)
                                    } else {
                                        showConnectDialog = true
                                    }
                                }
                            )
                        }

                        TvNavTab.BROWSER -> {
                            BrowserScreen(
                                currentPath = currentPath,
                                files = browserFiles,
                                isLoading = isLoadingFiles,
                                errorMessage = browserError,
                                onNavigateInto = { dir ->
                                    loadDirectory(dir.fullPath)
                                },
                                onNavigateBack = {
                                    val parent = ftpService.getParentPath(currentPath)
                                    loadDirectory(parent)
                                },
                                onFileSelected = { file ->
                                    when (file.category) {
                                        MediaCategory.VIDEO -> launchVideoFile(file)
                                        MediaCategory.AUDIO -> launchAudioFile(file)
                                        MediaCategory.IMAGE -> launchImageFile(file)
                                        else -> {}
                                    }
                                },
                                onRefresh = {
                                    loadDirectory(currentPath)
                                }
                            )
                        }

                        TvNavTab.SETTINGS -> {
                            SettingsScreen(
                                savedServers = savedServers,
                                onDeleteServer = { server ->
                                    scope.launch { repository.deleteServer(server) }
                                },
                                onClearHistory = {
                                    scope.launch { repository.clearHistory() }
                                },
                                bufferSeconds = bufferSeconds,
                                onBufferSecondsChange = { bufferSeconds = it }
                            )
                        }
                    }
                }
            }

            if (showConnectDialog) {
                ConnectDialog(
                    initialServer = serverToEdit,
                    onDismiss = { showConnectDialog = false },
                    onConnect = { newServer ->
                        showConnectDialog = false
                        connectToServer(newServer, autoBrowse = true)
                    }
                )
            }
        }
    }
}
