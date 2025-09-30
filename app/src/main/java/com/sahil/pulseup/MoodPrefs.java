package com.sahil.pulseup;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class MoodPrefs {
    private static final String PREFS_NAME = "mood_prefs";

    public static void saveMoods(Context context, int month, int year, Map<Integer, String> moods) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONObject json = new JSONObject();
            for (Map.Entry<Integer, String> entry : moods.entrySet()) {
                json.put(entry.getKey().toString(), entry.getValue());
            }
            String jsonString = json.toString();
            // Use commit to ensure synchronous write for reliability in this simple app
            prefs.edit().putString("moods_" + month + "_" + year, jsonString).commit();

            // Also save each day's mood individually (per-day keys) for robustness
            SharedPreferences.Editor editor = prefs.edit();
            for (Map.Entry<Integer, String> entry : moods.entrySet()) {
                editor.putString("mood_" + year + "_" + month + "_" + entry.getKey(), entry.getValue());
            }
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Debug helper: return the raw JSON string stored for a month/year (or null)
    public static String getRawJson(Context context, int month, int year) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getString("moods_" + month + "_" + year, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<Integer, String> loadMoods(Context context, int month, int year) {
        Map<Integer, String> map = new HashMap<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString("moods_" + month + "_" + year, null);
            if (json != null && !json.isEmpty()) {
                JSONObject obj = new JSONObject(json);
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        map.put(Integer.parseInt(key), obj.getString(key));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            // Merge per-day keys if present (newer/more robust storage)
            // Check days 1..31 for any individual day keys and overwrite
            for (int d = 1; d <= 31; d++) {
                String key = "mood_" + year + "_" + month + "_" + d;
                String value = prefs.getString(key, null);
                if (value != null && !value.isEmpty()) {
                    map.put(d, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void setMood(Context context, int year, int month, int day, String emoji) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<Integer, String> existing = loadMoods(context, month, year);
        if (emoji == null || emoji.isEmpty()) {
            existing.remove(day);
        } else {
            existing.put(day, emoji);
        }
        saveMoods(context, month, year, existing);
    }

    public static String getMood(Context context, int year, int month, int day) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Prefer per-day key if available
        String perDayValue = prefs.getString("mood_" + year + "_" + month + "_" + day, null);
        if (perDayValue != null && !perDayValue.isEmpty()) {
            return perDayValue;
        }
        // Fall back to monthly map
        Map<Integer, String> map = loadMoods(context, month, year);
        return map.get(day);
    }
}
