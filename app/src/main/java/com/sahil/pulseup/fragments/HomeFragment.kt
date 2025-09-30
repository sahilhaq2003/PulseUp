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

        val moodJournalCard: CardView = view.findViewById(R.id.moodJournalCard)
        moodJournalCard.setOnClickListener {
            startActivity(Intent(requireContext(), CalendarActivity::class.java))
        }

        // Bottom nav: open Mood (calendar) or Profile
        view.findViewById<LinearLayout>(R.id.navMood)?.setOnClickListener {
            startActivity(Intent(requireContext(), CalendarActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.navHabits)?.setOnClickListener {
            startActivity(Intent(requireContext(), HabitsActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
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
            for (i in 6 downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -i)
                val month = c.get(Calendar.MONTH)
                val year = c.get(Calendar.YEAR)
                val day = c.get(Calendar.DAY_OF_MONTH)
                val mood = MoodPrefs.getMood(requireContext(), year, month, day)
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
            val text = view?.findViewById<TextView>(R.id.hydrationNextText)
            text?.text = HydrationPrefs.getNextReminderText(requireContext())
        } catch (_: Exception) { }
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
                background = resources.getDrawable(R.drawable.rounded_button_bg, null)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 12, 16, 0)
                }
            }
            
            seeMoreButton.setOnClickListener {
                startActivity(Intent(requireContext(), HabitsActivity::class.java))
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
}
