package com.company.chamberly.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.company.chamberly.R
import com.company.chamberly.logEvent
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// TODO: Add cache file for sign in
//TODO: Fix auto login. Wait for user to click create account to log them in.
class WelcomeActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private val realtimeDB = Firebase.database.reference
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var sharedPreferences: SharedPreferences
    private var token: String? = ""

//    private lateinit var appUpdateManager: AppUpdateManager
//    private val updateType = AppUpdateType.IMMEDIATE
//    private val updateRequestCode = 200
    private var isListener: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        realtimeDB
            .child("UX_Android")
            .child("shouldAuthenticateUser")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    checkAppDisabled(!((snapshot.value as Boolean?) ?: true))
                }

                override fun onCancelled(error: DatabaseError) {
                    //Most likely a network issue has occurred. Will implement later.
                }
            })
        realtimeDB
            .child("UX_Android")
            .child("latestAndroidAppVersion")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    checkForAppUpdates(snapshot.value as String? ?: "1.0.0")
                }
                override fun onCancelled(error: DatabaseError) {
                    //Most likely a network issue has occurred. Will implement later.
                }
            })

        sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val hasLoggedIn = sharedPreferences.getBoolean("hasLoggedIn", false)

//        appUpdateManager = AppUpdateManagerFactory.create(this)

