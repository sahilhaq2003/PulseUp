package com.sahil.pulseup

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.cardview.widget.CardView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.*


class Home : AppCompatActivity() {
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
            // Already on Home - you could scroll to top here if desired
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        // (legacy settingsTab removed) bottom nav uses navProfile
    }

    override fun onResume() {
        super.onResume()
        // Update preview emoji for today
        try {
            val today = Calendar.getInstance()
            val month = today.get(Calendar.MONTH)
            val year = today.get(Calendar.YEAR)
            val day = today.get(Calendar.DAY_OF_MONTH)
            val moods = MoodPrefs.loadMoods(this, month, year)
            val preview = findViewById<TextView>(R.id.moodPreviewEmoji)
            preview?.text = moods[day] ?: "🙂"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
