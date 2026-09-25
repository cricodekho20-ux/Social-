package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.repository.DownloadRepository
import com.example.data.repository.VideoInspectorRepository

class SocialVideoSaverApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    lateinit var videoInspectorRepository: VideoInspectorRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        videoInspectorRepository = VideoInspectorRepository()
        downloadRepository = DownloadRepository(this, database.downloadDao())

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Video Downloads"
            val descriptionText = "Notifications for active video downloads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "social_video_downloads"
        lateinit var instance: SocialVideoSaverApp
            private set
    }
}
