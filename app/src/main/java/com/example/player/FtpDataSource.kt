package com.example.player

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.example.data.model.FtpServer
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

class FtpDataSource(
    private val defaultServer: FtpServer? = null
) : BaseDataSource(/* isNetwork = */ true) {

    companion object {
        private const val TAG = "FtpDataSource"
        private const val SOCKET_TIMEOUT_MS = 25_000
    }

    private var ftpClient: FTPClient? = null
    private var inputStream: InputStream? = null
    private var dataSpec: DataSpec? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        val uri = dataSpec.uri
        Log.d(TAG, "Opening FTP stream for: $uri (position: ${dataSpec.position}, length: ${dataSpec.length})")

        val host = uri.host ?: defaultServer?.host ?: throw IOException("No FTP host provided in URI: $uri")
        val port = if (uri.port != -1) uri.port else (defaultServer?.port ?: 2121)
        val userInfo = uri.userInfo
        val username = when {
            userInfo != null && userInfo.contains(":") -> userInfo.substringBefore(":")
            userInfo != null -> userInfo
            defaultServer != null && !defaultServer.isAnonymous && defaultServer.username.isNotBlank() -> defaultServer.username
            else -> "anonymous"
        }
        val password = when {
            userInfo != null && userInfo.contains(":") -> userInfo.substringAfter(":")
            defaultServer != null && !defaultServer.isAnonymous -> defaultServer.password
            else -> ""
        }
        val remotePath = uri.path ?: throw IOException("Invalid path in URI: $uri")

        val client = FTPClient().apply {
            defaultTimeout = SOCKET_TIMEOUT_MS
            connectTimeout = SOCKET_TIMEOUT_MS
            setDataTimeout(java.time.Duration.ofMillis(SOCKET_TIMEOUT_MS.toLong()))
            controlEncoding = "UTF-8"
            bufferSize = 64 * 1024
        }

        try {
            client.connect(host, port)
            val reply = client.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect()
                throw IOException("FTP server refused connection. Reply code: $reply")
            }

            if (!client.login(username, password)) {
                client.disconnect()
                throw IOException("FTP login failed for user '$username'")
            }

            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)

            // Seeking support: use FTP REST command to seek to requested byte offset
            if (dataSpec.position > 0) {
                Log.d(TAG, "Seeking stream via REST to offset: ${dataSpec.position}")
                client.setRestartOffset(dataSpec.position)
            }

            val stream = client.retrieveFileStream(remotePath)
                ?: throw IOException("Failed to retrieve FTP file stream for path: $remotePath (Code: ${client.replyCode})")

            this.ftpClient = client
            this.inputStream = stream

            // Determine content length
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                bytesRemaining = dataSpec.length
            } else {
                // Determine file length from SIZE or return UNSET
                val totalSize = getRemoteFileSize(client, remotePath)
                bytesRemaining = if (totalSize > 0) {
                    (totalSize - dataSpec.position).coerceAtLeast(0L)
                } else {
                    C.LENGTH_UNSET.toLong()
                }
            }

            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open FTP stream: ${e.message}", e)
            closeConnection(client, null)
            throw IOException("FTP streaming failed: ${e.localizedMessage}", e)
        }
    }

    private fun getRemoteFileSize(client: FTPClient, path: String): Long {
        return try {
            val files = client.listFiles(path)
            if (!files.isNullOrEmpty()) {
                files[0].size
            } else {
                C.LENGTH_UNSET.toLong()
            }
        } catch (_: Exception) {
            C.LENGTH_UNSET.toLong()
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val stream = inputStream ?: return C.RESULT_END_OF_INPUT
        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            min(bytesRemaining, length.toLong()).toInt()
        }

        val bytesRead: Int
        try {
            bytesRead = stream.read(buffer, offset, bytesToRead)
        } catch (e: IOException) {
            Log.w(TAG, "Error reading from FTP stream: ${e.message}")
            throw e
        }

        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                Log.w(TAG, "Premature end of FTP stream (expected $bytesRemaining more bytes)")
            }
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }

        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        val stream = inputStream
        val client = ftpClient
        inputStream = null
        ftpClient = null

        closeConnection(client, stream)

        if (opened) {
            opened = false
            transferEnded()
        }
        dataSpec = null
    }

    private fun closeConnection(client: FTPClient?, stream: InputStream?) {
        try {
            stream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing stream", e)
        }
        if (client != null && client.isConnected) {
            Thread {
                try {
                    client.completePendingCommand()
                } catch (_: Exception) {}
                try {
                    client.disconnect()
                } catch (_: Exception) {}
            }.start()
        }
    }
}
