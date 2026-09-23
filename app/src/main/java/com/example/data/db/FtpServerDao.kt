package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FtpServer
import kotlinx.coroutines.flow.Flow

@Dao
interface FtpServerDao {
    @Query("SELECT * FROM ftp_servers ORDER BY lastConnectedAt DESC")
    fun getAllServers(): Flow<List<FtpServer>>

    @Query("SELECT * FROM ftp_servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Long): FtpServer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: FtpServer): Long

    @Update
    suspend fun updateServer(server: FtpServer)

    @Delete
    suspend fun deleteServer(server: FtpServer)

    @Query("DELETE FROM ftp_servers WHERE id = :id")
    suspend fun deleteServerById(id: Long)

    @Query("DELETE FROM ftp_servers")
    suspend fun clearAll()
}
