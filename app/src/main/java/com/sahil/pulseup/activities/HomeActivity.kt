package com.sahil.pulseup.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sahil.pulseup.R
import com.sahil.pulseup.data.HabitPrefs
import com.sahil.pulseup.data.UserPrefs
import com.sahil.pulseup.ui.LineChartView
import java.util.*

class HomeActivity : AppCompatActivity() {
    private lateinit var habitsContainer: LinearLayout
    private var lineChart: LineChartView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        // If no user is logged in, send to Login
        if (!UserPrefs.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        habitsContainer = findViewById(R.id.habitsContainer)
        
        // Add habit button
        findViewById<ImageView>(R.id.addHabitBtn)?.setOnClickListener {
            showAddHabitDialog()
        }

        val moodJournalCard: CardView = findViewById(R.id.moodJournalCard)
        moodJournalCard.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        // Bottom nav: open Mood (calendar) or Profile
        findViewById<LinearLayout>(R.id.navMood)?.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navHabits)?.setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        lineChart = findViewById(R.id.lineChart)
        loadHabits()
        updateMoodTrendChart()
        updateHydrationCard()

        // Hydration add button -> open CreateReminder
        findViewById<android.widget.ImageButton>(R.id.hydrationAddBtn)?.setOnClickListener {
            startActivity(Intent(this, CreateReminderActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Update preview emoji for today
        try {
            val today = Calendar.getInstance()
            val month = today.get(Calendar.MONTH)
            val year = today.get(Calendar.YEAR)
            val day = today.get(Calendar.DAY_OF_MONTH)
            val moods = com.sahil.pulseup.data.MoodPrefs.loadMoods(this, month, year)
            val preview = findViewById<TextView>(R.id.moodPreviewEmoji)
            preview?.text = moods[day] ?: getString(R.string.default_mood)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Refresh habits when returning to screen
        loadHabits()
        updateMoodTrendChart()
        updateHydrationCard()
    }

    private fun updateMoodTrendChart() {
        try {
            // Map emojis to scores 0..1 for simple charting
            fun emojiToScore(emoji: String?): Float = when (emoji) {
                getString(R.string.mood_happy) -> 0.8f
                getString(R.string.mood_neutral) -> 0.5f
                getString(R.string.mood_angry) -> 0.1f
                else -> 0.6f
            }

            val values = FloatArray(7)
            for (i in 6 downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -i)
                val month = c.get(Calendar.MONTH)
                val year = c.get(Calendar.YEAR)
                val day = c.get(Calendar.DAY_OF_MONTH)
                val mood = com.sahil.pulseup.data.MoodPrefs.getMood(this, year, month, day)
                val score = emojiToScore(mood)
                values[6 - i] = score
                android.util.Log.d("ChartUpdate", "Day $day: mood=$mood, score=$score")
            }
            android.util.Log.d("ChartUpdate", "Setting chart values: ${values.contentToString()}")
            lineChart?.setValues(values)
        } catch (e: Exception) { 
            android.util.Log.e("ChartUpdate", "Error updating chart", e)
        }
    }

    private fun updateHydrationCard() {
        try {
            val text = findViewById<TextView>(R.id.hydrationNextText)
            text?.text = com.sahil.pulseup.data.HydrationPrefs.getNextReminderText(this)
        } catch (_: Exception) { }
    }
    
    private fun loadHabits() {
        habitsContainer.removeAllViews()
        val habits = HabitPrefs.getHabits(this)
        
        if (habits.isEmpty()) {
            // Show empty state
            val emptyView = TextView(this).apply {
                text = getString(R.string.no_habits_yet)
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            habitsContainer.addView(emptyView)
            return
        }
        
        // Show only first 2 habits
        val habitsToShow = habits.take(2)
        habitsToShow.forEach { habit ->
            val habitView = createHabitView(habit)
            habitsContainer.addView(habitView)
        }
        
        // Add "See More" button if there are more than 2 habits
        if (habits.size > 2) {
            val seeMoreButton = Button(this).apply {
                text = getString(R.string.see_more_habits, habits.size - 2)
                textSize = 14f
                setTextColor(resources.getColor(R.color.colorWhite, null))
                setPadding(24, 12, 24, 12)
                // Add rounded corners
                background = resources.getDrawable(R.drawable.rounded_button_bg, null)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 12, 16, 0)
                }
            }
            
            seeMoreButton.setOnClickListener {
                startActivity(Intent(this, HabitsActivity::class.java))
            }
            
            habitsContainer.addView(seeMoreButton)
        }
    }
    
    private fun createHabitView(habit: HabitPrefs.Habit): View {
        val inflater = LayoutInflater.from(this)
        val habitView = inflater.inflate(R.layout.habit_item, habitsContainer, false)
        
        val checkbox = habitView.findViewById<CheckBox>(R.id.habitCheckbox)
        val titleText = habitView.findViewById<TextView>(R.id.habitTitle)
        val progressText = habitView.findViewById<TextView>(R.id.habitProgress)
        val progressBar = habitView.findViewById<ProgressBar>(R.id.habitProgressBar)
        val editBtn = habitView.findViewById<ImageView>(R.id.habitEditBtn)
        val deleteBtn = habitView.findViewById<ImageView>(R.id.habitDeleteBtn)
        
        titleText.text = habit.title
        
        // Update progress percentage text if it exists
        val progressPercentText = habitView.findViewById<TextView>(R.id.habitProgressPercent)
        val streakText = habitView.findViewById<TextView>(R.id.habitStreak)
        
        updateHabitProgress(habit, progressText, progressBar, checkbox)
        
        // Update additional UI elements if they exist
        val progress = HabitPrefs.getProgress(this, habit.id)
        val percentage = if (habit.targetPerDay > 0) {
            ((progress.toFloat() / habit.targetPerDay) * 100).toInt()
        } else {
            0
        }
        
        progressPercentText?.text = "$percentage%"
        
        if (progress >= habit.targetPerDay) {
            streakText?.visibility = View.VISIBLE
            streakText?.text = "🔥 ${progress} day streak"
        } else {
            streakText?.visibility = View.GONE
        }
        
        // Checkbox click - toggle completion
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                HabitPrefs.incrementProgress(this, habit.id)
            } else {
                val current = HabitPrefs.getProgress(this, habit.id)
                if (current > 0) {
                    HabitPrefs.setProgress(this, habit.id, current - 1)
                }
            }
            updateHabitProgress(habit, progressText, progressBar, checkbox)
            
            // Update additional UI elements
            val progress = HabitPrefs.getProgress(this, habit.id)
            val percentage = if (habit.targetPerDay > 0) {
                ((progress.toFloat() / habit.targetPerDay) * 100).toInt()
            } else {
                0
            }
            
            progressPercentText?.text = "$percentage%"
            
            if (progress >= habit.targetPerDay) {
                streakText?.visibility = View.VISIBLE
                streakText?.text = "🔥 ${progress} day streak"
            } else {
                streakText?.visibility = View.GONE
            }
        }
        
