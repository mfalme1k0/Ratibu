package com.ik0ha.ratibu.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ik0ha.ratibu.R

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val sessionName = inputData.getString("session_name") ?: "Session"
        val providerName = inputData.getString("provider_name") ?: "Provider"
        
        showNotification(sessionName, providerName)
        
        return Result.success()
    }

    private fun showNotification(sessionName: String, providerName: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "session_reminder"
        val timeLabel = inputData.getString("time_label") ?: "soon"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Session Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Upcoming Session")
            .setContentText("Your session '$sessionName' with $providerName starts in $timeLabel.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
