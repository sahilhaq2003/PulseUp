package com.sahil.pulseup.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.sahil.pulseup.R
import com.sahil.pulseup.data.UserPrefs
import com.sahil.pulseup.fragments.*

class MainFragmentActivity : AppCompatActivity() {
    
    private lateinit var currentFragment: Fragment
    private var isTabletLayout = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_fragment)
        
        // If no user is logged in, send to Login
        if (!UserPrefs.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_fragment_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Check if we're using tablet layout
        val navigationPanel = findViewById<View>(R.id.navigation_panel)
        isTabletLayout = navigationPanel != null
        
        if (isTabletLayout) {
            setupTabletNavigation()
        }
        
        // Load the default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }
    
    private fun setupTabletNavigation() {
        findViewById<LinearLayout>(R.id.nav_home)?.setOnClickListener {
            loadFragment(HomeFragment())
            updateNavigationSelection(R.id.nav_home)
        }
        
        findViewById<LinearLayout>(R.id.nav_habits)?.setOnClickListener {
            loadFragment(HabitsFragment())
            updateNavigationSelection(R.id.nav_habits)
        }
        
        findViewById<LinearLayout>(R.id.nav_mood)?.setOnClickListener {
            loadFragment(MoodJournalFragment())
            updateNavigationSelection(R.id.nav_mood)
        }
        
        findViewById<LinearLayout>(R.id.nav_settings)?.setOnClickListener {
            loadFragment(SettingsFragment())
            updateNavigationSelection(R.id.nav_settings)
        }
        
        findViewById<android.widget.Button>(R.id.logout_btn)?.setOnClickListener {
            UserPrefs.setLoggedIn(this, false)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        // Set initial selection
        updateNavigationSelection(R.id.nav_home)
    }
    
    private fun updateNavigationSelection(selectedId: Int) {
        if (!isTabletLayout) return
        
        val navItems = listOf(R.id.nav_home, R.id.nav_habits, R.id.nav_mood, R.id.nav_settings)
        navItems.forEach { id ->
            val item = findViewById<LinearLayout>(id)
            if (item != null) {
                // Find the first ImageView in the LinearLayout
                val imageViewCount = item.childCount
                for (i in 0 until imageViewCount) {
                    val child = item.getChildAt(i)
                    if (child is ImageView) {
                        if (id == selectedId) {
                            child.setColorFilter(resources.getColor(R.color.colorPrimary, null))
                        } else {
                            child.setColorFilter(resources.getColor(R.color.colorTextSecondary, null))
                        }
                        break
                    }
                }
                
                // Update background
                if (id == selectedId) {
                    item.setBackgroundResource(R.drawable.card_professional)
                } else {
                    item.setBackgroundResource(R.drawable.card_professional)
                }
            }
        }
        
        // Update stats in tablet layout
        updateTabletStats()
    }
    
    private fun updateTabletStats() {
        // Update quick stats shown in tablet navigation (only if views exist)
        try {
            findViewById<TextView>(R.id.habitsCompleted)?.text = com.sahil.pulseup.data.HabitPrefs.getHabits(this).count { habit ->
                com.sahil.pulseup.data.HabitPrefs.getProgress(this, habit.id) >= habit.targetPerDay
            }.toString()
            
            // Update today's mood
            val today = java.util.Calendar.getInstance()
            val year = today.get(java.util.Calendar.YEAR)
            val month = today.get(java.util.Calendar.MONTH)
            val day = today.get(java.util.Calendar.DAY_OF_MONTH)
            val todaysMood = com.sahil.pulseup.data.MoodPrefs.getMood(this, year, month, day)
            findViewById<TextView>(R.id.moodEntry)?.text = todaysMood ?: "😐"
        } catch (e: Exception) {
            // Views don't exist, ignore
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (isTabletLayout) {
            updateTabletStats()
            val currentType = when(currentFragment) {
                is HomeFragment -> R.id.nav_home
                is HabitsFragment -> R.id.nav_habits  
                is MoodJournalFragment -> R.id.nav_mood
                is SettingsFragment -> R.id.nav_settings
                else -> R.id.nav_home
            }
            updateNavigationSelection(currentType)
        }
    }
    
    fun loadFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
            
        if (isTabletLayout) {
            val fragmentType = when(fragment) {
                is HomeFragment -> R.id.nav_home
                is HabitsFragment -> R.id.nav_habits
                is MoodJournalFragment -> R.id.nav_mood
                is SettingsFragment -> R.id.nav_settings
                else -> R.id.nav_home
            }
            updateNavigationSelection(fragmentType)
        }
    }
    
    override fun onBackPressed() {
        if (currentFragment is HomeFragment) {
            // If on home fragment, exit app
            super.onBackPressed()
        } else {
            // Otherwise, go back to home
            loadFragment(HomeFragment())
            if (isTabletLayout) {
                updateNavigationSelection(R.id.nav_home)
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
