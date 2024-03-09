package com.company.chamberly.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.company.chamberly.models.Topic
import com.company.chamberly.R
import com.company.chamberly.models.topicToMap
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateTopicActivity : ComponentActivity() {

    private val auth = Firebase.auth
    private val database = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_topic)

        val currentUser = auth.currentUser

        val editText = findViewById<EditText>(R.id.topic_title)
        val createButton = findViewById<Button>(R.id.create_button)

        val maxLength = 50
        val filterArray = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        editText.filters = filterArray


        createButton.setOnClickListener {
            createButton.isEnabled = false
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val currentUserUID = sharedPreferences.getString("uid", currentUser?.uid)
            val currentUserName = sharedPreferences.getString("displayName", "NONE")
            val topicTitle = editText.text.toString()
            val notificationKey = sharedPreferences.getString("notificationKey", "")
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

                documentRef.set(topicToMap(topic = topic))
                    .addOnSuccessListener {
                        val realtimeDb = FirebaseDatabase.getInstance()
                        val topicDataRef = realtimeDb.getReference(topic.TopicID)
                        topicDataRef.child("AuthorUID").setValue(topic.AuthorUID)
                        topicDataRef.child("AuthorName").setValue(topic.AuthorName)
                        topicDataRef.child("timestamp").setValue(System.currentTimeMillis() / 1000)
                        topicDataRef.child("TopicTitle").setValue(topic.TopicTitle)
                        val usersRef = topicDataRef.child("users")
                        val userRef = usersRef.child(topic.AuthorUID)
                        userRef.child("isReserved").setValue(false)
                        userRef.child("isSubbed").setValue(false)
                        userRef.child("restricted").setValue(false)
                        userRef.child("notificationKey").setValue(notificationKey)
                        userRef.child("penalty").setValue(0)
                        userRef.child("timestamp").setValue(System.currentTimeMillis() / 1000)
                        val editor = sharedPreferences.edit()
                        val savedTopics = sharedPreferences.getString("topics", "")!!.split(",").toMutableList()
                        Log.d("SAVEDTOPICs", savedTopics.toString())
                        if(!savedTopics.contains(topic.TopicID)) {
                            savedTopics.add(topic.TopicID)
                        }
                        Log.d("SAVEDTOPICs", savedTopics.joinToString(","))
                        editor.putString("topics", savedTopics.joinToString(","))
                        editor.apply()

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
}