package com.company.chamberly.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.company.chamberly.models.Topic
import com.company.chamberly.R
import com.company.chamberly.logEvent
import com.company.chamberly.models.toMap
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateTopicActivity : ComponentActivity() {

    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private val realtimeDb = FirebaseDatabase.getInstance()
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val analyticsBundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this@CreateTopicActivity)
        setContentView(R.layout.activity_create_topic)

        val currentUser = auth.currentUser!!

        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val currentUserUID = sharedPreferences.getString("uid", currentUser.uid)
        val currentUserName = sharedPreferences.getString("displayName", "NONE")

        val notificationKey = sharedPreferences.getString("notificationKey", "")
        val topicsList = sharedPreferences.getString("topics", "")?.split(",") ?: emptyList()
        val isListener = sharedPreferences.getBoolean("isListener", false)

        val blockedUsers = mutableListOf<String>()

        database
            .collection("Accounts")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener {
                blockedUsers += it["blockedUsers"] as List<String>? ?: emptyList()
            }

        val editText = findViewById<EditText>(R.id.topic_title)
        val createButton = findViewById<Button>(R.id.create_button)

        realtimeDb
            .reference
            .child("UX_Android")
            .child("pendingChambersNotSubbedLimit")
            .get()
            .addOnSuccessListener {
                val maxAllowedTopics = (it.value as Long?) ?: 25
                createButton.isEnabled = true
                if(topicsList.size > maxAllowedTopics) {
                    showTooManyTopicsMessage(topicsList, editText, createButton)
                }
            }


        val maxLength = 50
        val filterArray = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        editText.filters = filterArray

        createButton.setOnClickListener {
            createButton.isEnabled = false
            val topicTitle = editText.text.toString()
            if(topicTitle.isEmpty()) {
                editText.error = "Please enter a title"
            } else {
                val topic = Topic(
                    AuthorName = currentUserName ?: "",
                    AuthorUID = currentUserUID ?: "",
                    TopicTitle = topicTitle,
                    weight = 60,
                )

                val collectionRef = database.collection("TopicIds")
                val documentRef = collectionRef.document()
                topic.TopicID = documentRef.id

                documentRef.set(topic.toMap())
                    .addOnSuccessListener {
                        val topicDataRef = realtimeDb.getReference(topic.TopicID)
                        topicDataRef.child("authorUID").setValue(topic.AuthorUID)
                        topicDataRef.child("authorName").setValue(topic.AuthorName)
                        topicDataRef.child("timestamp").setValue(System.currentTimeMillis() / 1000)
                        topicDataRef.child("topicTitle").setValue(topic.TopicTitle)
                        val usersRef = topicDataRef.child("users")
                        val userRef = usersRef.child(topic.AuthorUID)
                        userRef.child("isReserved").setValue(false)
                        userRef.child("lfl").setValue(!isListener) //Listener won't look for listeners
                        userRef.child("lfv").setValue(isListener) //Venter will look for listeners
                        userRef.child("isAndroid").setValue(true)
                        userRef.child("isSubbed").setValue(false)
                        userRef.child("restricted").setValue(false)
                        userRef.child("notificationKey").setValue(notificationKey)
                        userRef.child("penalty").setValue(0)
                        userRef.child("timestamp").setValue(System.currentTimeMillis() / 1000)
                        userRef.child("blockedUsers").setValue(blockedUsers as List<String>)

                        val savedTopics = topicsList.toMutableList()
                        if(!savedTopics.contains(topic.TopicID)) {
                            savedTopics.add(topic.TopicID)
                        }
                        editor.putString("topics", savedTopics.joinToString(","))
                        editor.apply()

                        logEvent(
                            firebaseAnalytics = firebaseAnalytics,
                            eventName = "topic_started",
                            params = hashMapOf(
                                "uid" to currentUserUID!!,
                                "name" to currentUserName!!
                            )
                        )

                        logEvent(
                            firebaseAnalytics = firebaseAnalytics,
                            eventName = "Started_Procrastinating",
                            params = hashMapOf(
                                "uid" to currentUserUID,
                                "name" to currentUserName,
                            )
                        )

                        Toast.makeText(this@CreateTopicActivity, "Topic created successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@CreateTopicActivity, MainActivity::class.java)
                        startActivity(intent)

                    }
                    .addOnFailureListener {  e ->
                        Toast.makeText(this@CreateTopicActivity, "Error creating topic: $topicTitle $e", Toast.LENGTH_SHORT).show()
                        createButton.isEnabled = false
                    }
            }
        }
    }


    private fun showTooManyTopicsMessage(
        topicsList: List<String>,
        editText: EditText,
        createButton: Button
    ) {
        createButton.isEnabled = false
        editText.isEnabled = false
        val tooManyTopicsView = findViewById<LinearLayout>(R.id.too_many_topics_layout)
        val clearButton = tooManyTopicsView.findViewById<Button>(R.id.clear_all_topics_button)
        tooManyTopicsView.visibility = View.VISIBLE

        clearButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("topics")
            editor.apply()
            for(topic in topicsList) {
                if(topic.isBlank()) {
                    continue
                }
                val topicRef = realtimeDb.getReference(topic)
                topicRef.child("users").removeValue()
            }
            tooManyTopicsView.visibility = View.GONE
            createButton.isEnabled = true
            editText.isEnabled = true
        }
    }
}