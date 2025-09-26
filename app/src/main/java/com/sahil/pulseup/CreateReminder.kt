package com.sahil.pulseup

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TimePicker
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class CreateReminder : AppCompatActivity() {
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
        val saveBtn = findViewById<Button>(R.id.saveReminderBtn)

        // initialize to current time
        val cal = Calendar.getInstance()
        timePicker.hour = cal.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = cal.get(Calendar.MINUTE)

        saveBtn.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            // Save chosen time to HydrationPrefs as next reminder time (simple implementation)
            HydrationPrefs.setNextReminderTime(this, hour, minute)
            android.widget.Toast.makeText(this, "Reminder set for ${String.format("%02d", hour)}:${String.format("%02d", minute)}", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
