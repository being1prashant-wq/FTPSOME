package com.example.data.ftp

import android.util.Log
import com.example.data.model.FtpFileItem
import com.example.data.model.FtpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException

sealed class FtpResult<out T> {
    data class Success<out T>(val data: T) : FtpResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : FtpResult<Nothing>()
}

class FtpClientService {

    companion object {
        private const val TAG = "FtpClientService"
        const val DEFAULT_TIMEOUT_MS = 15_000
    }

    private var activeClient: FTPClient? = null
    var currentServer: FtpServer? = null
        private set

    var currentDirectory: String = "/"
        private set

    suspend fun connect(server: FtpServer): FtpResult<Boolean> = withContext(Dispatchers.IO) {
        disconnect()
        val client = createConfiguredClient()
        try {
            Log.d(TAG, "Connecting to ${server.host}:${server.port}")
            client.connect(server.host, server.port)
            val reply = client.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect()
                return@withContext FtpResult.Error("FTP server refused connection (Code: $reply).")
            }

            val username = if (server.isAnonymous || server.username.isBlank()) "anonymous" else server.username
            val password = if (server.isAnonymous) "" else server.password

            val loginSuccess = client.login(username, password)
            if (!loginSuccess) {
                client.disconnect()
                return@withContext FtpResult.Error("Authentication failed for user '$username'. Check username and password.")
            }

            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            client.bufferSize = 64 * 1024

            activeClient = client
            currentServer = server
            currentDirectory = "/"

            Log.i(TAG, "Connected successfully to ${server.displayAddress}")
            FtpResult.Success(true)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to connect to ${server.host}:${server.port}", e)
            try { client.disconnect() } catch (_: Exception) {}
            FtpResult.Error(
                "Unable to connect to ${server.host}:${server.port}. Ensure phone FTP server is ON and on the same Wi-Fi network.",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error connecting to FTP", e)
            try { client.disconnect() } catch (_: Exception) {}
            FtpResult.Error("Connection error: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    suspend fun listFiles(path: String = currentDirectory): FtpResult<List<FtpFileItem>> = withContext(Dispatchers.IO) {
        val server = currentServer ?: return@withContext FtpResult.Error("Not connected to FTP server.")
        
        // Ensure connection is still alive, otherwise reconnect
        if (!isConnectionAlive()) {
            val reconnectResult = connect(server)
            if (reconnectResult is FtpResult.Error) {
                return@withContext reconnectResult
            }
        }

        val client = activeClient ?: return@withContext FtpResult.Error("FTP client not available.")

        try {
            val targetPath = normalizePath(path)
            val files: Array<FTPFile> = client.listFiles(targetPath) ?: emptyArray()

            val items = files.mapNotNull { ftpFile ->
                val name = ftpFile.name
                if (name == "." || name == "..") return@mapNotNull null

                val isDir = ftpFile.isDirectory
                val size = if (isDir) 0L else ftpFile.size
                val timestamp = ftpFile.timestamp?.timeInMillis ?: 0L
                val fullPath = if (targetPath.endsWith("/")) "$targetPath$name" else "$targetPath/$name"

                FtpFileItem(
                    name = name,
                    fullPath = fullPath,
                    isDirectory = isDir,
                    sizeBytes = size,
                    lastModifiedTime = timestamp,
                    category = FtpFileItem.determineCategory(name, isDir)
                )
            }.sortedWith(
                compareByDescending<FtpFileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )

            currentDirectory = targetPath
            FtpResult.Success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files in $path", e)
            FtpResult.Error("Failed to list folder contents: ${e.localizedMessage}", e)
        }
    }

    fun isConnectionAlive(): Boolean {
        val client = activeClient ?: return false
        return try {
            client.isConnected && client.sendNoOp()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            activeClient?.let {
                if (it.isConnected) {
                    try { it.logout() } catch (_: Exception) {}
                    try { it.disconnect() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting FTP client", e)
        } finally {
            activeClient = null
        }
    }

    private fun createConfiguredClient(): FTPClient {
        return FTPClient().apply {
            defaultTimeout = DEFAULT_TIMEOUT_MS
            connectTimeout = DEFAULT_TIMEOUT_MS
            setDataTimeout(java.time.Duration.ofMillis(20_000))
            controlEncoding = "UTF-8"
            setAutodetectUTF8(true)
        }
    }

    fun normalizePath(path: String): String {
        var p = path.trim()
        if (p.isEmpty() || !p.startsWith("/")) {
            p = "/$p"
        }
        return p
    }

    fun getParentPath(path: String): String {
        val normalized = normalizePath(path)
        if (normalized == "/" || normalized.isEmpty()) return "/"
        val lastSlash = normalized.dropLast(1).lastIndexOf('/')
        return if (lastSlash <= 0) "/" else normalized.substring(0, lastSlash)
    }
}
