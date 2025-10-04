package com.sahil.pulseup.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.sahil.pulseup.R
import com.sahil.pulseup.activities.*
import com.sahil.pulseup.activities.MainFragmentActivity
import com.sahil.pulseup.data.UserPrefs
import com.sahil.pulseup.data.HydrationPrefs

class SettingsFragment : Fragment() {

    private var photoUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dataUri = result.data?.data
            if (dataUri != null) {
                photoUri = dataUri
                view?.findViewById<ImageView>(R.id.profilePhoto)?.setImageURI(dataUri)
                UserPrefs.setProfilePhoto(requireContext(), dataUri.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backBtn = view.findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener { 
            // Switch back to HomeFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(HomeFragment())
        }

        val profilePhoto = view.findViewById<ImageView>(R.id.profilePhoto)
        val changePhoto = view.findViewById<Button>(R.id.changePhotoBtn)
        val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val phoneInput = view.findViewById<TextInputEditText>(R.id.phoneInput)
        val saveBtn = view.findViewById<Button>(R.id.saveBtn)
        val logoutBtn = view.findViewById<Button>(R.id.logoutBtn)
        
        // Hydration settings
        val hydrationSwitch = view.findViewById<Switch>(R.id.hydrationSwitch)
        val hydrationInterval = view.findViewById<SeekBar>(R.id.hydrationInterval)
        val hydrationIntervalText = view.findViewById<TextView>(R.id.hydrationIntervalText)
        val hydrationStartTime = view.findViewById<SeekBar>(R.id.hydrationStartTime)
        val hydrationStartText = view.findViewById<TextView>(R.id.hydrationStartText)
        val hydrationEndTime = view.findViewById<SeekBar>(R.id.hydrationEndTime)
        val hydrationEndText = view.findViewById<TextView>(R.id.hydrationEndText)

        // Prefill user data
        UserPrefs.getSavedName(requireContext())?.let { nameInput.setText(it) }
        UserPrefs.getSavedEmail(requireContext())?.let { emailInput.setText(it) }
        UserPrefs.getProfilePhoto(requireContext())?.let { uriStr ->
            runCatching { Uri.parse(uriStr) }.onSuccess { uri ->
                photoUri = uri
                profilePhoto.setImageURI(uri)
            }
        }
        
        // Prefill hydration settings
        hydrationSwitch.isChecked = HydrationPrefs.isEnabled(requireContext())
        hydrationInterval.progress = HydrationPrefs.getIntervalHours(requireContext()) - 1
        hydrationStartTime.progress = HydrationPrefs.getStartHour(requireContext())
        hydrationEndTime.progress = HydrationPrefs.getEndHour(requireContext())
        updateHydrationTexts()

        changePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            pickImage.launch(Intent.createChooser(intent, "Select Profile Photo"))
        }

        // Hydration settings listeners
        hydrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && android.os.Build.VERSION.SDK_INT >= 33) {
                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                if (requireActivity().checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(permission), 1001)
                    // Don't enable until permission result, UI switch remains checked; we'll enable in onRequestPermissionsResult
                    return@setOnCheckedChangeListener
                }
            }
            HydrationPrefs.setEnabled(requireContext(), isChecked)
            Toast.makeText(requireContext(), if (isChecked) "Hydration reminders enabled" else "Hydration reminders disabled", Toast.LENGTH_SHORT).show()
        }
        
        hydrationInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    HydrationPrefs.setIntervalHours(requireContext(), progress + 1)
                    updateHydrationTexts()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        hydrationStartTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    HydrationPrefs.setStartHour(requireContext(), progress)
                    updateHydrationTexts()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        hydrationEndTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    HydrationPrefs.setEndHour(requireContext(), progress)
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
                Toast.makeText(requireContext(), "Name and Email are required", Toast.LENGTH_SHORT).show()
            } else {
                UserPrefs.updateProfile(requireContext(), name, email, phone)
                Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
            }
        }

        logoutBtn.setOnClickListener {
            UserPrefs.setLoggedIn(requireContext(), false)
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Bottom navigation
        view.findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            // Switch to HomeFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(HomeFragment())
        }
        view.findViewById<LinearLayout>(R.id.navProfile)?.isSelected = true
        view.findViewById<LinearLayout>(R.id.navHabits)?.setOnClickListener {
            // Switch to HabitsFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(HabitsFragment())
        }
        view.findViewById<LinearLayout>(R.id.navMood)?.setOnClickListener {
            // Switch to MoodJournalFragment using MainFragmentActivity
            (requireActivity() as MainFragmentActivity).loadFragment(MoodJournalFragment())
        }
        view.findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            // Already on Profile/Settings - do nothing
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hydrationSwitch = view?.findViewById<Switch>(R.id.hydrationSwitch)
            hydrationSwitch?.isChecked = granted
            HydrationPrefs.setEnabled(requireContext(), granted)
            if (!granted) {
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateHydrationTexts() {
        val intervalHours = HydrationPrefs.getIntervalHours(requireContext())
        val startHour = HydrationPrefs.getStartHour(requireContext())
        val endHour = HydrationPrefs.getEndHour(requireContext())
        
        view?.findViewById<TextView>(R.id.hydrationIntervalText)?.text = "Every $intervalHours hour(s)"
        view?.findViewById<TextView>(R.id.hydrationStartText)?.text = "Start: ${String.format("%02d:00", startHour)}"
        view?.findViewById<TextView>(R.id.hydrationEndText)?.text = "End: ${String.format("%02d:00", endHour)}"
    }
}
