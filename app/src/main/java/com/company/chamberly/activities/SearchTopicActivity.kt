package com.company.chamberly.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.company.chamberly.models.Topic
import com.company.chamberly.R
import com.company.chamberly.adapters.TopicAdapter
import com.company.chamberly.logEvent
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yalantis.library.Koloda
import com.yalantis.library.KolodaListener
import kotlin.math.absoluteValue

class SearchTopicActivity: ComponentActivity(), KolodaListener {
    private val auth = Firebase.auth
    private var currentUID: String = ""
    private var displayName: String = ""
    private lateinit var koloda: Koloda
    private lateinit var adapter: TopicAdapter
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var sharedPreferences: SharedPreferences
    private val database = Firebase.database
    private val firestore = Firebase.firestore
    private var isFirstTimeEmpty = true
    private var lastTimestamp: Any? = null
    private var procrastinatingTopics: MutableList<String> = mutableListOf()
    private var isListener: Boolean = false
    private var notificationKey: String = ""
    private val blockedUsers: MutableList<String> = mutableListOf()
    private var maxAllowedTopics: Long = 25

    override fun onCardDrag(position: Int, cardView: View, progress: Float) {
        val rightSwipeOverlay = cardView.findViewById<LinearLayout>(R.id.rightSwipeOverlay)
        val leftSwipeOverlay = cardView.findViewById<LinearLayout>(R.id.leftSwipeOverlay)
        if(progress.absoluteValue <= 0.05f) {
            rightSwipeOverlay.visibility = View.GONE
            leftSwipeOverlay.visibility = View.GONE
        } else if(progress < 0f) {
            rightSwipeOverlay.visibility = View.GONE
            leftSwipeOverlay.visibility = View.VISIBLE
        } else if (progress > 0f){
            rightSwipeOverlay.visibility = View.VISIBLE
            leftSwipeOverlay.visibility = View.GONE
        } else {
            rightSwipeOverlay.visibility = View.GONE
            leftSwipeOverlay.visibility = View.GONE
        }

        super.onCardDrag(position, cardView, progress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_topic)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        koloda = findViewById(R.id.koloda)
        koloda.kolodaListener = this

        sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        isListener = sharedPreferences.getBoolean("isListener", false)
        procrastinatingTopics = sharedPreferences.getString("topics", "")!!
            .split(",")
            .toMutableList()
        if (procrastinatingTopics[0] == "") {
            procrastinatingTopics.removeAt(0)
        }
        notificationKey = sharedPreferences.getString("notificationKey", "") ?: ""
        currentUID = sharedPreferences.getString("uid", auth.currentUser!!.uid) ?: ""
        displayName = sharedPreferences.getString("displayName", "Anonymous") ?: ""

        database.reference
            .child("UX_Android")
            .child("pendingChambersNotSubbedLimit")
            .get()
            .addOnSuccessListener {
                maxAllowedTopics = (it.value as Long?) ?: 25
            }

        // POSSIBLE REDUNDANT EVENTS
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "chamber_search",
            params = hashMapOf(
                "UID" to currentUID,
                "name" to displayName
            )
        )

        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "landed_on_cards_view",
            params = hashMapOf(
                "UID" to currentUID,
                "name" to displayName
            )
        )

        firestore
            .collection("Accounts")
            .document(currentUID)
            .get()
            .addOnSuccessListener {
                blockedUsers += it["blockedUsers"] as List<String>? ?: emptyList()
            }

        if(procrastinatingTopics.size >= maxAllowedTopics) {
            showTooManyTopicsDialog()
        }

        adapter = TopicAdapter()
        koloda.adapter = adapter

        val dismissButton: ImageButton = findViewById(R.id.ic_skip)
        val joinButton: ImageButton = findViewById(R.id.ic_chat)

        dismissButton.setOnClickListener {
            // POSSIBLE REDUNDANT EVENTS
            logEvent(
                firebaseAnalytics = firebaseAnalytics,
                eventName = "swiped_left",
                params = hashMapOf(
                    "UID" to currentUID,
                    "name" to displayName
                )
            )
            logEvent(
                firebaseAnalytics = firebaseAnalytics,
                eventName = "swiped_on_card",
                params = hashMapOf(
                    "UID" to currentUID,
                    "name" to displayName
                )
            )
            koloda.onClickLeft()
        }

        joinButton.setOnClickListener {
            // POSSIBLE REDUNDANT EVENTS
            logEvent(
                firebaseAnalytics = firebaseAnalytics,
                eventName = "swiped_right_${if(isListener) "listener" else "ventor"}",
                params = hashMapOf(
                    "UID" to currentUID,
                    "name" to displayName
                )
            )
            logEvent(
                firebaseAnalytics = firebaseAnalytics,
                eventName = "swiped_on_card",
                params = hashMapOf(
                    "UID" to currentUID,
                    "name" to displayName
                )
            )
            logEvent(
                firebaseAnalytics = firebaseAnalytics,
                eventName = "Started_Procrastinating",
                params = hashMapOf(
                    "UID" to currentUID,
                    "name" to displayName
                )
            )
            koloda.onClickRight()
        }

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchTopics() {

        val query: Query = if (lastTimestamp == null) {
            firestore.collection("TopicIds")
                .orderBy(if(isListener) "lflWeight" else "lfvWeight", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(8)
        } else {
            firestore.collection("TopicIds")
                .orderBy(if(isListener) "lflWeight" else "lfvWeight", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(8)
                .startAfter(lastTimestamp)
        }
        fetchTopicsRecursively(query)
    }

    private fun fetchTopicsRecursively(query: Query) {
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val savedTopic = sharedPreferences.getString("topics", "")!!.split(",")
        query.get()
            .addOnSuccessListener { querySnapshot ->
                for (documentSnapshot in querySnapshot) {
                    val topic = documentSnapshot.toObject(Topic::class.java).copy(TopicID = documentSnapshot.id)
                    if (topic.TopicID !in savedTopic) {
                        adapter.setData(topic)
                    }
                }

                val lastDocument = querySnapshot.documents.lastOrNull()
                lastTimestamp = lastDocument?.get("timestamp")
                if (adapter.count == 0) {
                    val kolodaView = findViewById<com.yalantis.library.Koloda>(R.id.koloda)
                    val buttonsView = findViewById<LinearLayout>(R.id.buttonsLayout)
                    val emptyStateView = findViewById<RelativeLayout>(R.id.emptyStateView)
                    kolodaView.visibility = View.GONE
                    buttonsView.visibility = View.GONE
                    emptyStateView.visibility = View.VISIBLE
                } else {
                    val kolodaView = findViewById<com.yalantis.library.Koloda>(R.id.koloda)
                    val buttonsView = findViewById<LinearLayout>(R.id.buttonsLayout)
                    val emptyStateView = findViewById<RelativeLayout>(R.id.emptyStateView)
                    kolodaView.visibility = View.VISIBLE
                    buttonsView.visibility = View.VISIBLE
                    emptyStateView.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Log.e("SearchTopicActivity", "Error fetching topics: $it")
            }
    }


    override fun onCardSwipedRight(position: Int) {
        val topic = adapter.getItem(position + 1)
        val topicID = topic.TopicID
        procrastinatingTopics.add(topicID)
        // POSSIBLE REDUNDANT EVENTS
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "swiped_right_${if(isListener) "listener" else "ventor"}",
            params = hashMapOf(
                "UID" to currentUID,
                "name" to displayName
            )
        )
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "swiped_on_card",
            params = hashMapOf(
                "UID" to currentUID,
                "name" to displayName
            )
        )
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "Started_Procrastinating",
            params = hashMapOf(
                "UID" to currentUID,
                "name" to displayName
            )
        )
        if(procrastinatingTopics.size > maxAllowedTopics) {
            showTooManyTopicsDialog()
        } else {
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val savedTopics = (sharedPreferences.getString("topics", "")!!.split(",") + topicID).joinToString(",")
            editor.putString("topics", savedTopics)
            editor.apply()

            val topicRef = database.reference.child(topicID)
            val workerRef = topicRef.child("users").child(currentUID)
            workerRef.child("isAndroid").setValue(true)
            workerRef.child("isReserved").setValue(false)
            workerRef.child("isSubscribed").setValue(true)
            workerRef.child("notificationKey").setValue("")
            workerRef.child("lfl").setValue(!isListener)
            workerRef.child("lfv").setValue(isListener)
            workerRef.child("penalty").setValue(0)
            workerRef.child("timestamp").setValue(System.currentTimeMillis() / 1000)
        }
        super.onCardSwipedRight(position)
    }

    override fun onCardSwipedLeft(position: Int) {
        super.onCardSwipedLeft(position)
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "swiped_left",
            params = hashMapOf(
                "UID" to currentUID,
                "name" to displayName
            )
        )
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = "swiped_on_card",
            params = hashMapOf(
                "UID" to currentUID,
                "name" to displayName
            )
        )
    }


    override fun onEmptyDeck() {
        if(isFirstTimeEmpty) {
            isFirstTimeEmpty = false
        } else {
            fetchTopics()
        }
        super.onEmptyDeck()
    }

    private fun showTooManyTopicsDialog() {
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.cancel_procrastination_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        val confirmButton = dialog.findViewById<Button>(R.id.cancelProcrastinationButton)
        val dismissButton = dialog.findViewById<Button>(R.id.dismissDialogButton)
        val loadingIndicator = dialog.findViewById<CircularProgressIndicator>(R.id.loading_indicator)

        confirmButton.setOnClickListener {
            confirmButton.isEnabled = false
            loadingIndicator.visibility = View.VISIBLE
            dismissButton.isEnabled = false
            cancelProcrastinationOnAllTopics(dialog)
        }

        dismissButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }
    }

    private fun cancelProcrastinationOnAllTopics(dialog: Dialog) {
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val uid = sharedPreferences.getString("uid", "") ?: auth.currentUser!!.uid
        for(topic in procrastinatingTopics) {
            database.reference.child(topic).child("users").child(uid).removeValue()
        }
        procrastinatingTopics.clear()
        val editor = sharedPreferences.edit()
        editor.remove("topics")
        editor.apply()
        dialog.dismiss()
    }
}