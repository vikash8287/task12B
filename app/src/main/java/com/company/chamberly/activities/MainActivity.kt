package com.company.chamberly.activities

import android.app.ActionBar.LayoutParams
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper

import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.company.chamberly.adapters.TopicRequestRecyclerViewAdapter
import com.company.chamberly.logEvent
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.setAutoLogAppEventsEnabled
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlin.math.log

class MainActivity : ComponentActivity() {
    private val auth = Firebase.auth
    private val realtimeDb = Firebase.database
    private val firestore = Firebase.firestore
    private var isShowingJoinDialog: Boolean = false
    private lateinit var topicRequestsAdapter: TopicRequestRecyclerViewAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var isUserRestricted: Boolean = false
    private lateinit var appEventsLogger: AppEventsLogger


    // TODO: Change venter to ventor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAutoLogAppEventsEnabled(false)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        // TODO: Complete implementation of app events logger (facebook)
        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
        appEventsLogger = AppEventsLogger.Companion.newLogger(this)
        appEventsLogger.logEvent("TestEvent")
        val hasLoggedIn = sharedPreferences.getBoolean("hasLoggedIn", false)
        val displayName = sharedPreferences.getString("displayName", "Anonymous")

        if (!hasLoggedIn || Firebase.auth.currentUser == null) {
            redirectToWelcomeActivity()
            return
        }
        setContentView(R.layout.activity_main)

