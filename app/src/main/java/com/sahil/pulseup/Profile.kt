package com.sahil.pulseup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Profile : AppCompatActivity() {

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

        // Prefill
        UserPrefs.getSavedName(this)?.let { nameInput.setText(it) }
        UserPrefs.getSavedEmail(this)?.let { emailInput.setText(it) }
        UserPrefs.getProfilePhoto(this)?.let { uriStr ->
            runCatching { Uri.parse(uriStr) }.onSuccess { uri ->
                photoUri = uri
                profilePhoto.setImageURI(uri)
            }
        }

        changePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            pickImage.launch(Intent.createChooser(intent, "Select Profile Photo"))
        }

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
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}


