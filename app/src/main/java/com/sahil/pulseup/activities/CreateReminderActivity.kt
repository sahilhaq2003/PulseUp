package com.sahil.pulseup.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sahil.pulseup.R
import com.sahil.pulseup.data.HydrationPrefs

class CreateReminderActivity : AppCompatActivity() {
    
    private lateinit var timePicker: TimePicker
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var frequencyGroup: RadioGroup
    private lateinit var backButton: ImageView
    
    private var selectedFrequency = "once"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_reminder)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.buttonContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()
        setupAnimations()
    }
    
    private fun initializeViews() {
        timePicker = findViewById(R.id.timePicker)
        saveButton = findViewById(R.id.saveReminderBtn)
        cancelButton = findViewById(R.id.cancelButton)
        frequencyGroup = findViewById(R.id.frequencyGroup)
        backButton = findViewById(R.id.backButton)
        
        // Set default time to current time + 1 hour
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.HOUR_OF_DAY, 1)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            timePicker.minute = calendar.get(java.util.Calendar.MINUTE)
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            @Suppress("DEPRECATION")
            timePicker.currentMinute = calendar.get(java.util.Calendar.MINUTE)
        }
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finishWithAnimation()
        }
        
        cancelButton.setOnClickListener {
            finishWithAnimation()
        }
        
        saveButton.setOnClickListener {
            createReminder()
        }
        
        frequencyGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedFrequency = when (checkedId) {
                R.id.radioOnce -> "once"
                R.id.radioDaily -> "daily"
                R.id.radioCustom -> "custom"
                else -> "once"
            }
        }
    }
    
    private fun setupAnimations() {
        // Animate cards entrance
        val cards = listOf(
            findViewById<View>(R.id.headerCard),
            findViewById<View>(R.id.timeCard),
            findViewById<View>(R.id.frequencyCard)
        )
        
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 50f
            
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(index * 100L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
    
    private fun createReminder() {
        val hour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.hour
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour
        }
        val minute = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.minute
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentMinute
        }
        
        // Save reminder settings based on frequency
        HydrationPrefs.setEnabled(this, true)
        HydrationPrefs.setStartHour(this, hour)
        
        when (selectedFrequency) {
            "once" -> {
                // For once, set end time to same hour
                HydrationPrefs.setEndHour(this, hour)
                HydrationPrefs.setIntervalHours(this, 1)
            }
            "daily" -> {
                // For daily, set end time to 10 PM and interval to 2 hours
                HydrationPrefs.setEndHour(this, 22)
                HydrationPrefs.setIntervalHours(this, 2)
            }
            "custom" -> {
                // For custom, set end time to 8 PM and interval to 3 hours
                HydrationPrefs.setEndHour(this, 20)
                HydrationPrefs.setIntervalHours(this, 3)
            }
        }
        
        // Set the next reminder time
        HydrationPrefs.setNextReminderTime(this, hour, minute)
        
        // Show success animation
        showSuccessAnimation(hour, minute)
    }
    
    private fun showSuccessAnimation(hour: Int, minute: Int) {
        // Disable buttons during animation
        saveButton.isEnabled = false
        cancelButton.isEnabled = false
        
        // Create success message
        val timeString = String.format("%02d:%02d", hour, minute)
        val message = getString(R.string.reminder_set_for, timeString)
        
        // Animate button to show success
        val originalText = saveButton.text
        val originalBackground = saveButton.background
        
        saveButton.text = getString(R.string.created)
        saveButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
        
        // Animate scale
        val scaleAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f)
        scaleAnimator.duration = 200
        scaleAnimator.addUpdateListener { animator ->
            val scale = animator.animatedValue as Float
            saveButton.scaleX = scale
            saveButton.scaleY = scale
        }
        
        scaleAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Show toast and finish
                Toast.makeText(this@CreateReminderActivity, message, Toast.LENGTH_LONG).show()
                
                // Delay before finishing
                saveButton.postDelayed({
                    finishWithAnimation()
                }, 1000)
            }
        })
        
        scaleAnimator.start()
    }
    
    private fun finishWithAnimation() {
        // Animate exit
        val rootView = findViewById<View>(android.R.id.content)
        rootView.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            .start()
    }
    
    override fun onBackPressed() {
        finishWithAnimation()
    }
}