        // Edit button
        editBtn.setOnClickListener {
            showEditHabitDialog(habit)
        }
        
        // Delete button
        deleteBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_habit))
                .setMessage(getString(R.string.delete_habit_confirm, habit.title))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    HabitPrefs.deleteHabit(this, habit.id)
                    loadHabits()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
        
        return habitView
    }
    
    private fun updateHabitProgress(habit: HabitPrefs.Habit, progressText: TextView, progressBar: ProgressBar, checkbox: CheckBox) {
        val progress = HabitPrefs.getProgress(this, habit.id)
        val percentage = HabitPrefs.getCompletionPercentage(this, habit)
        
        progressText.text = "$progress/${habit.targetPerDay}"
        progressBar.progress = percentage
        checkbox.isChecked = progress >= habit.targetPerDay
    }
    
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.habitTitleInput)
        val targetInput = dialogView.findViewById<EditText>(R.id.habitTargetInput)
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val title = titleInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 1
                
                if (title.isNotEmpty()) {
                    HabitPrefs.addHabit(this, title, target)
                    loadHabits()
                } else {
                    Toast.makeText(this, getString(R.string.please_enter_habit_title), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditHabitDialog(habit: HabitPrefs.Habit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.habitTitleInput)
        val targetInput = dialogView.findViewById<EditText>(R.id.habitTargetInput)
        
        titleInput.setText(habit.title)
        targetInput.setText(habit.targetPerDay.toString())
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val title = titleInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 1
                
                if (title.isNotEmpty()) {
                    val updatedHabit = habit.copy(title = title, targetPerDay = target)
                    HabitPrefs.updateHabit(this, updatedHabit)
                    loadHabits()
                } else {
                    Toast.makeText(this, getString(R.string.please_enter_habit_title), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
