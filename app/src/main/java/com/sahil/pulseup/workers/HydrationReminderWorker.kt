package com.sahil.pulseup.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sahil.pulseup.R
import com.sahil.pulseup.activities.MainFragmentActivity

class HydrationReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "hydration_reminders"
        private const val NOTIFICATION_ID = 1001
    }

    override fun doWork(): Result {
        return try {
            showHydrationNotification()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showHydrationNotification() {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to stay hydrated"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for when notification is tapped
        val intent = Intent(applicationContext, MainFragmentActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Stay Hydrated! 💧")
            .setContentText("Time for your water break. Your body will thank you!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
