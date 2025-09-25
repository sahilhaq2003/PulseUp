package com.sahil.pulseup

import android.content.Context
import androidx.work.Constraints
import androidx.work.*
import java.util.Locale
import java.util.concurrent.TimeUnit

object HydrationPrefs {
    private const val PREFS_NAME = "hydration_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL_HOURS = "interval_hours"
    private const val KEY_START_HOUR = "start_hour"
    private const val KEY_END_HOUR = "end_hour"
    private const val KEY_LAST_SCHEDULED_AT = "last_scheduled_at"
    
    private const val WORK_NAME = "hydration_reminder_work"
    
    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }
    
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
            
        if (enabled) {
            scheduleReminders(context)
        } else {
            cancelReminders(context)
        }
    }
    
    fun getIntervalHours(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL_HOURS, 2)
    }
    
    fun setIntervalHours(context: Context, hours: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL_HOURS, hours.coerceAtLeast(1))
            .apply()
        if (isEnabled(context)) scheduleReminders(context)
    }
    
    fun getStartHour(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_START_HOUR, 8)
    }
    
    fun setStartHour(context: Context, hour: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_START_HOUR, hour.coerceIn(0, 23))
            .apply()
        if (isEnabled(context)) scheduleReminders(context)
    }
    
    fun getEndHour(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_END_HOUR, 22)
    }
    
    fun setEndHour(context: Context, hour: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_END_HOUR, hour.coerceIn(0, 23))
            .apply()
        if (isEnabled(context)) scheduleReminders(context)
    }
    
    private fun scheduleReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        // Cancel existing work
        workManager.cancelUniqueWork(WORK_NAME)
        
        val intervalHours = getIntervalHours(context)
        val startHour = getStartHour(context)
        val endHour = getEndHour(context)
        
        // Create periodic work request
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
            
        val hydrationWork = PeriodicWorkRequestBuilder<HydrationReminderWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(startHour), TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            hydrationWork
        )

        // Store last scheduled time
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SCHEDULED_AT, System.currentTimeMillis())
            .apply()
    }
    
    private fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
    
    private fun calculateInitialDelay(startHour: Int): Long {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val delayHours = if (currentHour < startHour) {
            startHour - currentHour
        } else {
            24 - currentHour + startHour
        }
        return delayHours * 60L // Convert to minutes
    }

    fun getNextReminderText(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return "Reminders off"

        val interval = getIntervalHours(context)
        val startHour = getStartHour(context)
        val endHour = getEndHour(context)

        val now = java.util.Calendar.getInstance()
        var next = java.util.Calendar.getInstance()
        next.add(java.util.Calendar.HOUR_OF_DAY, interval)

        // If current time is before start hour, use today start
        if (now.get(java.util.Calendar.HOUR_OF_DAY) < startHour) {
            next = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, startHour)
                set(java.util.Calendar.MINUTE, 0)
            }
        }

        // If next is after end hour today, set to next day start
        if (next.get(java.util.Calendar.HOUR_OF_DAY) > endHour) {
            next = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, startHour)
                set(java.util.Calendar.MINUTE, 0)
            }
        }

        val hour = next.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = next.get(java.util.Calendar.MINUTE)
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = ((hour + 11) % 12) + 1
        val displayMinute = String.format(Locale.getDefault(), "%02d", minute)
        return "Next reminder: $displayHour:$displayMinute $ampm"
    }
}
