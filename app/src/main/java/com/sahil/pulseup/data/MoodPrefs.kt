package com.sahil.pulseup.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.HashMap
import java.util.Map
import java.util.Iterator

object MoodPrefs {
    private const val PREFS_NAME = "mood_prefs"

    fun saveMoods(context: Context, month: Int, year: Int, moods: kotlin.collections.Map<Int, String>) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject()
            for ((key, value) in moods) {
                json.put(key.toString(), value)
            }
            val jsonString = json.toString()
            // Use commit to ensure synchronous write for reliability in this simple app
            prefs.edit().putString("moods_$month" + "_" + year, jsonString).commit()

            // Also save each day's mood individually (per-day keys) for robustness
            val editor = prefs.edit()
            for ((key, value) in moods) {
                editor.putString("mood_$year" + "_" + month + "_" + key, value)
            }
            editor.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Debug helper: return the raw JSON string stored for a month/year (or null)
    fun getRawJson(context: Context, month: Int, year: Int): String? {
        return try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString("moods_$month" + "_" + year, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadMoods(context: Context, month: Int, year: Int): kotlin.collections.Map<Int, String> {
        val map: MutableMap<Int, String> = HashMap()
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString("moods_$month" + "_" + year, null)
            if (!json.isNullOrEmpty()) {
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
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
                val key = "mood_$year" + "_" + month + "_" + d
                val value = prefs.getString(key, null)
                if (!value.isNullOrEmpty()) {
                    map[d] = value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun setMood(context: Context, year: Int, month: Int, day: Int, emoji: String?) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = loadMoods(context, month, year).toMutableMap()
        if (emoji.isNullOrEmpty()) {
            existing.remove(day)
        } else {
            existing[day] = emoji
        }
        saveMoods(context, month, year, existing)
    }

    fun getMood(context: Context, year: Int, month: Int, day: Int): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Prefer per-day key if available
        val perDayValue = prefs.getString("mood_$year" + "_" + month + "_" + day, null)
        if (!perDayValue.isNullOrEmpty()) {
            return perDayValue
        }
        // Fall back to monthly map
        val map = loadMoods(context, month, year)
        return map[day]
    }
}
