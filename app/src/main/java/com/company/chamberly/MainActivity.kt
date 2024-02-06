package com.company.chamberly

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("userDetails", Context.MODE_PRIVATE)
        val hasLoggedIn = sharedPreferences.getBoolean("hasLoggedIn", false)
        val displayName = sharedPreferences.getString("displayName", "Anonymous")

        if (!hasLoggedIn || Firebase.auth.currentUser == null ) {
            redirectToWelcomeActivity()
            return
        }

        setContentView(R.layout.activity_main)
        checkNotificationPermission()

        val createButton = findViewById<Button>(R.id.createChamberButton)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }

        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
        usernameTextView.text = displayName
        val profilePicButton = findViewById<ImageButton>(R.id.profilePic)
        profilePicButton.setOnClickListener {
            showProfileOptionsPopup()
        }

        val followUsButton = findViewById<Button>(R.id.followUs)
        followUsButton.setOnClickListener {
            openInstagramPage("https://www.instagram.com/chamberly_app/")
        }

        val myChambersButton = findViewById<ImageButton>(R.id.myChambersButton)
        myChambersButton.setOnClickListener {
            val intent = Intent(this, ActiveChambersActivity::class.java)
            startActivity(intent)
        }


        createButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, CreateActivity::class.java)
            startActivity(intent)
        }
        val searchButton = findViewById<Button>(R.id.findChamberButton)
        searchButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, SearchActivity::class.java)
            startActivity(intent)
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                auth.signOut()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun showProfileOptionsPopup() {
        val options = arrayOf("Delete Account", "Show Privacy Policy")
        val builder = AlertDialog.Builder(this)
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> deleteAccount() // Delete account option
                1 -> showPrivacyPolicy() // Show privacy policy option
            }
        }
        builder.show()
    }

    private fun showPrivacyPolicy() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chamberly.net/privacy-policy"))
        startActivity(browserIntent)
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            // Optional: Delete user's associated data from Firestore

            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("userDetails", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        clear()
                        apply()
                    }
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    redirectToWelcomeActivity()
                } else {
                    Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.remove()
        }
        super.onDestroy()
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
    }
    private fun checkNotificationPermission() {
        if (!isNotificationPermissionGranted()) {
            // Notification permission is not granted, show a button to request it
            requestNotificationPermission()
        }
    }

    private fun redirectToWelcomeActivity() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun openInstagramPage(url: String) {
        try {
            // Try to open the Instagram page in the Instagram app
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$url"))
            intent.setPackage("com.instagram.android")
            startActivity(intent)
        } catch (e: Exception) {
            // If the Instagram app is not installed, open the page in a web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$url"))
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } else {
            // For devices prior to Android 8, show a toast to explain how to enable notifications
            Toast.makeText(this, "Please enable notifications for this app in system settings", Toast.LENGTH_LONG).show()
        }
    }
}