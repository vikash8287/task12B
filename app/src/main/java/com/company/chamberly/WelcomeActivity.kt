package com.company.chamberly

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

// TODO: Add cache file for sign in
class WelcomeActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private var token: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("userDetails", Context.MODE_PRIVATE)
        val hasLoggedIn = sharedPreferences.getBoolean("hasLoggedIn", false)

        if (hasLoggedIn && Firebase.auth.currentUser != null) {
            goToMainActivity()
        } else {
            setContentView(R.layout.activity_welcome)


            val currentUser = auth.currentUser
            // Check if the user is logged in
            //token = getFcmToken{}
            val messaging = FirebaseMessaging.getInstance()
            messaging.isAutoInitEnabled = true
            if (currentUser != null) {
                userExist(currentUser.uid) { exists ->
                    if (exists) {
                        // The user exists
                        sharedPreferences.edit().putBoolean("hasLoggedIn", true).apply()
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                        goToMainActivity()
                    } else {
                        // The user does not exist
                        // Handle the case when the user does not exist
                        // For example, show a login screen or redirect to sign up page
                    }
                }
            } else {
                // The user is not logged in
                // Handle the case when the user is not logged in
                // For example, show a login screen or redirect to sign up page
                auth.signInAnonymously()
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            sharedPreferences.edit().putBoolean("hasLoggedIn", true).apply()
                            Toast.makeText(this, "Welcome!${user?.uid}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }


            val addButton = findViewById<Button>(R.id.btnCreateAccount)
            addButton.setOnClickListener {
                check()
            }


            val tvTermsConditions = findViewById<TextView>(R.id.tvTermsConditions)
            val fullText =
                getString(R.string.feel_free_we_do_not_ask_your_real_name_but_it_must_comply_with_the_terms_conditions)
            val spannableString = SpannableString(fullText)

            val termsStart = fullText.indexOf("Terms & Conditions")
            val termsEnd = termsStart + "Terms & Conditions".length

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(view: View) {
                    // Handle the click event here, for example, open a browser with the URL
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.chamberly.net/terms-and-conditions")
                    )
                    startActivity(browserIntent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.parseColor("#7A7AFF") // Set the color you want here
                    ds.isUnderlineText = false // Remove underline
                }
            }

            spannableString.setSpan(
                clickableSpan,
                termsStart,
                termsEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            tvTermsConditions.text = spannableString
            tvTermsConditions.movementMethod = LinkMovementMethod.getInstance()
            tvTermsConditions.highlightColor =
                Color.TRANSPARENT // Optional: Remove the default click highlight color

        }
    }


    //TODO: Check if user exist in database
    private fun userExist(uid: String, callback: (Boolean) -> Unit) {
        // Check if UID is exist in Display_Names collection
        val displayNameRef = database.collection("Display_Names").whereEqualTo("UID", uid)
        displayNameRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Check if UID is exist in Accounts collection
                    val accountRef = database.collection("Accounts").whereEqualTo("UID", uid)
                    accountRef.get()
                        .addOnSuccessListener { querySnapshot ->
                            callback(!querySnapshot.isEmpty)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    private fun check() {
        val user = Firebase.auth.currentUser
        val editText = findViewById<EditText>(R.id.etEmail)

        if (user != null) {
            val displayName = editText.text.toString()
            // Save displayName to SharedPreferences
            val sharedPreferences = getSharedPreferences("userDetails", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("uid", user.uid)
            editor.putString("displayName", displayName)
            editor.apply()


            // Check if displayName is already used
            val displayNameRef =
                database.collection("Display_Names").whereEqualTo("displayName", displayName)
            displayNameRef.get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        // displayName is available, proceed with storing data
                        // store fcm token also for receiving notifications
                        getFcmToken { fcmToken ->
                            if (fcmToken != null) {
                                if (fcmToken.isNotBlank() && fcmToken.isNotBlank()) {
                                    token = fcmToken
                                }
                            }
                        }
                        val displayNameData = mapOf(
                            "Display_Name" to displayName,
                            "Email" to "${user.uid}@chamberly.net",
                            "UID" to user.uid,
                            "FCMTOKEN" to token
                        )
                        database.collection("Display_Names").document(displayName)
                            .set(displayNameData)
                            .addOnSuccessListener {
                                // Add a new document with a generated ID into Account collection
                                Toast.makeText(this, "Welcome to Chamberly!", Toast.LENGTH_SHORT)
                                    .show()

                                val account = mapOf(
                                    "UID" to user.uid,
                                    "Display_Name" to displayName,
                                    "Email" to "${user.uid}@chamberly.net",
                                    "platform" to "android",
                                    "timestamp" to FieldValue.serverTimestamp(),
                                    "FCMTOKEN" to token
                                )
                                database.collection("Accounts").document(user.uid)
                                    .set(account)
                                    .addOnSuccessListener {
                                        Log.d("TAG", "DocumentSnapshot successfully written!")

                                        // Save displayName to SharedPreferences
                                        val sharedPreferences =
                                            getSharedPreferences("cache", Context.MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()
                                        editor.putString("uid", user.uid)
                                        editor.putString("displayName", displayName)
                                        editor.apply()

                                        // Go to MainActivity
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish() // Optional: Finish WelcomeActivity to prevent going back
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("TAG", "Error writing document", e)
                                        Toast.makeText(
                                            this,
                                            "Error writing document",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Error storing displayName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        // displayName is already used
                        Toast.makeText(this, "This name has been used!", Toast.LENGTH_SHORT).show()
                        //editText.error = "This name has been used!"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking displayName", Toast.LENGTH_SHORT).show()
                }
        }
    }

    //asynchronous operation
    fun getFcmToken(callback: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            if (fcmToken != null) {
                Log.i("tokenFCM", fcmToken)
                callback(fcmToken)
            } else {
                Log.i("tokenFCM", "Token is null")
                callback(null)
            }
        }.addOnFailureListener {
            Log.e("tokenFCM", "Error fetching token", it)
            callback(null)
        }
    }
}