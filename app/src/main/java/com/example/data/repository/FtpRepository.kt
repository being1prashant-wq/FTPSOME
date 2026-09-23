package com.example.data.repository

import com.example.data.db.FtpServerDao
import com.example.data.db.PlaybackBookmarkDao
import com.example.data.model.FtpServer
import com.example.data.model.PlaybackBookmark
import kotlinx.coroutines.flow.Flow

class FtpRepository(
    private val ftpServerDao: FtpServerDao,
    private val playbackBookmarkDao: PlaybackBookmarkDao
) {
    val allServers: Flow<List<FtpServer>> = ftpServerDao.getAllServers()
    val recentBookmarks: Flow<List<PlaybackBookmark>> = playbackBookmarkDao.getRecentBookmarks()

    suspend fun saveServer(server: FtpServer): Long {
        return ftpServerDao.insertServer(server)
    }

    suspend fun deleteServer(server: FtpServer) {
        ftpServerDao.deleteServer(server)
    }

    suspend fun deleteServerById(id: Long) {
        ftpServerDao.deleteServerById(id)
    }

    suspend fun updateServer(server: FtpServer) {
        ftpServerDao.updateServer(server)
    }

    suspend fun getBookmark(fullPath: String): PlaybackBookmark? {
        return playbackBookmarkDao.getBookmark(fullPath)
    }

    suspend fun saveBookmark(bookmark: PlaybackBookmark) {
        playbackBookmarkDao.saveBookmark(bookmark)
    }

    suspend fun deleteBookmark(fullPath: String) {
        playbackBookmarkDao.deleteBookmark(fullPath)
    }

    suspend fun clearHistory() {
        playbackBookmarkDao.clearAll()
    }
}
