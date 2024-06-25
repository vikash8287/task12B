package com.chamberly.chamberly.presentation.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import com.chamberly.chamberly.R

class UpdateActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.update_notice)
        val contentType = intent.getStringExtra("type")
        val updateButton = findViewById<Button>(R.id.update_button)
        val updateTitle = findViewById<TextView>(R.id.update_title)
        val updateBody = findViewById<TextView>(R.id.update_body)
        if(contentType == "appDisabled") {
            updateButton.visibility = View.GONE
            updateTitle.text = "App down"
            updateBody.text = "We are currently facing some issues. Please try again later."
        } else {
            val appId = application.packageName
            updateButton.setOnClickListener {
                try {
                    startActivity(Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$appId")
                    ))
                } catch (e: ActivityNotFoundException) {
                    // If the Play Store is not installed, open the web browser
                    startActivity(Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appId")
                    ))
                }
            }
        }

        onBackPressedDispatcher.addCallback {
            //Do nothing. Users should be unable to navigate to any screen from here.
        }
    }
}