        val messaging = FirebaseMessaging.getInstance()
        checkRestrictions()
        messaging.token.addOnCompleteListener { task ->
            if(!task.isSuccessful) {
                Log.w("FCMTOKEN", "FCMTOKEN fetch failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val editor = sharedPreferences.edit()
            editor.putString("notificationKey", token)
            editor.apply()
        }
        messaging.isAutoInitEnabled = true

        checkUserRating()
        val groupChatId = intent.getStringExtra("GroupChatId") ?: "" // Default to empty string if null
        val groupTitle = intent.getStringExtra("GroupTitle") ?: ""
        val authorName = intent.getStringExtra("Authorname") ?: ""
        val authorUID = intent.getStringExtra("AuthorUID") ?: auth.currentUser!!.uid
        checkAndGoToChatActivity(groupChatId, groupTitle, authorName, authorUID)

        val createChamberButton = findViewById<Button>(R.id.createChamberButton)
        val createTopicButton = findViewById<Button>(R.id.createTopicButton)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            requestNotificationPermission()
        }

        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
        usernameTextView.text = displayName
        val profilePicButton = findViewById<ImageButton>(R.id.profilePic)
        profilePicButton.setOnClickListener { buttonView ->
            showProfileOptionsPopup(buttonView)
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


        createChamberButton.setOnClickListener {
            val intent = Intent(this, CreateChamberActivity::class.java)
            startActivity(intent)
        }
        createTopicButton.setOnClickListener {
            val intent = Intent(this, CreateTopicActivity::class.java)
            startActivity(intent)
        }
        val searchButton = findViewById<Button>(R.id.findChamberButton)
        searchButton.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        val findTopicButton = findViewById<Button>(R.id.findTopicButton)
        findTopicButton.setOnClickListener {
            val intent = Intent(this, SearchTopicActivity::class.java)
            startActivity(intent)
        }

    }

    private fun checkAndGoToChatActivity(groupChatId: String, groupTitle: String, authorName: String, authorUID: String) {
        if (groupChatId.isNotBlank()) {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("GroupChatId", groupChatId)
            intent.putExtra("GroupTitle", groupTitle)
            intent.putExtra("Authorname", authorName)
            intent.putExtra("AuthorUID", authorUID)
        }

    }

    private fun showProfileOptionsPopup(buttonView: View) {

        val profileOptionsPopUp = Dialog(this, R.style.Dialog)
        profileOptionsPopUp.setContentView(R.layout.popup_profile_options)

        val deleteAccountButton = profileOptionsPopUp.findViewById<TextView>(R.id.delete_account)
        val showPrivacyPolicyButton = profileOptionsPopUp.findViewById<TextView>(R.id.show_privacy_policy)
        val submitFeedbackButton = profileOptionsPopUp.findViewById<TextView>(R.id.submit_feedback)

        val roleSelectorButton = profileOptionsPopUp.findViewById<RadioGroup>(R.id.role_selector_group)
        val confirmRoleChangeView = profileOptionsPopUp.findViewById<LinearLayout>(R.id.confirm_role_change_view)
        val confirmRoleChangeButton = profileOptionsPopUp.findViewById<TextView>(R.id.confirm_role_change_button)
        val ventorButton = profileOptionsPopUp.findViewById<RadioButton>(R.id.role_ventor)
        val listenerButton = profileOptionsPopUp.findViewById<RadioButton>(R.id.role_listener)

        var isListener = sharedPreferences.getBoolean("isListener", false)

        roleSelectorButton.setOnCheckedChangeListener { _, selectedButton ->
            listenerButton.setTextColor(if(selectedButton == R.id.role_listener) Color.BLACK else Color.WHITE)
            ventorButton.setTextColor(if(selectedButton == R.id.role_ventor) Color.BLACK else Color.WHITE)

            if(isListener && selectedButton == R.id.role_ventor) {
                confirmRoleChangeView.visibility = View.VISIBLE
                confirmRoleChangeButton.setOnClickListener {
                    isListener = false
                    setRole("ventor")
                    confirmRoleChangeView.visibility = View.GONE
                }
            } else if(!isListener && selectedButton == R.id.role_listener) {
                confirmRoleChangeView.visibility = View.VISIBLE
                confirmRoleChangeButton.setOnClickListener {
                    isListener = true
                    setRole("listener")
                    confirmRoleChangeView.visibility = View.GONE
                }
            } else {
                confirmRoleChangeView.visibility = View.GONE
            }
        }
        roleSelectorButton.check(if(isListener) R.id.role_listener else R.id.role_ventor)


        deleteAccountButton.setOnClickListener {
            profileOptionsPopUp.dismiss()
            deleteAccount()
        }
        showPrivacyPolicyButton.setOnClickListener {
            profileOptionsPopUp.dismiss()
            showPrivacyPolicy()
        }
        submitFeedbackButton.setOnClickListener {
            submitFeedback(profileOptionsPopUp)
        }

        val params = WindowManager.LayoutParams()
        params.copyFrom(profileOptionsPopUp.window?.attributes)
        params.gravity = Gravity.TOP
        params.y = buttonView.bottom
        profileOptionsPopUp.window?.attributes = params
        profileOptionsPopUp.show()
    }

    private fun showPrivacyPolicy() {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chamberly.net/privacy-policy"))
        startActivity(browserIntent)
    }

    private fun setRole(role: String) {
        val editor = sharedPreferences.edit()
        val uid = sharedPreferences.getString("uid", auth.currentUser!!.uid) ?: ""
        val currUserRef = firestore.collection("Accounts").document(uid)
        currUserRef.update("selectedRole", role)
        stopProcrastination()
        editor.putBoolean("isListener", role == "listener")
        editor.apply()
    }



    private fun deleteAccount() {
        val user = auth.currentUser
        val uid = sharedPreferences.getString("uid", auth.currentUser!!.uid)
        val displayName = sharedPreferences.getString("displayName", "")
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "account_deleted",
            params = hashMapOf(
                "UID" to uid!!,
                "name" to displayName!!
            )
        )

        if (user != null) {
            // Optional: Delete user's associated data from Firestore
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    with(sharedPreferences.edit()) {
                        clear()
                        putBoolean("isNewUser", false)
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

    private fun submitFeedback(dialog: Dialog) {
        dialog.setContentView(R.layout.dialog_feedback)
        dialog.show()
        val submitButton = dialog.findViewById<Button>(R.id.submitFeedbackButton)
        val dismissButton = dialog.findViewById<Button>(R.id.dismissFeedbackDialogButton)
        val editText = dialog.findViewById<EditText>(R.id.feedback_text)
        val feedbackSuccessText = dialog.findViewById<TextView>(R.id.feedback_success_text)
        dismissButton?.setOnClickListener {
            dialog.dismiss()
        }

        submitButton?.setOnClickListener {
            val feedbackText = "Android: ${editText.text}"
            val feedbackRef = firestore.collection("Feedback").document()
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", "") ?: auth.currentUser!!.uid
            val displayName = sharedPreferences.getString("displayName", "") ?: ""
            feedbackRef.set(mapOf(
                "byName" to displayName,
                "byUID" to uid,
                "feedbackData" to feedbackText,
                "timestamp" to FieldValue.serverTimestamp()
            )).addOnSuccessListener {
                editText.visibility = View.GONE
                feedbackSuccessText.visibility = View.VISIBLE
                submitButton.visibility = View.GONE
            }
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.setPackage("com.instagram.android")
            startActivity(intent)
        } catch (e: Exception) {
            // If the Instagram app is not installed, open the page in a web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // TODO: Handle denial of permission
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(this@MainActivity, "Grant notification permissions", Toast.LENGTH_SHORT).show()
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    private fun checkRestrictions() {
        val uid = sharedPreferences.getString("uid", "") ?: auth.currentUser!!.uid
        val userRef = firestore.collection("Accounts").document(uid)
        userRef.update(mapOf(Pair("timestamp", FieldValue.serverTimestamp())))
        userRef.addSnapshotListener { snapshot, error ->
            if(snapshot != null && snapshot.exists()) {
                val currentTime = snapshot.getTimestamp("timestamp")
                val restrictionRef = firestore.collection("Restrictions").document(uid)
                restrictionRef.addSnapshotListener { value, error ->
                    if(value != null && value.exists()) {
                        val restrictedUntil = value.getTimestamp("restrictedUntil")
                        isUserRestricted = if(restrictedUntil != null && currentTime != null) {
                            restrictedUntil > currentTime
                        } else {
                            false
                        }
                        attachTopicRequestListeners()
                    }
                }
            }
        }
    }

    private fun attachTopicRequestListeners() {
        val savedTopics = sharedPreferences.getString("topics", "")!!.split(",").toMutableList()
        val editor = sharedPreferences.edit()
        for (topic: String in savedTopics) {
            if (topic.isBlank()) {
                continue
            }
            val topicRef = realtimeDb.reference.child(topic)
            val userRef = topicRef
                .child("users")
                .child(sharedPreferences.getString("uid", "") ?: "")

            userRef
                .child("isReserved")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isReserved = snapshot.value as Boolean?
                        if (isReserved == null) {
                            savedTopics.remove(topic)
                            editor.putString("topics", savedTopics.joinToString(","))
                            editor.apply()
                        } else {
                            userRef.child("restricted").setValue(isUserRestricted)
                            if (isReserved) {
                                var topicTitle = ""
                                var reserverID = ""
                                topicRef.child("topicTitle")
                                    .get()
                                    .addOnSuccessListener { topicTitleSnapshot ->
                                        topicTitle = topicTitleSnapshot.value.toString()
                                        topicRef.child("users")
                                            .child(auth.currentUser!!.uid)
                                            .child("reservedBy")
                                            .get()
                                            .addOnSuccessListener { reservedBySnapshot ->
                                                reserverID = reservedBySnapshot.value.toString()
                                                showTopicJoinDialog("$topic::$topicTitle::$reserverID")
                                            }
                                    }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }

    private fun showTopicJoinDialog(topic: String) {
        if(topic.isBlank()) {
            return
        }
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.topic_join_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val savedTopics = (sharedPreferences.getString("topics", "")?:"").split(",").toMutableList()
        val userName = sharedPreferences.getString("displayName", "") ?: "Anonymous"
        val currUserUID = sharedPreferences.getString("uid", auth.currentUser!!.uid)!!
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.topic_requests)
        val editor = sharedPreferences.edit()
        if (isShowingJoinDialog) {
            topicRequestsAdapter.addItem(topic)
        } else {
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            val dividerItemDecoration = DividerItemDecoration(this, layoutManager.orientation)
            recyclerView.addItemDecoration(dividerItemDecoration)
            topicRequestsAdapter = TopicRequestRecyclerViewAdapter(
                listOf(topic),
                acceptRequest = { topicId, topicTitle, reserverID, reservedBy ->
                    val topicRef = realtimeDb.reference.child(topicId)
                    savedTopics.remove(topicId)
                    editor.putString("topics", savedTopics.joinToString(","))
                    editor.apply()
                    topicRef
                        .child("users")
                        .child(currUserUID)
                        .child("isReady")
                        .setValue(true)
                    topicRef
                        .child("users")
                        .child(reserverID)
                        .child("groupChatId")
                        .setValue("")
                    topicRef.child("users")
                        .child(reserverID)
                        .child("groupTitle")
                        .setValue(topicTitle)
                    topicRef
                        .child("users")
                        .child(reserverID)
                        .child("groupChatId")
                        .addValueEventListener(object: ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val groupChatId = snapshot.value as String?
                                if(!groupChatId.isNullOrBlank()) {
                                    logEvent(
                                        firebaseAnalytics = firebaseAnalytics,
                                        eventName = "New_Match",
                                        params = hashMapOf(
                                            "UID" to currUserUID,
                                            "name" to userName
                                        )
                                    )
                                    logEvent(
                                        firebaseAnalytics = firebaseAnalytics,
                                        eventName = "accepted_match",
                                        params = hashMapOf(
                                            "UID" to currUserUID,
                                            "name" to userName,
                                            "groupChatId" to groupChatId
                                        )
                                    )
                                    val chamberRef = realtimeDb.reference.child(groupChatId)
                                    topicRef.child("users").child(reserverID).removeValue()
                                    topicRef.child("users").child(currUserUID).removeValue()

                                    chamberRef
                                        .child("users")
                                        .child("members")
                                        .child(currUserUID)
                                        .child("name")
                                        .setValue(userName)

                                    firestore
                                        .collection("GroupChatIds")
                                        .document(groupChatId)
                                        .update("locked", true, "members",FieldValue.arrayUnion(currUserUID))

                                    firestore
                                        .collection("GroupChatIds")
                                        .document(groupChatId)
                                        .update("locked", true)
                                        .addOnSuccessListener {
                                            val userRef = firestore
                                                .collection("users")
                                                .document(currUserUID)
                                            userRef.update("chambers", FieldValue.arrayUnion(groupChatId))
                                            val intent = Intent(this@MainActivity, ChatActivity::class.java)
                                            intent.putExtra("GroupChatId", groupChatId)
                                            intent.putExtra("GroupTitle", topicTitle)
                                            intent.putExtra("Authorname", userName)
                                            intent.putExtra("AuthorUID", currUserUID)
                                            startActivity(intent)
                                        }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }

                        })
                    topicRequestsAdapter.removeItem("$topicId::$topicTitle::$reserverID")
                    if(topicRequestsAdapter.itemCount == 0) {
                        dialog.dismiss()
                        isShowingJoinDialog = false
                    }
                },
                denyRequest = { topicId, topicTitle, reserverID ->
                    realtimeDb.reference.child(topicId).child("users").child(auth.currentUser!!.uid).child("isReserved").setValue(false)
                    realtimeDb.reference.child(topicId).child("users").child(auth.currentUser!!.uid).child("reservedBy").removeValue()
                    topicRequestsAdapter.removeItem("$topicId::$topicTitle::$reserverID")
                    logEvent(
                        firebaseAnalytics = firebaseAnalytics,
                        eventName = "skipped_match",
                        params = hashMapOf(
                            "UID" to currUserUID,
                            "name" to userName
                        )
                    )
                    if(topicRequestsAdapter.itemCount == 0) {
                        dialog.dismiss()
                        isShowingJoinDialog = false
                    }
                }
            )
            recyclerView.adapter = topicRequestsAdapter
            dialog.show()
            isShowingJoinDialog = true
        }
    }
    private fun stopProcrastination() {
        val editor = sharedPreferences.edit()
        val uid = sharedPreferences.getString("uid", auth.currentUser!!.uid)
        val topicsList = sharedPreferences.getString("topics", "")!!.split(",") as MutableList<String>
        for(topic in topicsList) {
            if(topic.isNotBlank()) {
                realtimeDb
                    .reference
                    .child(topic)
                    .child("users")
                    .child("members")
                    .child(uid!!)
                    .removeValue()
            }
        }
        editor.remove("topics")
        editor.apply()
    }

    private fun checkUserRating() {
        val uid = sharedPreferences.getString("uid", auth.currentUser!!.uid)!!
        val displayName = sharedPreferences.getString("displayName", "")!!
        val isRestricted = sharedPreferences.getBoolean("isRestricted", false)
        firestore.collection("StarReviews")
            .whereEqualTo("To", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if(!documents.isEmpty) {
                    val review = documents.documents[0]
                    val userRating = review.getDouble("AverageStars")!!
                    val ratingsCount = review.getLong("ReviewsCount")!!.toInt()

                    if(shouldBanUser(userRating = userRating, ratingsCount = ratingsCount)) {
                        // Ban user
                        logEvent(
                            firebaseAnalytics = firebaseAnalytics,
                            eventName = "star_reviews_auto_restriction",
                            params = hashMapOf(
                                "uid" to uid,
                                "name" to displayName
                            )
                        )
                        if(!isRestricted) {
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("isRestricted", true)
                            editor.apply()
                            banUser(uid)
                        }
                    } else if(isRestricted){
                        unbanUser(uid)
                    }
                }
            }
    }

    private fun shouldBanUser(userRating: Double, ratingsCount: Int): Boolean {
        return userRating <= 3.5 && ratingsCount >= 6
    }

    private fun banUser(uid: String) {
        val topicsList = sharedPreferences.getString("topics", "")!!.split(",") as MutableList<String>

        for(topic in topicsList) {

        }
    }

    private fun unbanUser(uid: String) {

    }
}