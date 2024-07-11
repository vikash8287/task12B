package com.chamberly.chamberly.presentation.activities

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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.notification.CheckUpNotification
import com.chamberly.chamberly.presentation.adapters.TopicRequestRecyclerViewAdapter
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.setAutoLogAppEventsEnabled
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private var isShowingJoinDialog: Boolean = false
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
                val intent = Intent(applicationContext, UpdateActivity::class.java)
                intent.putExtra("type", "update")
                startActivity(intent)
                finish()
            }
        }

        userViewModel.loginUser()

        userViewModel.userState.observe(this) {
            if(it.UID.isBlank()) {
                navController.popBackStack(R.id.main_fragment, true)
                navController.navigate(
                    R.id.authentication_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
            } else if (navController.currentDestination?.id == R.id.authentication_fragment) {
                checkRestrictions()
                navController.popBackStack(R.id.authentication_fragment, true)
                navController.navigate(
                    R.id.main_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
                val groupChatID = intent.getStringExtra("groupChatId") ?: ""
                checkAndOpenChat(groupChatId = groupChatID)
            }
        }

        userViewModel.chamberID.observe(this) {
            if(!it.isNullOrBlank()) {
                if(navController.currentDestination?.id == R.id.chat_fragment) {
                    navController.popBackStack()
                }
                navController.navigate(
                    R.id.chat_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                            popEnter = R.anim.slide_in
                            popExit = R.anim.slide_out
                        }
                    }
                )
            } else {
                if (navController.currentDestination?.id == R.id.chat_fragment) {
                   navController.popBackStack()
                }
            }
        }

        userViewModel.pendingTopics.observe(this) {
            if (it != null && it.size > 0) {
                for (topic in it) {
                    if(topic.isNotBlank()) {
                        firestore
                            .collection("TopicIds")
                            .document(topic)
                            .get()
                            .addOnSuccessListener { topicDocSnapshot ->
                                userViewModel.pendingTopicTitles[topic] =
                                    topicDocSnapshot["TopicTitle"] as String? ?: ""
                            }
                    }
                }
            }
        }

        userViewModel.completedMatches.observe(this) {
            if(it.first.isNotBlank() && it.second.isNotBlank()) {
                showNewMatchDialog(it.first, it.second)
            }
        }

        setupJoinRequestsDialog()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            requestNotificationPermission()
        }
    startCheckUpNotificationService(this)
    }

    private fun checkAndOpenChat(groupChatId: String) {
        if(userViewModel.userState.value?.UID.isNullOrBlank()) {
            return
        }
        if (groupChatId.isNotBlank() && groupChatId != "nil") {
            userViewModel.openChamber(groupChatId)
        }
    }

    private fun requestNotificationPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if(!isGranted) {
                    showPermissionNotGrantedDialog()
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(
                this@MainActivity,
                "Grant notification permissions",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    private fun showNewMatchDialog(
        groupChatId: String,
        topicTitle: String
    ) {
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.dialog_match_successful)

        val topicTitleTextView = dialog.findViewById<TextView>(R.id.topicTitleTextView)
        val openChatButton = dialog.findViewById<Button>(R.id.openChatButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        topicTitleTextView.text = "Chamber topic: \"$topicTitle\""
        openChatButton.setOnClickListener {
            userViewModel.openChamber(groupChatId)
            dialog.dismiss()
        }
        cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showPermissionNotGrantedDialog() {
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.dialog_leave_chamber)
    }

    private fun checkRestrictions() {
        val uid = userViewModel.userState.value!!.UID
        val userRef = firestore.collection("Accounts").document(uid)
        userRef.update(mapOf(Pair("timestamp", FieldValue.serverTimestamp())))
        userRef.addSnapshotListener { snapshot, _ ->
            if(snapshot != null && snapshot.exists()) {
                val currentTime = snapshot.getTimestamp("timestamp")
                val restrictionRef = firestore.collection("Restrictions").document(uid)
                restrictionRef.addSnapshotListener { value, _ ->
                    if(value != null && value.exists()) {
                        val restrictedUntil = value.getTimestamp("restrictedUntil")
                        isUserRestricted = if(restrictedUntil != null && currentTime != null) {
                            restrictedUntil > currentTime
                        } else {
                            false
                        }
                    }
                }
            }
        }
    }

    private fun setupJoinRequestsDialog() {
        val topicJoinRequestDialog = Dialog(this, R.style.Dialog)
        topicJoinRequestDialog.setContentView(R.layout.topic_join_dialog)
        topicJoinRequestDialog.setCancelable(false)
        topicJoinRequestDialog.setCanceledOnTouchOutside(false)

        val topicsAdapter = TopicRequestRecyclerViewAdapter(
            acceptRequest = { userViewModel.acceptMatch(it) },
            denyRequest = { userViewModel.denyMatch(it) }
        )

        val topicsRecyclerView =
            topicJoinRequestDialog.findViewById<RecyclerView>(R.id.topic_requests)
        topicsRecyclerView.adapter = topicsAdapter

        val layoutManager = LinearLayoutManager(this)
        topicsRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            this,
            layoutManager.orientation
        )
        topicsRecyclerView.addItemDecoration(dividerItemDecoration)

        userViewModel.matches.observe(this) {
            if (it.size > 0) {
                topicsAdapter.updateList(it)
                if (!isShowingJoinDialog) {
                    topicJoinRequestDialog.show()
                    isShowingJoinDialog = true
                }
            } else {
                if(isShowingJoinDialog) {
                    topicJoinRequestDialog.dismiss()
                    isShowingJoinDialog = false
                }
                topicsAdapter.clear()
            }
        }
    }

//    private fun checkUserRating() {
//        val uid = userViewModel.userState.value!!.UID
//        val isRestricted = userViewModel.userState.value!!.isRestricted
//        firestore.collection("StarReviews")
//            .whereEqualTo("To", uid)
//            .orderBy("timestamp", Query.Direction.DESCENDING)
//            .limit(1)
//            .get()
//            .addOnSuccessListener { documents ->
//                if(!documents.isEmpty) {
//                    val review = documents.documents[0]
//                    val userRating = review.getDouble("AverageStars")!!
//                    val ratingsCount = review.getLong("ReviewsCount")!!.toInt()
//
//                    if(shouldBanUser(userRating = userRating, ratingsCount = ratingsCount)) {
//                        // Ban user
//                        userViewModel.logEventToAnalytics("star_reviews_auto_restriction")
//                        if(!isRestricted) {
//                            userViewModel.restrictUser(true)
//                            banUser(uid)
//                        }
//                    } else if(isRestricted){
//                        unbanUser(uid)
//                    }
//                }
//            }
//    }

//    private fun shouldBanUser(userRating: Double, ratingsCount: Int): Boolean {
//        return userRating <= 3.5 && ratingsCount >= 6
//    }
//
//    private fun banUser(uid: String) {
//        val topicsList =
//            sharedPreferences.getString("topics", "")!!.split(",")
//                    as MutableList<String>
//
//        for(topic in topicsList) {
//
//        }
//    }
//
//    private fun unbanUser(uid: String) {
//
//    }

    private fun startCheckUpNotificationService(context: Context) {
        val backgroundTaskState:Boolean = sharedPreferences.getBoolean("checkUpNotification",false)
        if(backgroundTaskState) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, CheckUpNotification::class.java)
        val pendingIntent =    PendingIntent.getBroadcast(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )



        if(hasScheduleExactAlarmPermission(context)){
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent)
            Log.d("AlarmManagerPermission","Success")
        }else{
            Log.d("AlarmManagerPermission","Denied")

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