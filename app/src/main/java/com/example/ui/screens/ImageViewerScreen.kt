package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FtpFileItem
import com.example.data.model.FtpServer
import com.example.ui.components.TvButton
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayOutputStream

@Composable
fun ImageViewerScreen(
    server: FtpServer,
    images: List<FtpFileItem>,
    initialIndex: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, images.size - 1)) }
    var currentBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val currentItem = images.getOrNull(currentIndex)

    BackHandler {
        onExit()
    }

    LaunchedEffect(currentIndex, server) {
        val item = images.getOrNull(currentIndex) ?: return@LaunchedEffect
        isLoading = true
        errorMsg = null
        currentBitmap = null

        withContext(Dispatchers.IO) {
            val client = FTPClient().apply {
                defaultTimeout = 15000
                connectTimeout = 15000
                setDataTimeout(java.time.Duration.ofMillis(15000))
            }
            try {
                client.connect(server.host, server.port)
                val user = if (server.isAnonymous || server.username.isBlank()) "anonymous" else server.username
                val pass = if (server.isAnonymous) "" else server.password
                client.login(user, pass)
                client.enterLocalPassiveMode()
                client.setFileType(FTP.BINARY_FILE_TYPE)

                val outStream = ByteArrayOutputStream()
                val success = client.retrieveFile(item.fullPath, outStream)
                if (success) {
                    val bytes = outStream.toByteArray()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    withContext(Dispatchers.Main) {
                        currentBitmap = bitmap
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMsg = "Failed to load image from phone FTP"
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMsg = "Error: ${e.localizedMessage}"
                    isLoading = false
                }
            } finally {
                try { client.disconnect() } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (currentIndex > 0) currentIndex--
                            true
                        }
                        Key.DirectionRight -> {
                            if (currentIndex < images.size - 1) currentIndex++
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Image Display
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap!!.asImageBitmap(),
                contentDescription = currentItem?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = TvCyanPrimary)
                Text("Loading picture from phone...", color = Color.White, fontSize = 13.sp)
            }
        }

        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 15.sp
            )
        }

        // Top Overlay Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvButton(
                    onClick = onExit,
                    isPrimary = false,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back", fontSize = 13.sp)
                }

                Text(
                    text = currentItem?.name ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Text(
                text = "${currentIndex + 1} / ${images.size}",
                color = TvCyanPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Bottom Navigation Pills
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TvButton(
                onClick = { if (currentIndex > 0) currentIndex-- },
                enabled = currentIndex > 0,
                isPrimary = false
            ) {
                Icon(Icons.Default.NavigateBefore, contentDescription = "Previous")
                Spacer(Modifier.width(4.dp))
                Text("Previous")
            }

            TvButton(
                onClick = { if (currentIndex < images.size - 1) currentIndex++ },
                enabled = currentIndex < images.size - 1,
                isPrimary = false
            ) {
                Text("Next")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.NavigateNext, contentDescription = "Next")
            }
        }
    }
}
