package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.ftp.FtpClientService
import com.example.data.repository.FtpRepository

class FtpTvApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var ftpRepository: FtpRepository
        private set

    lateinit var ftpService: FtpClientService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        ftpRepository = FtpRepository(database.ftpServerDao(), database.playbackBookmarkDao())
        ftpService = FtpClientService()
    }

    companion object {
        lateinit var instance: FtpTvApplication
            private set
    }
}
