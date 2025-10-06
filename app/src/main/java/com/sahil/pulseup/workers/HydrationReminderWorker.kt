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

    fun showHydrationNotification() {
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
                enableVibration(true)
                setShowBadge(true)
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

        // Create action intents
        val snoozeIntent = Intent(applicationContext, HydrationReminderWorker::class.java).apply {
            action = "SNOOZE_ACTION"
        }
        val snoozePendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(applicationContext, HydrationReminderWorker::class.java).apply {
            action = "DISMISS_ACTION"
        }
        val dismissPendingIntent = PendingIntent.getService(
            applicationContext,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with enhanced content and actions
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("💧 Hydration Reminder")
            .setContentText("Time to drink water! Stay hydrated for better health.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your body needs water to function properly. Take a moment to hydrate yourself. Even mild dehydration can affect your mood and energy levels."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(R.drawable.ic_water_drop, "Snooze 15min", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
