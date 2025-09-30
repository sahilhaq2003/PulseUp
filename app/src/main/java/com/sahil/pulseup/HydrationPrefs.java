package com.sahil.pulseup;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HydrationPrefs {
    private static final String PREFS_NAME = "hydration_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_INTERVAL_HOURS = "interval_hours";
    private static final String KEY_START_HOUR = "start_hour";
    private static final String KEY_END_HOUR = "end_hour";
    private static final String KEY_LAST_SCHEDULED_AT = "last_scheduled_at";
    private static final String KEY_NEXT_HOUR = "next_hour";
    private static final String KEY_NEXT_MINUTE = "next_minute";
    
    private static final String WORK_NAME = "hydration_reminder_work";
    
    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false);
    }
    
    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply();
            
        if (enabled) {
            scheduleReminders(context);
        } else {
            cancelReminders(context);
        }
    }
    
    public static int getIntervalHours(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL_HOURS, 2);
    }
    
    public static void setIntervalHours(Context context, int hours) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL_HOURS, Math.max(hours, 1))
            .apply();
        if (isEnabled(context)) scheduleReminders(context);
    }
    
    public static int getStartHour(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_START_HOUR, 8);
    }
    
    public static void setStartHour(Context context, int hour) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_START_HOUR, Math.max(0, Math.min(23, hour)))
            .apply();
        if (isEnabled(context)) scheduleReminders(context);
    }
    
    public static int getEndHour(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_END_HOUR, 22);
    }
    
    public static void setEndHour(Context context, int hour) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_END_HOUR, Math.max(0, Math.min(23, hour)))
            .apply();
        if (isEnabled(context)) scheduleReminders(context);
    }
    
    private static void scheduleReminders(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        
        workManager.cancelUniqueWork(WORK_NAME);
        
        int intervalHours = getIntervalHours(context);
        int startHour = getStartHour(context);
        int endHour = getEndHour(context);
        
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build();
            
        PeriodicWorkRequest hydrationWork = new PeriodicWorkRequest.Builder(
            HydrationReminderWorker.class,
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(startHour), TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build();
            
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            hydrationWork
        );

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SCHEDULED_AT, System.currentTimeMillis())
            .apply();
    }
    
    private static void cancelReminders(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
    
    private static long calculateInitialDelay(int startHour) {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int delayHours = (currentHour < startHour) ? 
            (startHour - currentHour) : 
            (24 - currentHour + startHour);
        return delayHours * 60L;
    }

    public static String getNextReminderText(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, false)) return "Reminders off";

        int explicitHour = prefs.getInt(KEY_NEXT_HOUR, -1);
        int explicitMinute = prefs.getInt(KEY_NEXT_MINUTE, -1);
        if (explicitHour >= 0 && explicitHour <= 23 && explicitMinute >= 0 && explicitMinute <= 59) {
            int hour = explicitHour;
            int minute = explicitMinute;
            String ampm = (hour >= 12) ? "PM" : "AM";
            int displayHour = ((hour + 11) % 12) + 1;
            String displayMinute = String.format(Locale.getDefault(), "%02d", minute);
            return "Next reminder: " + displayHour + ":" + displayMinute + " " + ampm;
        }

        int interval = getIntervalHours(context);
        int startHour = getStartHour(context);
        int endHour = getEndHour(context);

        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.add(Calendar.HOUR_OF_DAY, interval);

        if (now.get(Calendar.HOUR_OF_DAY) < startHour) {
            next = Calendar.getInstance();
            next.set(Calendar.HOUR_OF_DAY, startHour);
            next.set(Calendar.MINUTE, 0);
        }

        if (next.get(Calendar.HOUR_OF_DAY) > endHour) {
            next = Calendar.getInstance();
            next.add(Calendar.DAY_OF_YEAR, 1);
            next.set(Calendar.HOUR_OF_DAY, startHour);
            next.set(Calendar.MINUTE, 0);
        }

        int hour = next.get(Calendar.HOUR_OF_DAY);
        int minute = next.get(Calendar.MINUTE);
        String ampm = (hour >= 12) ? "PM" : "AM";
        int displayHour = ((hour + 11) % 12) + 1;
        String displayMinute = String.format(Locale.getDefault(), "%02d", minute);
        return "Next reminder: " + displayHour + ":" + displayMinute + " " + ampm;
    }

    public static void setNextReminderTime(Context context, int hour, int minute) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NEXT_HOUR, Math.max(0, Math.min(23, hour)))
            .putInt(KEY_NEXT_MINUTE, Math.max(0, Math.min(59, minute)))
            .apply();

        if (isEnabled(context)) {
            scheduleReminders(context);
        }
    }
}
