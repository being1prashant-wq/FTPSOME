package com.example.player

import androidx.media3.datasource.DataSource
import com.example.data.model.FtpServer

class FtpDataSourceFactory(
    private val defaultServer: FtpServer? = null
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return FtpDataSource(defaultServer)
    }
}
