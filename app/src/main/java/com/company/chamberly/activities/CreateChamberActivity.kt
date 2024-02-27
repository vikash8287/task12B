package com.company.chamberly.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.company.chamberly.models.Chamber
import com.company.chamberly.R
import com.company.chamberly.models.chamberToMap
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateChamberActivity : ComponentActivity() {
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private val firestore = Firebase.firestore      // firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chamber)

        val currentUser = auth.currentUser

        val editText = findViewById<EditText>(R.id.chamber_title)
        val createButton = findViewById<Button>(R.id.create_button)


        // Limit the length of the title
        val maxLength = 50
        val filterArray = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        editText.filters = filterArray

        createButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val authorUID = sharedPreferences.getString("uid", currentUser?.uid)
            val authorName = sharedPreferences.getString("displayName", "NONE")
            val title = editText.text.toString()
            if (title.isEmpty()){
                editText.error = "Please enter a title"
            } else {
                //Toast.makeText(this, AuthorName, Toast.LENGTH_SHORT).show()
                val chamber = Chamber(
                    AuthorName = authorName ?: "",
                    AuthorUID = authorUID ?: "",
                    groupTitle = title
                )

                val collectionRef = database.collection("GroupChatIds")
                val documentRef = collectionRef.document() // generate a random document ID
                chamber.groupChatId = documentRef.id // set the document ID to the random ID

                documentRef.set(chamberToMap(chamber = chamber))
                    .addOnSuccessListener {
                        // Save additional data to Realtime Database
                        val realtimeDb = FirebaseDatabase.getInstance()
                        val chamberDataRef = realtimeDb.getReference(chamber.groupChatId)

                        // Set "Host" data
                        chamberDataRef.child("host").setValue(chamber.AuthorUID)

                        // Set empty "messages" child key
                        chamberDataRef.child("messages").push().setValue("")

                        // Set "timestamp" data
                        val timestamp = System.currentTimeMillis() / 1000 // Convert to seconds
                        chamberDataRef.child("timestamp").setValue(timestamp)


                        firestore.collection("GroupChatIds").document(chamber.groupChatId)
                            .update("members", FieldValue.arrayUnion(authorUID))

                        // Set "Title" data
                        chamberDataRef.child("title").setValue(chamber.groupTitle)

                        val userRef = firestore.collection("users").document(authorUID!!)
                        userRef.update("chambers", FieldValue.arrayUnion(chamber.groupChatId))
                            .addOnSuccessListener {
                                // Handle success
                            }
                            .addOnFailureListener {
                                // Handle error
                            }

                        // Set "Users" data
                        val usersRef = chamberDataRef.child("users")
                        val membersRef = usersRef.child("members")
                        val hostRef = membersRef.child(chamber.AuthorUID)
                        hostRef.setValue(authorName).addOnSuccessListener {
                            val intent = Intent(this@CreateChamberActivity, ChatActivity::class.java)
                            //TODO : pass chamber object to ChatActivity
                            //intent.putExtra("chamber", chamber)
                            intent.putExtra("GroupChatId", chamber.groupChatId)
                            intent.putExtra("GroupTitle", chamber.groupTitle)
                            intent.putExtra("AuthorName",chamber.AuthorName)
                            intent.putExtra("AuthorUID",chamber.AuthorUID)
                            startActivity(intent)
                            finish()
                        }
                        //Toast.makeText(this, "Chamber created: $chamber", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error creating chamber: $e", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@CreateChamberActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }
    override fun onDestroy() {
        onBackPressedCallback.remove()
        super.onDestroy()
    }

}