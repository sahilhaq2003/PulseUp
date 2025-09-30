package com.sahil.pulseup.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object HydrationPrefs {
    private const val PREFS_NAME = "hydration_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL_HOURS = "interval_hours"
    private const val KEY_START_HOUR = "start_hour"
    private const val KEY_END_HOUR = "end_hour"
    private const val KEY_LAST_SCHEDULED_AT = "last_scheduled_at"
    private const val KEY_NEXT_HOUR = "next_hour"
    private const val KEY_NEXT_MINUTE = "next_minute"
    
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
            .putInt(KEY_INTERVAL_HOURS, Math.max(hours, 1))
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
            .putInt(KEY_START_HOUR, Math.max(0, Math.min(23, hour)))
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
            .putInt(KEY_END_HOUR, Math.max(0, Math.min(23, hour)))
            .apply()
        if (isEnabled(context)) scheduleReminders(context)
    }
    
    private fun scheduleReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        workManager.cancelUniqueWork(WORK_NAME)
        
        val intervalHours = getIntervalHours(context)
        val startHour = getStartHour(context)
        val endHour = getEndHour(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
            
        val hydrationWork = PeriodicWorkRequest.Builder(
            com.sahil.pulseup.workers.HydrationReminderWorker::class.java,
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

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SCHEDULED_AT, System.currentTimeMillis())
            .apply()
    }
    
    private fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
    
    private fun calculateInitialDelay(startHour: Int): Long {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val delayHours = if (currentHour < startHour) 
            (startHour - currentHour) 
        else 
            (24 - currentHour + startHour)
        return delayHours * 60L
    }

    fun getNextReminderText(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return "Reminders off"

        val explicitHour = prefs.getInt(KEY_NEXT_HOUR, -1)
        val explicitMinute = prefs.getInt(KEY_NEXT_MINUTE, -1)
        if (explicitHour in 0..23 && explicitMinute in 0..59) {
            val hour = explicitHour
            val minute = explicitMinute
            val ampm = if (hour >= 12) "PM" else "AM"
            val displayHour = ((hour + 11) % 12) + 1
            val displayMinute = String.format(Locale.getDefault(), "%02d", minute)
            return "Next reminder: $displayHour:$displayMinute $ampm"
        }

        val interval = getIntervalHours(context)
        val startHour = getStartHour(context)
        val endHour = getEndHour(context)

        val now = Calendar.getInstance()
        var next = Calendar.getInstance()
        next.add(Calendar.HOUR_OF_DAY, interval)

        if (now.get(Calendar.HOUR_OF_DAY) < startHour) {
            next = Calendar.getInstance()
            next.set(Calendar.HOUR_OF_DAY, startHour)
            next.set(Calendar.MINUTE, 0)
        }

        if (next.get(Calendar.HOUR_OF_DAY) > endHour) {
            next = Calendar.getInstance()
            next.add(Calendar.DAY_OF_YEAR, 1)
            next.set(Calendar.HOUR_OF_DAY, startHour)
            next.set(Calendar.MINUTE, 0)
        }

        val hour = next.get(Calendar.HOUR_OF_DAY)
        val minute = next.get(Calendar.MINUTE)
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = ((hour + 11) % 12) + 1
        val displayMinute = String.format(Locale.getDefault(), "%02d", minute)
        return "Next reminder: $displayHour:$displayMinute $ampm"
    }

    fun setNextReminderTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NEXT_HOUR, Math.max(0, Math.min(23, hour)))
            .putInt(KEY_NEXT_MINUTE, Math.max(0, Math.min(59, minute)))
            .apply()

        if (isEnabled(context)) {
            scheduleReminders(context)
        }
    }
}
