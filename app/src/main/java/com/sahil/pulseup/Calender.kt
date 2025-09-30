package com.sahil.pulseup

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Calender : AppCompatActivity() {

    private var daysGrid: GridLayout? = null
    private val moodHistory = mutableMapOf<Int, String>()

    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calender)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.calendar_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Optional ActionBar (may be null if using a custom header)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mood Journal"

    daysGrid = findViewById(R.id.daysGrid)

    loadMoods()

    findViewById<ImageView?>(R.id.backBtn)?.setOnClickListener { finish() }

    val prevMonth = findViewById<ImageView?>(R.id.prevMonth)
    val nextMonth = findViewById<ImageView?>(R.id.nextMonth)
    findViewById<ImageView?>(R.id.shareBtn)?.setOnClickListener { shareMoodSummary() }

        // Bottom nav click handlers
        findViewById<LinearLayout>(R.id.navMood)?.setOnClickListener {
            // already on Mood, do nothing
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(android.content.Intent(this, Profile::class.java))
        }
        findViewById<LinearLayout>(R.id.navHabits)?.setOnClickListener {
            startActivity(android.content.Intent(this, Home::class.java))
        }

    renderCalendar()

        prevMonth?.setOnClickListener {
            currentMonth--
            if (currentMonth < 0) {
                currentMonth = 11
                currentYear--
            }
            // load moods for the new month/year and redraw
            loadMoods()
            renderCalendar()
        }

        nextMonth?.setOnClickListener {
            currentMonth++
            if (currentMonth > 11) {
                currentMonth = 0
                currentYear++
            }
            // load moods for the new month/year and redraw
            loadMoods()
            renderCalendar()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload moods in case they were changed while this activity was not in foreground
        loadMoods()
        renderCalendar()
    }

    private fun renderCalendar() {
    daysGrid?.removeAllViews()
    daysGrid?.columnCount = 7

        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.DAY_OF_MONTH, 1)

    val monthTitle = findViewById<TextView?>(R.id.monthTitle)
    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
    monthTitle?.text = "$monthName $currentYear"

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()

        // Add leading spacers so the first day aligns with weekday
        val firstDayOfWeekIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday=0
        for (i in 0 until firstDayOfWeekIndex) {
            val spacer = TextView(this).apply {
                text = ""
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            daysGrid?.addView(spacer)
        }

        for (day in 1..maxDay) {
            val dayView = TextView(this).apply {
                text = day.toString()
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 20, 0, 20)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }

                moodHistory[day]?.let { emoji ->
                    text = "$day\n$emoji"
                }

                setOnClickListener {
                    val isPastOrToday = (day <= todayDay &&
                            currentMonth == today.get(Calendar.MONTH) &&
                            currentYear == today.get(Calendar.YEAR)) ||
                            (currentYear < today.get(Calendar.YEAR)) ||
                            (currentYear == today.get(Calendar.YEAR) &&
                                    currentMonth < today.get(Calendar.MONTH))

                    if (isPastOrToday) {
                        showMoodPicker(day)
                    }
                }

                // Highlight today
                if (day == todayDay && currentMonth == today.get(Calendar.MONTH) && currentYear == today.get(Calendar.YEAR)) {
                    setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
                    setTextColor(resources.getColor(android.R.color.white, null))
                }
            }
            daysGrid?.addView(dayView)
        }
    }

    private fun showMoodPicker(day: Int) {
        val moods = arrayOf("😊", "😐", "😡")

        // Use singleChoice to make user pick and confirm
        var selectedIndex = -1
        AlertDialog.Builder(this)
            .setTitle("Select Mood for $day")
            .setSingleChoiceItems(moods, -1) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Save") { dialog, _ ->
                if (selectedIndex >= 0) {
                    try {
                        moodHistory[day] = moods[selectedIndex]
                        // Persist through a single API
                        com.sahil.pulseup.MoodPrefs.setMood(this, currentYear, currentMonth, day, moods[selectedIndex])
                        renderCalendar()
                        Toast.makeText(this, "Mood saved", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // Catch unexpected errors during save to avoid crash
                        Toast.makeText(this, "Failed to save mood", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "No mood selected", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Clear") { dialog, _ ->
                try {
                    if (moodHistory.containsKey(day)) {
                        moodHistory.remove(day)
                        com.sahil.pulseup.MoodPrefs.setMood(this, currentYear, currentMonth, day, null)
                        renderCalendar()
                        Toast.makeText(this, "Mood cleared", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) { }
                dialog.dismiss()
            }
            .show()
    }

    private fun saveMoods() {
        try {
            com.sahil.pulseup.MoodPrefs.saveMoods(this, currentMonth, currentYear, moodHistory)
            // Debug: check stored JSON and show count of saved entries
            val raw = com.sahil.pulseup.MoodPrefs.getRawJson(this, currentMonth, currentYear)
            if (!raw.isNullOrEmpty()) {
                try {
                    val obj = JSONObject(raw)
                    val count = obj.length()
                    Toast.makeText(this, "Saved $count mood(s) for $currentMonth/$currentYear", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Saved (couldn't parse stored data)", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Save completed, but nothing stored (raw=null)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to save moods", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMoods() {
        try {
            val loaded = com.sahil.pulseup.MoodPrefs.loadMoods(this, currentMonth, currentYear)
            moodHistory.clear()
            moodHistory.putAll(loaded)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to load saved moods", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mood_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.share_mood_summary -> {
                shareMoodSummary()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun shareMoodSummary() {
        val monthName = Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
        }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        
        val moods = com.sahil.pulseup.MoodPrefs.loadMoods(this, currentMonth, currentYear)
        val moodCount = moods.size
        val moodEmojis = moods.values.joinToString(" ")
        
        val summary = "My mood journal for $monthName $currentYear:\n" +
                "📅 $moodCount days tracked\n" +
                "😊 Moods: $moodEmojis\n\n" +
                "Tracked with PulseUp - Your wellness companion! 💚"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share Mood Summary"))
    }
}
