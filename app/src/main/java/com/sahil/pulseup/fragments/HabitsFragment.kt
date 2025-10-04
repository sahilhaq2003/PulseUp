package com.sahil.pulseup.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.sahil.pulseup.R
import com.sahil.pulseup.activities.*
import com.sahil.pulseup.activities.MainFragmentActivity
import com.sahil.pulseup.data.HabitPrefs

class HabitsFragment : Fragment() {
    private lateinit var habitsContainer: LinearLayout
    private var showAllHabits = true  // Show all habits by default in fragment mode
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        habitsContainer = view.findViewById(R.id.habitsContainer)
        
        // Add habit button
        view.findViewById<ImageView>(R.id.addHabitBtn)?.setOnClickListener {
            showAddHabitDialog()
        }
        
        // Back button
        view.findViewById<ImageView>(R.id.backBtn)?.setOnClickListener {
            // Switch back to HomeFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(HomeFragment())
        }
        
        // Add navigation functionality to header
        view.findViewById<LinearLayout>(R.id.header)?.setOnClickListener {
            showAllHabits = !showAllHabits
            loadHabits()
        }
        
        // Bottom navigation
        view.findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            // Switch to HomeFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(HomeFragment())
        }
        view.findViewById<LinearLayout>(R.id.navHabits)?.isSelected = true
        view.findViewById<LinearLayout>(R.id.navMood)?.setOnClickListener {
            // Switch to MoodJournalFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(MoodJournalFragment())
        }
        view.findViewById<LinearLayout>(R.id.navHabits)?.setOnClickListener {
            // Already on Habits - do nothing
        }
        view.findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            // Switch to SettingsFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(SettingsFragment())
        }
        
        loadHabits()
    }

    override fun onResume() {
        super.onResume()
        // Refresh habits when returning to screen
        loadHabits()
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
        
        habits.forEach { habit ->
            val habitView = createHabitView(habit)
            habitsContainer.addView(habitView)
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
