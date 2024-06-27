package com.company.chamberly.activities

import android.app.AlarmManager
import android.app.Dialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.company.chamberly.adapters.TopicRequestRecyclerViewAdapter
import com.company.chamberly.notification.CheckUpNotification
import com.company.chamberly.notification.ReminderNotification
import com.company.chamberly.viewmodels.UserViewModel
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

class MainActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val realtimeDb = Firebase.database
    private val firestore = Firebase.firestore
    private var isShowingJoinDialog: Boolean = false
    private lateinit var topicRequestsAdapter: TopicRequestRecyclerViewAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var isUserRestricted: Boolean = false
    private lateinit var appEventsLogger: AppEventsLogger
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAutoLogAppEventsEnabled(false)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
        appEventsLogger = AppEventsLogger.Companion.newLogger(this)
        appEventsLogger.logEvent("TestEvent")

        setContentView(R.layout.activity_main)
startCheckUpNotificationService(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        userViewModel.appState.observe(this) {
            if(!it.isAppEnabled) {
                val intent = Intent(applicationContext, UpdateActivity::class.java)
                intent.putExtra("type", "appDisabled")
                startActivity(intent)
                return@observe
            }
            if(!it.isAppUpdated) {
                goToUpdateScreen()
            }
        }

        userViewModel.loginUser()

        userViewModel.userState.observe(this) {
            if(it.UID.isBlank()) {
                navController.navigate(
                    R.id.welcome_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                        popUpTo(R.id.welcome_fragment) {
                            this.inclusive = true
                        }
                    }
                )
            } else if (navController.currentDestination?.id == R.id.welcome_fragment) {
                checkRestrictions()
                navController.navigate(
                    R.id.main_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                        popUpTo(R.id.welcome_fragment) {
                            this.inclusive = true
                        }
                    }
                )
                val groupChatID = intent.getStringExtra("groupChatId") ?: ""
                checkAndOpenChat(groupChatId = groupChatID)
            }
        }

        userViewModel.chamberID.observe(this) {
            if(!it.isNullOrBlank()) {
                navController.navigate(
                    R.id.chat_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
            } else {
                if (navController.currentDestination?.id == R.id.chat_fragment) {
                   navController.popBackStack()
                }
            }
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            requestNotificationPermission()
        }
    }

    private fun goToUpdateScreen() {
        val intent = Intent(applicationContext, UpdateActivity::class.java)
        intent.putExtra("type", "update")
        startActivity(intent)
        finish()
    }

    private fun checkAndOpenChat(groupChatId: String) {
        if (groupChatId.isNotBlank()) {
            userViewModel.openChamber(groupChatId)
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
        Log.d("TOPICSSAVED", savedTopics.toString())
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
                                                showTopicJoinDialog(topic,topicTitle,reserverID)
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

    private fun showTopicJoinDialog(topicID: String, topicTitle: String, reserverID: String) {
        if(topicID.isBlank()) {
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
            topicRequestsAdapter.addItem(topicID,topicTitle,reserverID)
        } else {
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            val dividerItemDecoration = DividerItemDecoration(this, layoutManager.orientation)
            recyclerView.addItemDecoration(dividerItemDecoration)
            topicRequestsAdapter = TopicRequestRecyclerViewAdapter(
                listOf(topicID),
                listOf(topicTitle),
                listOf(reserverID),
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
                                    userViewModel.logEventToAnalytics(
                                        "New_Match"
                                    )
                                    userViewModel.logEventToAnalytics(
                                        "accepted_match"
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
                                            userViewModel.openChamber(groupChatId)
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
                    realtimeDb.reference
                        .child(topicId)
                        .child("users")
                        .child(auth.currentUser!!.uid)
                        .child("isReserved")
                        .setValue(false)
                    realtimeDb.reference
                        .child(topicId)
                        .child("users")
                        .child(auth.currentUser!!.uid)
                        .child("reservedBy")
                        .removeValue()
                    topicRequestsAdapter.removeItem(topicId)
                    userViewModel.logEventToAnalytics("skipped_match")
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

    private fun checkUserRating() {
        val uid = userViewModel.userState.value!!.UID
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
                        userViewModel.logEventToAnalytics("star_reviews_auto_restriction")
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
        val topicsList =
            sharedPreferences.getString("topics", "")!!.split(",")
                    as MutableList<String>

//        for(topic in topicsList) {
//
//        }
    }

    private fun unbanUser(uid: String) {

    }

    private fun startCheckUpNotificationService(context: Context) {
        val backgroundTaskState:Boolean = sharedPreferences.getBoolean("checkUpNotification",false)
        if(backgroundTaskState) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, CheckUpNotification::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)


        if(hasScheduleExactAlarmPermission(context)){
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),AlarmManager.INTERVAL_DAY, pendingIntent)

        }
        with(sharedPreferences.edit()){
            putBoolean("checkUpNotification",true)
            commit()
        }
    }
    private fun hasScheduleExactAlarmPermission(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

}