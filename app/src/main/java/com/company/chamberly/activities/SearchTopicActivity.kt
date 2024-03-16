package com.company.chamberly.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.company.chamberly.models.Topic
import com.company.chamberly.R
import com.company.chamberly.adapters.TopicAdapter
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

class SearchTopicActivity: ComponentActivity(), KolodaListener {
    private val auth = Firebase.auth
    private val currentUser = auth.currentUser
    private lateinit var koloda: Koloda
    private lateinit var adapter: TopicAdapter
    private val database = Firebase.database
    private val firestore = Firebase.firestore
    private var isFirstTimeEmpty = true
    private var lastTimestamp: Any? = null

    override fun onCardDrag(position: Int, cardView: View, progress: Float) {
        val rightSwipeOverlay = cardView.findViewById<TextView>(R.id.rightSwipeOverlay)
        val leftSwipeOverlay = cardView.findViewById<TextView>(R.id.leftSwipeOverlay)

        if(progress == 0f) {
            rightSwipeOverlay.visibility = View.INVISIBLE
            leftSwipeOverlay.visibility = View.VISIBLE
        } else if(progress < 0f) {
            rightSwipeOverlay.visibility = View.INVISIBLE
            leftSwipeOverlay.visibility = View.VISIBLE
        } else if (progress > 0f){
            rightSwipeOverlay.visibility = View.VISIBLE
            leftSwipeOverlay.visibility = View.INVISIBLE
        } else {
            rightSwipeOverlay.visibility = View.INVISIBLE
            leftSwipeOverlay.visibility = View.INVISIBLE
        }

        super.onCardDrag(position, cardView, progress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_topic)
        koloda = findViewById(R.id.koloda)
        koloda.kolodaListener = this

        adapter = TopicAdapter()
        koloda.adapter = adapter

        val dismissButton: ImageButton = findViewById(R.id.ic_skip)
        val joinButton: ImageButton = findViewById(R.id.ic_chat)

        dismissButton.setOnClickListener {
            koloda.onClickLeft()
        }

        joinButton.setOnClickListener {
            koloda.onClickRight()
        }

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchChambers() {
        val query: Query = if (lastTimestamp == null) {
            firestore.collection("TopicIds")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(4)
        } else {
            firestore.collection("TopicIds")
                .orderBy("timestamp", Query.Direction.ASCENDING)
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
        val currentUserID = currentUser!!.uid
        val currentUserName = currentUser.displayName
        val topicID = topic.TopicID
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val savedTopics = (sharedPreferences.getString("topics", "")!!.split(",") + topicID).joinToString(",")
        editor.putString("topics", savedTopics)
        editor.apply()

//        sharedPreferences.w
        Log.d("TOPICSWIPE", topicID)
        val topicRef = database.reference.child(topicID)
        val workerRef = topicRef.child("users").child(currentUserID)
        workerRef.child("isReserved").setValue(false)
        workerRef.child("isSubscribed").setValue(true)
        workerRef.child("notificationKey").setValue("")
        workerRef.child("penalty").setValue(0)
        workerRef.child("timestamp").setValue(System.currentTimeMillis() / 1000)
        topicRef.child("users").child(currentUserID)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isReserved = snapshot.child("isReserved").value as Boolean? ?: return
                    if (isReserved) {
                        val intent = Intent(this@SearchTopicActivity, TopicJoinRequestActivity::class.java)
                        intent.putExtra("TopicTitle", topic.TopicTitle)
                        intent.putExtra("TopicID", topicID)
                        intent.putExtra("AuthorUID", currentUserID)
                        intent.putExtra("AuthorName", currentUserName)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
//        val authorRef = topicRef.child("users").child(AuthorUID)
//        authorRef.updateChildren(mapOf(
//            "isReady" to false,
//            "isReserved" to false,
//            "reservedBy" to currentUserID
//        ))
//        createChamber(topic, currentUserID)
//        sendNotification(currentUserID, AuthorUID)
        super.onCardSwipedRight(position)
    }


//    private fun createChamber(topic: Topic, currentUserID: String) {
//        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
//        val AuthorName = sharedPreferences.getString("displayName", "Anonymous")
//        val chamber = Chamber(
//            AuthorName = topic.AuthorName,
//            AuthorUID = topic.AuthorUID,
//            groupTitle = topic.TopicTitle
//        )
//        val documentRef = firestore.collection("GroupChatIds").document()
//        chamber.groupChatId = documentRef.id
//
//        documentRef.set(chamber)
//            .addOnSuccessListener {
//                val realtimeDb = FirebaseDatabase.getInstance()
//                val chamberDataRef = realtimeDb.getReference(chamber.groupChatId)
//                documentRef.update("members", listOf(currentUserID, topic.AuthorUID))
//                chamberDataRef.child("Host").setValue(chamber.AuthorUID)
//                chamberDataRef.child("Users").child("members").child(currentUserID).setValue(AuthorName)
//                    .addOnSuccessListener {
//                        documentRef.update("locked", true)
//                            .addOnSuccessListener {
//                                val intent = Intent(this@SearchTopicActivity, ChatActivity::class.java)
//                                intent.putExtra("groupChatId", chamber.groupChatId)
//                                intent.putExtra("groupTitle", chamber.groupTitle)
//                                intent.putExtra("AuthorName", chamber.AuthorName)
//                                intent.putExtra("AuthorUID", chamber.AuthorUID)
//                                startActivity(intent)
//                                finish()
//
//                                val userRef = firestore.collection("Users").document(currentUserID)
//                                userRef.update("chambers", FieldValue.arrayUnion(chamber.groupChatId))
//                                val authorRef = firestore.collection("Users").document(topic.AuthorUID)
//                                authorRef.update("chambers", FieldValue.arrayUnion(chamber.groupChatId))
//                            }
//                    }
//            }
//    }



    override fun onEmptyDeck() {
        if(isFirstTimeEmpty) {
            isFirstTimeEmpty = false
        } else {
            fetchChambers()
        }
        super.onEmptyDeck()
    }

}