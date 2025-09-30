package com.sahil.pulseup.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sahil.pulseup.R
import com.sahil.pulseup.data.HydrationPrefs
import com.sahil.pulseup.data.UserPrefs

class ProfileActivity : AppCompatActivity() {

    private var photoUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dataUri = result.data?.data
            if (dataUri != null) {
                photoUri = dataUri
                findViewById<ImageView>(R.id.profilePhoto).setImageURI(dataUri)
                UserPrefs.setProfilePhoto(this, dataUri.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        val profilePhoto = findViewById<ImageView>(R.id.profilePhoto)
        val changePhoto = findViewById<Button>(R.id.changePhotoBtn)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)
        
        // Hydration settings
        val hydrationSwitch = findViewById<Switch>(R.id.hydrationSwitch)
        val hydrationInterval = findViewById<SeekBar>(R.id.hydrationInterval)
        val hydrationIntervalText = findViewById<TextView>(R.id.hydrationIntervalText)
        val hydrationStartTime = findViewById<SeekBar>(R.id.hydrationStartTime)
        val hydrationStartText = findViewById<TextView>(R.id.hydrationStartText)
        val hydrationEndTime = findViewById<SeekBar>(R.id.hydrationEndTime)
        val hydrationEndText = findViewById<TextView>(R.id.hydrationEndText)

        // Prefill user data
        UserPrefs.getSavedName(this)?.let { nameInput.setText(it) }
        UserPrefs.getSavedEmail(this)?.let { emailInput.setText(it) }
        UserPrefs.getProfilePhoto(this)?.let { uriStr ->
            runCatching { Uri.parse(uriStr) }.onSuccess { uri ->
                photoUri = uri
                profilePhoto.setImageURI(uri)
            }
        }
        
        // Prefill hydration settings
        hydrationSwitch.isChecked = HydrationPrefs.isEnabled(this)
        hydrationInterval.progress = HydrationPrefs.getIntervalHours(this) - 1
        hydrationStartTime.progress = HydrationPrefs.getStartHour(this)
        hydrationEndTime.progress = HydrationPrefs.getEndHour(this)
        updateHydrationTexts()

        changePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            pickImage.launch(Intent.createChooser(intent, "Select Profile Photo"))
        }

        // Hydration settings listeners
        hydrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && android.os.Build.VERSION.SDK_INT >= 33) {
                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(permission), 1001)
                    // Don't enable until permission result, UI switch remains checked; we'll enable in onRequestPermissionsResult
                    return@setOnCheckedChangeListener
                }
            }
            HydrationPrefs.setEnabled(this, isChecked)
            Toast.makeText(this, if (isChecked) "Hydration reminders enabled" else "Hydration reminders disabled", Toast.LENGTH_SHORT).show()
        }
        
        hydrationInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    HydrationPrefs.setIntervalHours(this@ProfileActivity, progress + 1)
                    updateHydrationTexts()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        hydrationStartTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    HydrationPrefs.setStartHour(this@ProfileActivity, progress)
                    updateHydrationTexts()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        hydrationEndTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    HydrationPrefs.setEndHour(this@ProfileActivity, progress)
                    updateHydrationTexts()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
            } else {
                UserPrefs.updateProfile(this, name, email, phone)
                Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
            }
        }

        logoutBtn.setOnClickListener {
            UserPrefs.setLoggedIn(this, false)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hydrationSwitch = findViewById<Switch>(R.id.hydrationSwitch)
            hydrationSwitch.isChecked = granted
            HydrationPrefs.setEnabled(this, granted)
            if (!granted) {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateHydrationTexts() {
        val intervalHours = HydrationPrefs.getIntervalHours(this)
        val startHour = HydrationPrefs.getStartHour(this)
        val endHour = HydrationPrefs.getEndHour(this)
        
        findViewById<TextView>(R.id.hydrationIntervalText)?.text = "Every $intervalHours hour(s)"
        findViewById<TextView>(R.id.hydrationStartText)?.text = "Start: ${String.format("%02d:00", startHour)}"
        findViewById<TextView>(R.id.hydrationEndText)?.text = "End: ${String.format("%02d:00", endHour)}"
    }
}
