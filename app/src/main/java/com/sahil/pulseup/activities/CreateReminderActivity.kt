package com.sahil.pulseup.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sahil.pulseup.R

class CreateReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_reminder)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val timePicker = findViewById<android.widget.TimePicker>(R.id.timePicker)
        val saveButton = findViewById<Button>(R.id.saveReminderBtn)

        saveButton.setOnClickListener {
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
            
            Toast.makeText(this, "Reminder set for ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
