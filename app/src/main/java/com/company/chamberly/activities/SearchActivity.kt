package com.company.chamberly.activities

import android.content.ContentValues.TAG
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
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ContentInfoCompat.Flags
import com.company.chamberly.models.Chamber
import com.company.chamberly.models.Message
import com.company.chamberly.R
import com.company.chamberly.adapters.ChamberAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yalantis.library.Koloda
import com.yalantis.library.KolodaListener


// Firebase lock system: block other users from joining a chamber

class SearchActivity : ComponentActivity() ,KolodaListener{
    private val auth = Firebase.auth
    private val currentUser = auth.currentUser
    private lateinit var koloda: Koloda
    private lateinit var adapter: ChamberAdapter
    private val database = Firebase.database// realtime database
    private val firestore = Firebase.firestore
    private var isFirstTimeEmpty = true
    private var lastTimestamp: Any? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback


    override fun onCardDrag(position: Int, cardView: View, progress: Float) {
        // Get references to the overlay views
        val rightSwipeOverlay = cardView.findViewById<LinearLayout>(R.id.rightSwipeOverlay)
        val leftSwipeOverlay = cardView.findViewById<LinearLayout>(R.id.leftSwipeOverlay)

        // Based on the progress float, determine the visibility of the overlays
        if(progress==0f){
            rightSwipeOverlay.visibility= View.GONE
            leftSwipeOverlay.visibility=View.GONE
        }
        else if (progress > 0) { // Assuming positive progress indicates a right swipe
            rightSwipeOverlay.visibility = View.VISIBLE
            leftSwipeOverlay.visibility = View.GONE
        } else if (progress < 0) { // Assuming negative progress indicates a left swipe
            rightSwipeOverlay.visibility = View.GONE
            leftSwipeOverlay.visibility = View.VISIBLE
        } else { // No significant swipe, hide both overlays
            rightSwipeOverlay.visibility = View.GONE
            leftSwipeOverlay.visibility = View.GONE
        }

        super.onCardDrag(position, cardView, progress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        koloda = findViewById(R.id.koloda)
        koloda.kolodaListener = this

        // set adapter
        adapter = ChamberAdapter()
        koloda.adapter =  adapter


        // fetch now data from firestore onCreate
        //fetchChambers()

        val dislikeButton: ImageButton = findViewById(R.id.ic_skip)
        val likeButton: ImageButton = findViewById(R.id.ic_chat)


        dislikeButton.setOnClickListener {
            koloda.onClickLeft()
        }

        likeButton.setOnClickListener {
            koloda.onClickRight()
        }

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            // Explicitly start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }


        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                releaseCards()
                val intent = Intent(this@SearchActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }

    // Check from real-time database
    private fun isVacant(chamber: Chamber, callback: (Boolean) -> Unit) {
        val uid = getSharedPreferences("cache", Context.MODE_PRIVATE).getString("uid", currentUser?.uid)
        callback(chamber.membersLimit >= chamber.members.size && !chamber.members.contains(uid))
    }

    private fun fetchChambers() {
        val query: Query = if (lastTimestamp == null) {
            firestore.collection("GroupChatIds")
                .whereEqualTo("isLocked", false)
                .whereEqualTo("publishedPool", true)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(4)
        } else {
            // fetch next 4 chambers
            Log.e(TAG, "New FetchChambers: last document is at $lastTimestamp")
            firestore.collection("GroupChatIds")
                .whereEqualTo("isLocked", false)
                .whereEqualTo("publishedPool", true)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .startAfter(lastTimestamp)
                .limit(4)
        }

        fetchChambersRecursively(query)
    }

    private fun fetchChambersRecursively(query: Query) {
        val kolodaView = findViewById<Koloda>(R.id.koloda)
        val buttonsView = findViewById<LinearLayout>(R.id.buttonsLayout)
        val emptyStateView = findViewById<RelativeLayout>(R.id.emptyStateView)
        kolodaView.visibility = View.GONE
        buttonsView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        query.get()
            .addOnSuccessListener { querySnapshot ->
                for (documentSnapshot in querySnapshot) {
                    val chamber = documentSnapshot.toObject(Chamber::class.java)
                    kolodaView.visibility = View.VISIBLE
                    buttonsView.visibility = View.VISIBLE
                    emptyStateView.visibility = View.GONE
                    // set published pool as false to locked this chamber
                    firestore.collection("GroupChatIds").document(chamber.groupChatId)
                        .update("publishedPool", false)
                    isVacant(chamber) { isVacant ->
                        if (isVacant) {
                            adapter.setData(chamber)
                        }
                        else{
                            // reset publishedPool as true again
                            firestore.collection("GroupChatIds").document(chamber.groupChatId)
                                .update("publishedPool", true)
                        }
                    }
                }
                val lastDocument = querySnapshot.documents.lastOrNull()
                lastTimestamp = lastDocument?.get("timestamp")
            }
            .addOnFailureListener { exception ->
                Log.e("SearchActivity", "Error fetching chambers: $exception")
            }
    }

    // override koloda listener
    override fun onCardSwipedLeft(position: Int) {
        val chamber = adapter.getItem(position+1)

        firestore.collection("GroupChatIds").document(chamber.groupChatId)
            .update("publishedPool", true)

        // Call the super implementation if needed
        super.onCardSwipedLeft(position)
    }
    override fun onCardSwipedRight(position: Int) {

        val chamber = adapter.getItem(position+1)
        isVacant(chamber) { isVacant ->
            if (isVacant) {
                // add user to Chat
                joinChat(chamber)
            }
        }
        // Call the super implementation if needed
        super.onCardSwipedRight(position)
    }

    override fun onClickLeft(position: Int)  {
        val chamber = adapter.getItem(position+1)

        firestore.collection("GroupChatIds").document(chamber.groupChatId)
            .update("publishedPool", true)
        // Call the super implementation if needed
        super.onClickLeft(position)
    }

    override fun onClickRight(position: Int)  {
        //Log.e("SearchActivity", "Card swiped right at position: $position")
        val chamber = adapter.getItem(position+1)
        isVacant(chamber) { isVacant ->
            if (isVacant) {
                // add user to Chat
                joinChat(chamber)
            }
        }
        // Call the super implementation if needed
        super.onClickRight(position)
    }
    override fun onEmptyDeck() {
        if (isFirstTimeEmpty) {
            isFirstTimeEmpty = false
        } else {
            releaseCards()
            fetchChambers()
        }

        // Call the super implementation if needed
        super.onEmptyDeck()
    }

    // When exit the activity, set publishedPool as true again
    fun releaseCards(){
        for(chamber in adapter.dataList){
            firestore.collection("GroupChatIds").document(chamber.groupChatId)
                .update("publishedPool", true)
        }
    }

    private fun joinChat(chamber: Chamber){
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val authorUID = sharedPreferences.getString("uid", currentUser?.uid)
        val authorName = sharedPreferences.getString("displayName", "Anonymous")
        val chamberDataRef = database.reference.child(chamber.groupChatId)

        // New system message for user join
        val systemMessage1 = Message("Chamberly", "$authorName joined the chat. \n\nPlease be patient for others to respond :)", "system", "Chamberly")

        firestore.collection("GroupChatIds").document(chamber.groupChatId)
            .update("locked", true, "members", FieldValue.arrayUnion(authorUID))

        // Add system message to messages in the database
        chamberDataRef.child("messages").push().setValue(systemMessage1)
            .addOnSuccessListener {
                // Add user to members
                if (authorUID != null) {
                    chamberDataRef.child("users").child("members").child(authorUID).child("name").setValue(authorName)
                        .addOnSuccessListener {
                            // Lock the chamber
                            firestore.collection("GroupChatIds").document(chamber.groupChatId).update("locked" , true)
                                .addOnSuccessListener{
                                    // Start ChatActivity
                                    val intent = Intent(this@SearchActivity, ChatActivity::class.java)
                                    intent.putExtra("GroupChatId", chamber.groupChatId)
                                    intent.putExtra("GroupTitle", chamber.groupTitle)
                                    intent.putExtra("Authorname",chamber.AuthorName)
                                    intent.putExtra("AuthorUID",chamber.AuthorUID)
                                    startActivity(intent)
                                    finish()

                                    // Update user document with the new chamber ID in Firestore
                                    val userRef = firestore.collection("users").document(authorUID)
                                    userRef.update("chambers",  FieldValue.arrayUnion(chamber.groupChatId))
                                        .addOnSuccessListener {
                                            // Successfully updated user's chamber list
                                        }
                                        .addOnFailureListener { e ->
                                            // Handle error in updating the chamber list
                                        }
                                }
                        }
                }
            }
    }




    override fun onDestroy() {
        releaseCards()
        onBackPressedCallback.remove()
        super.onDestroy()
    }

}

