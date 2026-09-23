package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ftp_servers")
data class FtpServer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 2121,
    val username: String = "",
    val password: String = "",
    val isAnonymous: Boolean = true,
    val lastConnectedAt: Long = System.currentTimeMillis()
) {
    val displayAddress: String
        get() = "ftp://$host:$port"
}
