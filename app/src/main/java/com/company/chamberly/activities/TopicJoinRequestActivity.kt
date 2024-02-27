package com.company.chamberly.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.company.chamberly.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class TopicJoinRequestActivity: ComponentActivity() {

    private val realtimeDb = Firebase.database
    private val firestore = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_join_request)

        val topicId = intent.getStringExtra("TopicID") ?: ""
        val topicTitle = intent.getStringExtra("TopicTitle") ?: ""
        val authorUID = intent.getStringExtra("AuthorUID") ?: ""
        val authorName = intent.getStringExtra("AuthorName") ?: ""

        val joinerInfoText = findViewById<TextView>(R.id.joiner_info)
        val acceptButton = findViewById<Button>(R.id.accept_button)
        val denyButton = findViewById<Button>(R.id.deny_button)

        var reservedBy: String
        realtimeDb.reference.child(topicId)
            .child("users")
            .child(authorUID)
            .child("reservedBy")
            .get()
            .addOnSuccessListener {
                reservedBy = it.value.toString()
                firestore.collection("Accounts").document(reservedBy).get().addOnSuccessListener {userSnapshot ->
                    val name = userSnapshot.data?.get("Display_Name") ?: "Anonymous"
                    joinerInfoText.text =
                        getString(R.string.topic_join_request_message, name, topicTitle)
                }
            acceptButton.setOnClickListener {
                val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                val savedTopics = sharedPreferences.getString("topics", "")!!.split(",").toMutableList()
                savedTopics.remove(topicId)
                editor.putString("topics", savedTopics.joinToString(","))
                editor.apply()
                acceptRequestListener(
                    joinerUID = reservedBy,
                    authorUID = authorUID,
                    authorName = authorName,
                    groupTitle = topicTitle,
                    topicId = topicId,
                )
            }
        }

        denyButton.setOnClickListener {
            realtimeDb.reference.child(topicId).child("users").child(authorUID).child("isReserved").setValue(false)
            realtimeDb.reference.child(topicId).child("users").child(authorUID).child("reservedBy").removeValue()
            goToMainActivity()
        }

    }

    private fun acceptRequestListener(joinerUID: String, authorUID: String, authorName: String, groupTitle: String, topicId: String) {
        val topicRef = realtimeDb.reference.child(topicId)

        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("displayName", "Anonymous")
        topicRef.child("users").child(authorUID).child("isReady").setValue(true)
        topicRef.child("users").child(joinerUID).child("groupChatId").setValue("")
        topicRef.child("users").child(joinerUID).child("groupTitle").setValue(groupTitle)
        topicRef.child("users").child(joinerUID).child("groupChatId").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupChatId = snapshot.value as String?
                if(!groupChatId.isNullOrBlank()) {
                    Log.d("TopicJoin", "Chamber($groupChatId) successfully created for the topic $groupTitle")
                    val chamberDataRef = realtimeDb.reference.child(groupChatId)
                    topicRef.child("users").child(joinerUID).removeValue()
                    topicRef.child("users").child(authorUID).removeValue()

                    chamberDataRef.child("users").child("members").child(authorUID).child("name").setValue(authorName)

                    firestore.collection("GroupChatIds").document(groupChatId)
                        .update("locked", true, "members", FieldValue.arrayUnion(authorUID))

                    firestore.collection("GroupChatIds").document(groupChatId).update("locked", true)
                        .addOnSuccessListener {
                            val userRef = firestore.collection("users").document(authorUID)
                            userRef.update("chambers", FieldValue.arrayUnion(groupChatId))

                            val intent = Intent(this@TopicJoinRequestActivity, ChatActivity::class.java)
                            intent.putExtra("GroupChatId", groupChatId)
                            intent.putExtra("GroupTitle", groupTitle)
                            intent.putExtra("AuthorName", userName)
                            intent.putExtra("AuthorUID", authorUID)
                            startActivity(intent)
                            finish()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        val intent = Intent(this@TopicJoinRequestActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}