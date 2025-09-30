package com.sahil.pulseup.utils

object Constants {
    
    // SharedPreferences Keys
    object PrefsKeys {
        const val USER_PREFS = "user_prefs"
        const val HABIT_PREFS = "habit_prefs"
        const val MOOD_PREFS = "mood_prefs"
        const val HYDRATION_PREFS = "hydration_prefs"
        
        // User preferences
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
        const val KEY_LOGGED_IN = "logged_in"
        const val KEY_PHONE = "phone"
        const val KEY_PHOTO_URI = "photo_uri"
        
        // Habit preferences
        const val KEY_HABITS = "habits"
        
        // Hydration preferences
        const val KEY_ENABLED = "enabled"
        const val KEY_INTERVAL_HOURS = "interval_hours"
        const val KEY_START_HOUR = "start_hour"
        const val KEY_END_HOUR = "end_hour"
        const val KEY_LAST_SCHEDULED_AT = "last_scheduled_at"
        const val KEY_NEXT_HOUR = "next_hour"
        const val KEY_NEXT_MINUTE = "next_minute"
    }
    
    // Work Manager
    object WorkManager {
        const val HYDRATION_WORK_NAME = "hydration_reminder_work"
        const val HYDRATION_NOTIFICATION_ID = 1001
        const val HYDRATION_CHANNEL_ID = "hydration_reminders"
    }
    
    // Request Codes
    object RequestCodes {
        const val NOTIFICATION_PERMISSION = 1001
        const val PICK_IMAGE = 1002
    }
    
    // Default Values
    object Defaults {
        const val DEFAULT_HYDRATION_INTERVAL = 2
        const val DEFAULT_START_HOUR = 8
        const val DEFAULT_END_HOUR = 22
        const val DEFAULT_HABIT_TARGET = 1
    }
    
    // UI Constants
    object UI {
        const val CHART_PADDING = 40f
        const val POINT_RADIUS = 8f
        const val LINE_WIDTH = 4f
        const val MAX_HABITS_PREVIEW = 2
    }
}