//        if (updateType == AppUpdateType.FLEXIBLE) {
//            appUpdateManager.registerListener(installStateUpdatedListener)
//        }
        if (hasLoggedIn && auth.currentUser != null) {
            goToMainActivity()
        }
        else {
            setContentView(R.layout.activity_welcome)

            val messaging = FirebaseMessaging.getInstance()
            messaging.token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCMTOKEN", "FCMTOKEN fetch failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
            }
            messaging.isAutoInitEnabled = true

            handleRole()
            val addButton = findViewById<Button>(R.id.btnCreateAccount)
            val etEmail = findViewById<EditText>(R.id.etEmail)
            addButton.setOnClickListener {
                addButton.isEnabled = false
                etEmail.isEnabled = false
                auth.signInAnonymously()
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("hasLoggedIn", true)
                            editor.putString("uid", user!!.uid)
                            editor.apply()
                        } else {
                            // Add more helpful message
                            Toast.makeText(
                                baseContext,
                                "Authentication failed. ${task.result}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                check()
                addButton.isEnabled = true
                etEmail.isEnabled = true
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

    private fun handleRole() {
        val roleSelectorButtonGroup = findViewById<RadioGroup>(R.id.role_selector_group)
        val roleVenterButton = findViewById<RadioButton>(R.id.role_ventor)
        val roleListenerButton = findViewById<RadioButton>(R.id.role_listener)
        val roleDetails = findViewById<TextView>(R.id.role_details)

        roleVenterButton.setOnClickListener { roleSelectorButtonGroup.check(R.id.role_ventor) }
        roleListenerButton.setOnClickListener { roleSelectorButtonGroup.check(R.id.role_listener) }

        roleSelectorButtonGroup.setOnCheckedChangeListener { _, selectedButton ->
            isListener = selectedButton == R.id.role_listener
            roleListenerButton.setTextColor(if(isListener) Color.BLACK else Color.WHITE)
            roleVenterButton.setTextColor(if(!isListener) Color.BLACK else Color.WHITE)
            roleDetails.text =
                if(isListener) getString(R.string.listener_details)
                else getString(R.string.ventor_details)
        }

        roleSelectorButtonGroup.check(R.id.role_ventor)
    }

    private fun checkForAppUpdates(newVersion: String) {
//        appUpdateManager
//            .appUpdateInfo
//            .addOnSuccessListener { info ->
//                val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
//                val isUpdateAllowed = when (updateType) {
//                    AppUpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
//                    AppUpdateType.IMMEDIATE -> info.isImmediateUpdateAllowed
//                    else -> false
//                }
//                if (isUpdateAvailable && isUpdateAllowed) {
//                    Log.i("AppUpdate","New version is available: ${info.availableVersionCode()}")
//                    appUpdateManager.startUpdateFlowForResult(
//                        info,
//                        this,
//                        AppUpdateOptions.defaultOptions(updateType),
//                        updateRequestCode
//                    )
//                } else {
//                    Log.d("AppUpdate","No update available $isUpdateAllowed || $isUpdateAvailable")
//                }
//            }
//            .addOnFailureListener {
//                Log.d("AppUpdate", "Update check failed ${it.message}")
//            }
        try {
            val pInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            val versionCode = pInfo.versionName
            if(canUpdate(versionCode, newVersion)) {
                goToUpdateScreen()
            }
        } catch(e: Exception) {
            // Handle this case
        }
    }

    private fun canUpdate(currentVersionCode: String, newVersion: String): Boolean {
        // TODO: Fix the implementation
        return currentVersionCode != newVersion && newVersion.isNotBlank()
    }

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
        val intent = Intent(this@WelcomeActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    private fun check() {
        val user = Firebase.auth.currentUser
        val editText = findViewById<EditText>(R.id.etEmail)

        if (user != null) {
            val displayName = editText.text.toString()
            logEvent(
                firebaseAnalytics = firebaseAnalytics,
                eventName = "landed_on_confirm_login_page_vc",
                params = hashMapOf(
                    "uid" to user.uid,
                    "name" to displayName
                )
            )
            // Save displayName to SharedPreferences
            val isNewUser = sharedPreferences.getBoolean("isNewUser", true)
            val editor = sharedPreferences.edit()
            editor.putString("uid", user.uid)
            editor.putString("displayName", displayName)
            editor.putBoolean("isListener", isListener)
            editor.apply()


            // Check if displayName is already used
            val displayNameRef =
                database.collection("Display_Names")
                    .document(displayName)
            displayNameRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val displayNameData = mapOf(
                        "Display_Name" to displayName,
                        "Email" to "${user.uid}@chamberly.net",
                        "UID" to user.uid
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
                                "isModerator" to false,
                                "timestamp" to FieldValue.serverTimestamp(),
                                "selectedRole" to if(isListener) "listener" else "ventor"
                            )
                            database.collection("Accounts").document(user.uid)
                                .set(account)
                                .addOnSuccessListener {
                                    editor.putString("uid", user.uid)
                                    editor.putString("displayName", displayName)
                                    editor.putBoolean("isNewUser", false)
                                    editor.apply()
                                    logEvent(
                                        firebaseAnalytics = firebaseAnalytics,
                                        eventName = if (isNewUser) "first_time_user" else "account_recreated",
                                        params = hashMapOf(
                                            "uid" to user.uid,
                                            "name" to displayName
                                        )
                                    )
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
                                "Error storing displayName ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking displayName", Toast.LENGTH_SHORT).show()
                }
            val restrictionsRef = database.collection("Restrictions")
            restrictionsRef
                .document(user.uid)
                .set(mapOf(
                    "UID" to user.uid,
                    "restrictedUntil" to FieldValue.serverTimestamp()
                ))
        }
    }

    //asynchronous operation
    fun getFcmToken(callback: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { result ->
            if (result != null) {
                Log.i("tokenFCM", result)
                callback(result)
            } else {
                Log.i("tokenFCM", "Token is null")
                callback(null)
            }
        }.addOnFailureListener { exception ->
            Log.e("tokenFCM", "Error fetching token", exception)
            callback(null)
        }
    }

    override fun onResume() {
        super.onResume()
//        if (updateType == AppUpdateType.IMMEDIATE) {
//            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
//                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
//                    appUpdateManager.startUpdateFlowForResult(
//                        info,
//                        this,
//                        AppUpdateOptions.defaultOptions(updateType),
//                        updateRequestCode
//                    )
//                }
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        if (updateType == AppUpdateType.FLEXIBLE) {
//            appUpdateManager.unregisterListener(installStateUpdatedListener)
//        }
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Toast.makeText(
                applicationContext,
                "Update downloaded.",
                Toast.LENGTH_LONG
            ).show()
            lifecycleScope.launch {
                delay(5.seconds)
//                appUpdateManager.completeUpdate()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if(requestCode == updateRequestCode) {
//            if (resultCode != RESULT_OK) {
//                Log.w("AppUpdate", "Error occurred while updating.")
//            }
//            val temp = emptyList<String>()
//        }
    }

    private fun goToUpdateScreen() {
        val intent = Intent(applicationContext, UpdateActivity::class.java)
        intent.putExtra("type", "update")
        startActivity(intent)
        finish()
    }

    private fun checkAppDisabled(androidAppDisabled: Boolean) {
        if(androidAppDisabled) {
            val intent = Intent(applicationContext, UpdateActivity::class.java)
            intent.putExtra("type", "appDisabled")
            startActivity(intent)
        }
    }
}