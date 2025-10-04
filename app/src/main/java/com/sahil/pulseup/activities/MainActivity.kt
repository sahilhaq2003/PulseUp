package com.sahil.pulseup.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sahil.pulseup.R
import com.sahil.pulseup.data.UserPrefs

class MainActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        // Check if this is the first time launching the app
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        
        // Debug logging
        android.util.Log.d("MainActivity", "isFirstLaunch: $isFirstLaunch")
        android.util.Log.d("MainActivity", "isLoggedIn: ${UserPrefs.isLoggedIn(this)}")
        
        // FORCE ONBOARDING FOR TESTING - Remove this line later
        val forceOnboarding = false
        
        // Handle the Next button click
        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            if (isFirstLaunch || forceOnboarding) {
                // Mark that onboarding has been shown
                prefs.edit().putBoolean("is_first_launch", false).apply()
                
                // Start onboarding flow
                android.util.Log.d("MainActivity", "Starting onboarding flow")
                startActivity(Intent(this, Onboarding1Activity::class.java))
                finish()
            } else {
                // If not first launch, proceed with normal flow
                proceedWithAppFlow()
            }
        }
        
        // Long press to reset onboarding (for testing)
        nextButton.setOnLongClickListener {
            resetOnboarding()
            android.widget.Toast.makeText(this, "Onboarding reset! Next launch will show onboarding.", android.widget.Toast.LENGTH_SHORT).show()
            true
        }
        
        // Auto-proceed if first launch (show onboarding immediately)
        if (isFirstLaunch || forceOnboarding) {
            // Mark that onboarding has been shown
            prefs.edit().putBoolean("is_first_launch", false).apply()
            
            // Start onboarding flow
            android.util.Log.d("MainActivity", "Starting onboarding flow")
            startActivity(Intent(this, Onboarding1Activity::class.java))
            finish()
            return
        }
        
        // If not first launch, proceed with normal flow
        proceedWithAppFlow()
    }
    
    private fun proceedWithAppFlow() {
        // If user is already logged in, go to main app
        if (UserPrefs.isLoggedIn(this)) {
            startActivity(Intent(this, MainFragmentActivity::class.java))
            finish()
            return
        }
        
        // If not logged in, go to login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    // Helper method to reset onboarding (useful for testing)
    private fun resetOnboarding() {
        prefs.edit().putBoolean("is_first_launch", true).apply()
        android.util.Log.d("MainActivity", "Onboarding reset - next launch will show onboarding")
    }
    
    // Method to force show onboarding (for testing)
    fun forceShowOnboarding() {
        resetOnboarding()
        startActivity(Intent(this, Onboarding1Activity::class.java))
        finish()
    }
}
