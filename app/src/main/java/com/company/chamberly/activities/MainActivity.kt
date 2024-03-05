package com.company.chamberly.activities

import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.company.chamberly.adapters.TopicRequestRecyclerViewAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.internal.notify

class MainActivity : ComponentActivity() {
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val auth = Firebase.auth
    private val realtimeDb = Firebase.database
    private val firestore = Firebase.firestore
    private var isShowingJoinDialog: Boolean = false
    private lateinit var topicReqeustsAdapter: TopicRequestRecyclerViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val hasLoggedIn = sharedPreferences.getBoolean("hasLoggedIn", false)
        val displayName = sharedPreferences.getString("displayName", "Anonymous")

        if (!hasLoggedIn || Firebase.auth.currentUser == null) {
            redirectToWelcomeActivity()
            return
        }
        attachTopicRequestListeners()
        setContentView(R.layout.activity_main)
        checkNotificationPermission()

        val messaging = FirebaseMessaging.getInstance()

        messaging.token.addOnCompleteListener { task ->
            if(!task.isSuccessful) {
                Log.w("FCMTOKEN", "FCMTOKEN fetch failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result

            Log.d("FCMTOKEN", "SUCCESS $token")
        }
        messaging.isAutoInitEnabled = true

        val groupChatId = intent.getStringExtra("GroupChatId") ?: "" // Default to empty string if null
        val groupTitle = intent.getStringExtra("GroupTitle") ?: ""
        val authorName = intent.getStringExtra("AuthorName") ?: ""
        val authorUID = intent.getStringExtra("AuthorUID") ?: auth.currentUser!!.uid
        checkAndGoToChatActivity(groupChatId, groupTitle, authorName, authorUID)

        val createChamberButton = findViewById<Button>(R.id.createChamberButton)
        val createTopicButton = findViewById<Button>(R.id.createTopicButton)
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


        createChamberButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, CreateChamberActivity::class.java)
            startActivity(intent)
        }
        createTopicButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, CreateTopicActivity::class.java)
            startActivity(intent)
        }
        val searchButton = findViewById<Button>(R.id.findChamberButton)
        searchButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, SearchActivity::class.java)
            startActivity(intent)
        }
        val findTopicButton = findViewById<Button>(R.id.findTopicButton)
        findTopicButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, SearchTopicActivity::class.java)
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

    private fun checkAndGoToChatActivity(groupChatId: String, groupTitle: String, authorName: String, authorUID: String) {
        Log.d("DATA", "$groupTitle::$groupTitle:$groupChatId")
        if (groupChatId.isNotBlank()) {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("GroupChatId", groupChatId)
            intent.putExtra("GroupTitle", groupTitle)
            intent.putExtra("AuthorName", authorName)
            intent.putExtra("AuthorUID", authorUID)
        }

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
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chamberly.net/privacy-policy"))
        startActivity(browserIntent)
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            // Optional: Delete user's associated data from Firestore

            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } else {
            // For devices prior to Android 8, show a toast to explain how to enable notifications
            Toast.makeText(
                this,
                "Please enable notifications for this app in system settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun attachTopicRequestListeners() {
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val savedTopics = sharedPreferences.getString("topics", "")!!.split(",").toMutableList()
        val editor = sharedPreferences.edit()
        for (topic: String in savedTopics) {
            Log.d("TOPCIS", topic)
            if (topic.isBlank()) {
                continue
            }
            val topicRef = realtimeDb.reference.child(topic)
            val userRef = topicRef
                .child("users")
                .child(auth.currentUser!!.uid)

            Log.d("TOPICS", auth.currentUser?.uid + "----")
            userRef
                .child("isReserved")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isReserved = snapshot.value as Boolean?
                        if (isReserved == null) {
                            savedTopics.remove(topic)
                            editor.putString("topics", savedTopics.joinToString(","))
                            editor.apply()
                        } else if (isReserved) {
                            topicRef.get()
                                .addOnSuccessListener {  topicDocument ->
                                    val topicTitle = topicDocument.child("TopicTitle").value.toString()
                                    val reserverID = topicDocument.child("users").child(auth.currentUser!!.uid).child("reservedBy").value.toString()
                                    showTopicJoinDialog("$topic::$topicTitle::$reserverID")
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
        }

    }

    fun showTopicJoinDialog(topic: String) {
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.topic_join_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val savedTopics = (sharedPreferences.getString("topics", "")?:"").split(",").toMutableList()
        val userName = sharedPreferences.getString("displayName", "") ?: "Anonymous"
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.topic_requests)
        val editor = sharedPreferences.edit()
        if (isShowingJoinDialog) {
            topicReqeustsAdapter.addItem(topic)
        } else {
            recyclerView.layoutManager = LinearLayoutManager(this)
            topicReqeustsAdapter = TopicRequestRecyclerViewAdapter(
                listOf(topic),
                acceptRequest = { topicId, topicTitle, reserverID, reservedBy ->
                    val topicRef = realtimeDb.reference.child(topicId)
                    val currUserUID = auth.currentUser!!.uid
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
//                                            savedTopics.remove(topicId)
                                            editor.putString("topics", savedTopics.joinToString(","))
                                            editor.apply()
                                            val intent = Intent(this@MainActivity, ChatActivity::class.java)
                                            intent.putExtra("GroupChatId", groupChatId)
                                            intent.putExtra("GroupTitle", topicTitle)
                                            intent.putExtra("AuthorName", userName)
                                            intent.putExtra("AuthorUID", currUserUID)
                                            startActivity(intent)
                                            finish()
                                        }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }

                        })
                    topicReqeustsAdapter.removeItem("$topicId::$topicTitle::$reserverID")
                    if(topicReqeustsAdapter.itemCount == 0) {
                        dialog.dismiss()
                        isShowingJoinDialog = false
                    }
                },
                denyRequest = { topicId, topicTitle, reserverID ->
                    realtimeDb.reference.child(topicId).child("users").child(auth.currentUser!!.uid).child("isReserved").setValue(false)
                    realtimeDb.reference.child(topicId).child("users").child(auth.currentUser!!.uid).child("reservedBy").removeValue()
                    topicReqeustsAdapter.removeItem("$topicId::$topicTitle::$reserverID")
                    if(topicReqeustsAdapter.itemCount == 0) {
                        dialog.dismiss()
                        isShowingJoinDialog = false
                    }
                }
            )
            recyclerView.adapter = topicReqeustsAdapter
            dialog.show()
            isShowingJoinDialog = true
        }
    }
}