package com.sahil.pulseup.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * SharedPreferences-backed storage for user habits and daily progress.
 * No database is used per requirements.
 */
object HabitPrefs {
    private const val PREFS_NAME = "habit_prefs"
    private const val KEY_HABITS = "habits" // JSON array of habit objects

    data class Habit(
        val id: String,
        val title: String,
        val targetPerDay: Int
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Region: Habit List CRUD
    fun getHabits(context: Context): List<Habit> {
        val json = prefs(context).getString(KEY_HABITS, "[]") ?: "[]"
        val arr = JSONArray(json)
        val result = mutableListOf<Habit>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                Habit(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    targetPerDay = o.optInt("targetPerDay", 1)
                )
            )
        }
        return result
    }

    fun saveHabits(context: Context, habits: List<Habit>) {
        val arr = JSONArray()
        habits.forEach { h ->
            val o = JSONObject()
            o.put("id", h.id)
            o.put("title", h.title)
            o.put("targetPerDay", h.targetPerDay)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_HABITS, arr.toString()).apply()
    }

    fun addHabit(context: Context, title: String, targetPerDay: Int): Habit {
        val newHabit = Habit(
            id = generateId(),
            title = title,
            targetPerDay = targetPerDay.coerceAtLeast(1)
        )
        val list = getHabits(context).toMutableList()
        list.add(newHabit)
        saveHabits(context, list)
        return newHabit
    }

    fun updateHabit(context: Context, habit: Habit) {
        val list = getHabits(context).map { if (it.id == habit.id) habit else it }
        saveHabits(context, list)
    }

    fun deleteHabit(context: Context, habitId: String) {
        val list = getHabits(context).filter { it.id != habitId }
        saveHabits(context, list)
        // Also clear progress for this habit to avoid stale keys
        clearAllProgressForHabit(context, habitId)
    }

    // Region: Daily Progress
    private fun progressKey(habitId: String, dateKey: String) = "progress_${habitId}_$dateKey"

    fun getTodayKey(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getProgress(context: Context, habitId: String, dateKey: String = getTodayKey()): Int {
        return prefs(context).getInt(progressKey(habitId, dateKey), 0)
    }

    fun setProgress(context: Context, habitId: String, value: Int, dateKey: String = getTodayKey()) {
        prefs(context).edit().putInt(progressKey(habitId, dateKey), value.coerceAtLeast(0)).apply()
    }

    fun incrementProgress(context: Context, habitId: String, delta: Int = 1, dateKey: String = getTodayKey()): Int {
        val current = getProgress(context, habitId, dateKey)
        val newValue = (current + delta).coerceAtLeast(0)
        setProgress(context, habitId, newValue, dateKey)
        return newValue
    }

    fun resetTodayProgress(context: Context) {
        val habits = getHabits(context)
        val today = getTodayKey()
        val editor = prefs(context).edit()
        habits.forEach { h -> editor.remove(progressKey(h.id, today)) }
        editor.apply()
    }

    fun getCompletionPercentage(context: Context, habit: Habit, dateKey: String = getTodayKey()): Int {
        val progress = getProgress(context, habit.id, dateKey)
        return if (habit.targetPerDay <= 0) 0 else ((progress.toFloat() / habit.targetPerDay) * 100f).toInt().coerceIn(0, 100)
    }

    private fun clearAllProgressForHabit(context: Context, habitId: String) {
        // Best-effort cleanup for recent days (last 60 days)
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val editor = prefs(context).edit()
        for (i in 0..60) {
            val key = progressKey(habitId, sdf.format(cal.time))
            editor.remove(key)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        editor.apply()
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}
