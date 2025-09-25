package com.sahil.pulseup

import android.content.Context
import org.json.JSONObject

object MoodPrefs {
    private const val PREFS_NAME = "mood_prefs"

    fun saveMoods(context: Context, month: Int, year: Int, moods: Map<Int, String>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject(moods as Map<*, *>).toString()
            // Use commit to ensure synchronous write for reliability in this simple app
            prefs.edit().putString("moods_${month}_$year", json).commit()

            // Also save each day's mood individually (per-day keys) for robustness
            val editor = prefs.edit()
            for ((day, emoji) in moods) {
                editor.putString("mood_${year}_${month}_$day", emoji)
            }
            editor.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Debug helper: return the raw JSON string stored for a month/year (or null)
    fun getRawJson(context: Context, month: Int, year: Int): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString("moods_${month}_$year", null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadMoods(context: Context, month: Int, year: Int): MutableMap<Int, String> {
        val map = mutableMapOf<Int, String>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString("moods_${month}_$year", null)
            if (!json.isNullOrEmpty()) {
                val obj = JSONObject(json)
                obj.keys().forEach { key ->
                    try {
                        map[key.toInt()] = obj.getString(key)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }

            // Merge per-day keys if present (newer/more robust storage)
            // Check days 1..31 for any individual day keys and overwrite
            for (d in 1..31) {
                val key = "mood_${year}_${month}_$d"
                val v = prefs.getString(key, null)
                if (!v.isNullOrEmpty()) {
                    map[d] = v
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun setMood(context: Context, year: Int, month: Int, day: Int, emoji: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = loadMoods(context, month, year)
        if (emoji.isNullOrEmpty()) {
            existing.remove(day)
        } else {
            existing[day] = emoji
        }
        saveMoods(context, month, year, existing)
    }

    fun getMood(context: Context, year: Int, month: Int, day: Int): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Prefer per-day key if available
        prefs.getString("mood_${year}_${month}_$day", null)?.let { return it }
        // Fall back to monthly map
        val map = loadMoods(context, month, year)
        return map[day]
    }
}
