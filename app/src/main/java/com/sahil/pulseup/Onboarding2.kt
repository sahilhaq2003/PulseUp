package com.sahil.pulseup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Onboarding2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.third_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nextButton2 = findViewById<Button>(R.id.nextButton3)
        nextButton2.setOnClickListener {
            val intent = Intent(this, Onboarding3::class.java)
            startActivity(intent)
        }
    }
}
