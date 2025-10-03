package com.sahil.pulseup.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.sahil.pulseup.R
import com.sahil.pulseup.activities.*
import com.sahil.pulseup.activities.MainFragmentActivity
import com.sahil.pulseup.data.HabitPrefs
import com.sahil.pulseup.data.MoodPrefs
import com.sahil.pulseup.data.HydrationPrefs
import com.sahil.pulseup.ui.LineChartView
import java.util.*

class HomeFragment : Fragment() {
    private lateinit var habitsContainer: LinearLayout
    private var lineChart: LineChartView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        habitsContainer = view.findViewById(R.id.habitsContainer)
        
        // Add habit button
        view.findViewById<ImageView>(R.id.addHabitBtn)?.setOnClickListener {
            showAddHabitDialog()
        }

        setupMoodSelection()

        val moodJournalCard: CardView = view.findViewById(R.id.moodJournalCard)
        moodJournalCard.setOnClickListener {
            // Switch to MoodJournalFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(MoodJournalFragment())
        }

        // Bottom nav: open Mood (calendar) or Profile
        view.findViewById<LinearLayout>(R.id.navMood)?.setOnClickListener {
            // Switch to MoodJournalFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(MoodJournalFragment())
        }
        view.findViewById<LinearLayout>(R.id.navHabits)?.setOnClickListener {
            // Switch to HabitsFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(HabitsFragment())
        }
        view.findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            // Switch to SettingsFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(SettingsFragment())
        }

        lineChart = view.findViewById(R.id.lineChart)
        loadHabits()
        updateMoodTrendChart()
        updateHydrationCard()

        // Hydration add button -> open CreateReminder
        view.findViewById<android.widget.ImageButton>(R.id.hydrationAddBtn)?.setOnClickListener {
            startActivity(Intent(requireContext(), CreateReminderActivity::class.java))
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
            val moods = MoodPrefs.loadMoods(requireContext(), month, year)
            val preview = view?.findViewById<TextView>(R.id.moodPreviewEmoji)
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
            val moodEmojis = arrayOfNulls<String>(7)
            
            for (i in 6 downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -i)
                val month = c.get(Calendar.MONTH)
                val year = c.get(Calendar.YEAR)
                val day = c.get(Calendar.DAY_OF_MONTH)
                val mood = MoodPrefs.getMood(requireContext(), year, month, day)
                val score = emojiToScore(mood)
                values[6 - i] = score
                moodEmojis[6 - i] = mood
                android.util.Log.d("ChartUpdate", "Day $day: mood=$mood, score=$score")
            }
            
            android.util.Log.d("ChartUpdate", "Setting chart values: ${values.contentToString()}")
            lineChart?.setValues(values)
            
            // Update mood trend stats
            updateMoodStats(moodEmojis, values)
            
        } catch (e: Exception) { 
            android.util.Log.e("ChartUpdate", "Error updating chart", e)
        }
    }
    
    private fun updateMoodStats(moodEmojis: Array<String?>, values: FloatArray) {
        try {
            val avgMoodText = view?.findViewById<TextView>(R.id.avgMoodText)
            val trendText = view?.findViewById<TextView>(R.id.trendText)
            val streakText = view?.findViewById<TextView>(R.id.streakText)
            
            // Update average mood
            val validMoods = moodEmojis.filterNotNull()
            if (validMoods.isNotEmpty()) {
                val happyCount = validMoods.count { it == "😊" }
                val neutralCount = validMoods.count { it == "😐" }
                val sadCount = validMoods.count { it == "😡" }
                
                val avgMood = when {
                    happyCount > neutralCount && happyCount > sadCount -> "😊"
                    sadCount > neutralCount && sadCount > happyCount -> "😡"
                    else -> "😐"
                }
                avgMoodText?.text = avgMood
            } else {
                avgMoodText?.text = "😐"
            }
            
            // Update trend
            if (values.size >= 2) {
                val recent = values.takeLast(3).average()
                val older = values.take(3).average()
                trendText?.text = if (recent > older) "📈" else if (recent < older) "📉" else "➡️"
            } else {
                trendText?.text = "➡️"
            }
            
            // Update streak (mock data for now)
            streakText?.text = "3"
            
        } catch (e: Exception) {
            android.util.Log.e("MoodStats", "Error updating mood stats", e)
        }
    }

    private fun updateHydrationCard() {
        try {
            val nextText = view?.findViewById<TextView>(R.id.hydrationNextText)
            val statusText = view?.findViewById<TextView>(R.id.hydrationStatus)
            val progressBar = view?.findViewById<ProgressBar>(R.id.hydrationProgress)
            val progressText = view?.findViewById<TextView>(R.id.hydrationProgressText)
            
            // Update status and next reminder text
            val isEnabled = HydrationPrefs.isEnabled(requireContext())
             if (isEnabled) {
                 statusText?.text = "Active"
                 statusText?.setTextColor(resources.getColor(R.color.colorWhite, null))
                 statusText?.background = resources.getDrawable(R.drawable.status_badge_professional_active, null)
                
                // Show the actual next reminder time
                val nextReminderTime = getNextReminderTime()
                nextText?.text = "Next reminder: $nextReminderTime"
             } else {
                 statusText?.text = "Inactive"
                 statusText?.setTextColor(resources.getColor(R.color.colorWhite, null))
                 statusText?.background = resources.getDrawable(R.drawable.status_badge_professional_inactive, null)
                nextText?.text = "No reminders set"
            }
            
            // Update progress (mock data for now)
            val progress = 75 // This could be calculated based on actual hydration data
            progressBar?.progress = progress
            progressText?.text = "3 of 4 reminders today"
            
        } catch (_: Exception) { }
    }
    
    private fun getNextReminderTime(): String {
        return try {
            // First, try to get the explicitly set next reminder time
            val nextHour = HydrationPrefs.getNextReminderHour(requireContext())
            val nextMinute = HydrationPrefs.getNextReminderMinute(requireContext())
            
            if (nextHour != -1 && nextMinute != -1) {
                // Use the explicitly set time
                val ampm = if (nextHour >= 12) "PM" else "AM"
                val displayHour = if (nextHour > 12) nextHour - 12 else if (nextHour == 0) 12 else nextHour
                val displayMinute = String.format("%02d", nextMinute)
                return "$displayHour:$displayMinute $ampm"
            }
            
            // Fallback to calculated time based on settings
            val startHour = HydrationPrefs.getStartHour(requireContext())
            val intervalHours = HydrationPrefs.getIntervalHours(requireContext())
            val endHour = HydrationPrefs.getEndHour(requireContext())
            
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)
            
            // Calculate next reminder time
            var calculatedHour = startHour
            var calculatedMinute = 0
            
            // If current time is before start time, next reminder is at start time
            if (currentHour < startHour) {
                calculatedHour = startHour
            } else {
                // Find next reminder time based on interval
                var reminderHour = startHour
                while (reminderHour <= endHour) {
                    if (reminderHour > currentHour || (reminderHour == currentHour && currentMinute < calculatedMinute)) {
                        calculatedHour = reminderHour
                        break
                    }
                    reminderHour += intervalHours
                }
                
                // If no reminder found for today, set for tomorrow at start time
                if (calculatedHour > endHour) {
                    calculatedHour = startHour
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            // Format time
            val ampm = if (calculatedHour >= 12) "PM" else "AM"
            val displayHour = if (calculatedHour > 12) calculatedHour - 12 else if (calculatedHour == 0) 12 else calculatedHour
            val displayMinute = String.format("%02d", calculatedMinute)
            
            "$displayHour:$displayMinute $ampm"
            
        } catch (e: Exception) {
            "10:00 AM" // Default fallback
        }
    }
    
    private fun loadHabits() {
        habitsContainer.removeAllViews()
        val habits = HabitPrefs.getHabits(requireContext())
        
        if (habits.isEmpty()) {
            // Show empty state
            val emptyView = TextView(requireContext()).apply {
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
            val seeMoreButton = Button(requireContext()).apply {
                text = "See More (${habits.size - 2} more)"
                textSize = 14f
                setTextColor(resources.getColor(R.color.colorWhite, null))
                setPadding(24, 12, 24, 12)
                // Add rounded corners
                background = resources.getDrawable(R.drawable.button_professional_subtle, null)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 12, 16, 0)
                }
            }
            
            seeMoreButton.setOnClickListener {
                // Switch to HabitsFragment using MainFragmentActivity
                (requireActivity() as MainFragmentActivity).loadFragment(HabitsFragment())
            }
            
            habitsContainer.addView(seeMoreButton)
        }
    }
    
    private fun createHabitView(habit: HabitPrefs.Habit): View {
        val inflater = LayoutInflater.from(requireContext())
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
                HabitPrefs.incrementProgress(requireContext(), habit.id)
            } else {
                val current = HabitPrefs.getProgress(requireContext(), habit.id)
                if (current > 0) {
                    HabitPrefs.setProgress(requireContext(), habit.id, current - 1)
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
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete '${habit.title}'?")
                .setPositiveButton("Delete") { _, _ ->
                    HabitPrefs.deleteHabit(requireContext(), habit.id)
                    loadHabits()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        return habitView
    }
    
    private fun updateHabitProgress(habit: HabitPrefs.Habit, progressText: TextView, progressBar: ProgressBar, checkbox: CheckBox) {
        val progress = HabitPrefs.getProgress(requireContext(), habit.id)
        val percentage = HabitPrefs.getCompletionPercentage(requireContext(), habit)
        
        progressText.text = "$progress/${habit.targetPerDay}"
        progressBar.progress = percentage
        checkbox.isChecked = progress >= habit.targetPerDay
    }
    
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_habit, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.habitTitleInput)
        val targetInput = dialogView.findViewById<EditText>(R.id.habitTargetInput)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Add New Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 1
                
                if (title.isNotEmpty()) {
                    HabitPrefs.addHabit(requireContext(), title, target)
                    loadHabits()
                } else {
                    Toast.makeText(requireContext(), "Please enter a habit title", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditHabitDialog(habit: HabitPrefs.Habit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_habit, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.habitTitleInput)
        val targetInput = dialogView.findViewById<EditText>(R.id.habitTargetInput)
        
        titleInput.setText(habit.title)
        targetInput.setText(habit.targetPerDay.toString())
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 1
                
                if (title.isNotEmpty()) {
                    val updatedHabit = habit.copy(title = title, targetPerDay = target)
                    HabitPrefs.updateHabit(requireContext(), updatedHabit)
                    loadHabits()
                } else {
                    Toast.makeText(requireContext(), "Please enter a habit title", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupMoodSelection() {
        val moodPrefs = requireContext().getSharedPreferences("MoodPrefs", android.content.Context.MODE_PRIVATE)
        
        // Setup mood selection listeners
        requireView().findViewById<LinearLayout>(R.id.moodHappyContainer)?.setOnClickListener {
            saveTodayMood("Happy")
            showMoodSelectedFeedback("Happy")
        }
        
        requireView().findViewById<LinearLayout>(R.id.moodNeutralContainer)?.setOnClickListener {
            saveTodayMood("Neutral")
            showMoodSelectedFeedback("Neutral")
        }
        
        requireView().findViewById<LinearLayout>(R.id.moodSadContainer)?.setOnClickListener {
            saveTodayMood("Sad")
            showMoodSelectedFeedback("Sad")
        }
        
        // Show today's mood if already selected
        loadTodayMood()
    }
    
    private fun saveTodayMood(mood: String) {
        val moodPrefs = requireContext().getSharedPreferences("MoodPrefs", android.content.Context.MODE_PRIVATE)
        val editor = moodPrefs.edit()
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        editor.putString("today_mood", mood)
        editor.putString("today_mood_date", today)
        editor.apply()
        
        // Also save to the mood journal calendar system
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        // Convert mood text to emoji
        val moodEmoji = when (mood) {
            "Happy" -> "😊"
            "Neutral" -> "😐"
            "Sad" -> "😡"
            else -> "😐"
        }
        
        // Save to mood journal calendar
        com.sahil.pulseup.data.MoodPrefs.setMood(requireContext(), year, month, day, moodEmoji)
    }
    
    private fun loadTodayMood() {
        // First check if we have today's mood from the simple selection
        val moodPrefs = requireContext().getSharedPreferences("MoodPrefs", android.content.Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        val savedDate = moodPrefs.getString("today_mood_date", "")
        val savedMood = moodPrefs.getString("today_mood", "")
        
        if (savedDate == today && !savedMood.isNullOrEmpty()) {
            highlightSelectedMood(savedMood)
        } else {
            // Check if mood was set through mood journal calendar
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            
            val moodEmoji = com.sahil.pulseup.data.MoodPrefs.getMood(requireContext(), year, month, day)
            moodEmoji?.let { emoji ->
                val moodText = when (emoji) {
                    "😊" -> "Happy"
                    "😐" -> "Neutral"
                    "😡" -> "Sad"
                    else -> null
                }
                if (moodText != null) {
                    highlightSelectedMood(moodText)
                }
            }
        }
    }
    
    private fun highlightSelectedMood(mood: String) {
        // Reset all mood containers first
        requireView().findViewById<LinearLayout>(R.id.moodHappyContainer)?.alpha = 0.7f
        requireView().findViewById<LinearLayout>(R.id.moodNeutralContainer)?.alpha = 0.7f
        requireView().findViewById<LinearLayout>(R.id.moodSadContainer)?.alpha = 0.7f
        
        // Highlight selected mood
        when (mood) {
            "Happy" -> requireView().findViewById<LinearLayout>(R.id.moodHappyContainer)?.alpha = 1.0f
            "Neutral" -> requireView().findViewById<LinearLayout>(R.id.moodNeutralContainer)?.alpha = 1.0f
            "Sad" -> requireView().findViewById<LinearLayout>(R.id.moodSadContainer)?.alpha = 1.0f
        }
    }
    
    private fun showMoodSelectedFeedback(mood: String) {
        highlightSelectedMood(mood)
        Toast.makeText(requireContext(), "Mood recorded: $mood", Toast.LENGTH_SHORT).show()
    }
}
