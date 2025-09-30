package com.sahil.pulseup

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.cardview.widget.CardView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.*


class Home : AppCompatActivity() {
    private lateinit var habitsContainer: LinearLayout
    private var lineChart: com.sahil.pulseup.ui.LineChartView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        // If no user is logged in, send to Login
        if (!UserPrefs.isLoggedIn(this)) {
            startActivity(Intent(this, Login::class.java))
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
            val intent = Intent(this,Calender::class.java)
            startActivity(intent)
        }

        // Bottom nav: open Mood (calendar) or Profile
        findViewById<LinearLayout>(R.id.navMood)?.setOnClickListener {
            startActivity(Intent(this, Calender::class.java))
        }
        findViewById<LinearLayout>(R.id.navHabits)?.setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        lineChart = findViewById(R.id.lineChart)
        loadHabits()
        updateMoodTrendChart()
        updateHydrationCard()

        // Hydration add button -> open CreateReminder
        findViewById<android.widget.ImageButton>(R.id.hydrationAddBtn)?.setOnClickListener {
            startActivity(Intent(this, CreateReminder::class.java))
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
            val moods = com.sahil.pulseup.MoodPrefs.loadMoods(this, month, year)
            val preview = findViewById<TextView>(R.id.moodPreviewEmoji)
            preview?.text = moods[day] ?: "🙂"
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
                "😊" -> 0.8f
                "😐" -> 0.5f
                "😡" -> 0.1f
                else -> 0.6f
            }

            val values = FloatArray(7)
            for (i in 6 downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -i)
                val month = c.get(Calendar.MONTH)
                val year = c.get(Calendar.YEAR)
                val day = c.get(Calendar.DAY_OF_MONTH)
                val mood = com.sahil.pulseup.MoodPrefs.getMood(this, year, month, day)
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
            text?.text = com.sahil.pulseup.HydrationPrefs.getNextReminderText(this)
        } catch (_: Exception) { }
    }
    
    private fun loadHabits() {
        habitsContainer.removeAllViews()
        val habits = HabitPrefs.getHabits(this)
        
        if (habits.isEmpty()) {
            // Show empty state
            val emptyView = TextView(this).apply {
                text = "No habits yet. Tap + to add your first habit!"
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
                text = "See More (${habits.size - 2} more)"
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
        updateHabitProgress(habit, progressText, progressBar, checkbox)
        
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
        }
        
        // Edit button
        editBtn.setOnClickListener {
            showEditHabitDialog(habit)
        }
        
        // Delete button
        deleteBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete '${habit.title}'?")
                .setPositiveButton("Delete") { _, _ ->
                    HabitPrefs.deleteHabit(this, habit.id)
                    loadHabits()
                }
                .setNegativeButton("Cancel", null)
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
            .setTitle("Add New Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 1
                
                if (title.isNotEmpty()) {
                    HabitPrefs.addHabit(this, title, target)
                    loadHabits()
                } else {
                    Toast.makeText(this, "Please enter a habit title", Toast.LENGTH_SHORT).show()
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
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 1
                
                if (title.isNotEmpty()) {
                    val updatedHabit = habit.copy(title = title, targetPerDay = target)
                    HabitPrefs.updateHabit(this, updatedHabit)
                    loadHabits()
                } else {
                    Toast.makeText(this, "Please enter a habit title", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
