//package com.company.chamberly.activities
//
//import android.app.Dialog
//import android.content.ActivityNotFoundException
//import android.content.ClipData
//import android.content.ClipboardManager
//import android.content.Context
//import android.content.Intent
//import android.content.SharedPreferences
//import android.graphics.Color
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.Gravity
//import android.view.View
//import android.view.WindowManager
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageButton
//import android.widget.LinearLayout
//import android.widget.RatingBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.emoji2.emojipicker.EmojiPickerView
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.company.chamberly.OkHttpHandler
//import com.company.chamberly.R
//import com.company.chamberly.adapters.MessageAdapter
//import com.company.chamberly.logEvent
//import com.company.chamberly.models.Message
//import com.company.chamberly.models.UserRatingModel
//import com.company.chamberly.models.toMap
//import com.google.firebase.analytics.FirebaseAnalytics
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
//import com.google.firebase.database.ChildEventListener
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.ValueEventListener
//import com.google.firebase.database.ktx.database
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.Query
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
//import com.google.firebase.storage.ktx.storage
//import com.google.gson.Gson
//import id.zelory.compressor.constraint.size
//import okhttp3.Call
//import okhttp3.Callback
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import okhttp3.Response
//import org.json.JSONObject
//import java.io.File
//import java.io.IOException
//
//
//class ChatActivity : ComponentActivity(){
//    private lateinit var firebaseAnalytics: FirebaseAnalytics
//    private lateinit var cacheFile : File   // cache file
//    private lateinit var sharedPreferences: SharedPreferences
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var   messageAdapter: MessageAdapter
//    private lateinit var groupChatId: String
//    private lateinit var groupTitle :String
//    private lateinit var authorName :String
//    private lateinit var authorUID : String
//    private var messages = mutableListOf<Message>() // message list
//    private val auth = Firebase.auth                // get current user
//    private val database = Firebase.database        // realtime database
//    private val firestore = Firebase.firestore      // firestore
//    private val firebaseStorage = Firebase.storage
//    private var hasLeftChat: Boolean = false
//    private var replyingTo: String = ""
//    private val reactionEmojis: List<String> = listOf("👍", "💗", "😂", "😯", "😥", "😔", "+")
//    private val chamberLeavingOptions: Map<String, String> = mapOf(
//        "DONE VENTING" to "\"I am done venting, thank you so much 💗\"",
//        "WRONG MATCH" to "\"Wrong match, sorry 😢\"",
//        "IN A HURRY" to "\"Sorry, I am in a hurry 😰\"",
//        "JUST CHECKING THE APP OUT" to "\"Just checking the app 🥰\"",
//        "NO ACTIVITY" to "\"There is no activity 😔\""
//    )
//    private val reportReasons: List<String> = listOf(
//        "Harassment",
//        "Inappropriate Behavior",
//        "Unsupportive Behaviour",
//        "Spamming",
//        "Annoying"
//    )
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_chat)
//
//        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
//        sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE) // get shared preferences
//        val currentUser = auth.currentUser // get current user
//        val uid = sharedPreferences.getString("uid", currentUser?.uid) ?: currentUser?.uid // get uid
//
//        messageAdapter = MessageAdapter(uid!!) // create message adapter
//        groupChatId = intent.getStringExtra("GroupChatId") ?: "" // Default to empty string if null
//        groupTitle = intent.getStringExtra("GroupTitle") ?: ""
//        authorName = intent.getStringExtra("Authorname") ?: ""
//        authorUID = intent.getStringExtra("AuthorUID") ?: auth.currentUser!!.uid
//
//        recyclerView = findViewById(R.id.recyclerViewMessages)         // get recycler view
//        recyclerView.adapter = messageAdapter
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        messageAdapter.setOnMessageLongClickListener(object : MessageAdapter.OnMessageLongClickListener {
//            override fun onMessageLongClick(message: Message) {
//                showDialog(message)
//            }
//            override fun onSelfLongClick(message: Message) {
//                showSelfDialog(message)
//            }
//        })
//        recyclerView.adapter = messageAdapter
//
//        val infoButton = findViewById<ImageButton>(R.id.infoButton)
//
//        infoButton.setOnClickListener{
//            showInfoDialog()
//        }
//
//
//        //load cache file
//        cacheFile = File(this.cacheDir, groupChatId)
//        if(cacheFile.exists()){
//            //load data from the file
//            //the content of the file is a JSON string
//            val content = this.openFileInput(groupChatId).bufferedReader().use { it.readText() }
//            // Convert the content to a list of Message and update the UI
//            Gson()
//            val type = object : TypeToken<List<Message>>() {}.type
//            messages = Gson().fromJson(content, type)
//            messageAdapter.notifyDataSetChanged()    // update the UI
//        }
//
//        val backButton: ImageButton = findViewById(R.id.backButton)
//        backButton.setOnClickListener {
//            finish()
//        }
//
//        val messagesQuery = database.getReference(groupChatId)
//            .child("messages")
//            .orderByKey()
//            .limitToLast(40)
//        messagesQuery.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                messages.clear() // Clear the list
//
//                var lastMsg = ""
//
//                for (childSnapshot in snapshot.children) {
//                    // Check if the data can be converted to a Message object
//                    if (childSnapshot.value is Map<*, *>) {
//                        try {
//                            val message = childSnapshot.getValue(Message::class.java)
//                            if (message != null) {
//                                if (message.message_type == "custom" && message.message_content == "gameCard") {
//                                    message.message_content = message.game_content
//                                } else if (message.message_type == "photo") {
//                                }
//                                messages.add(message)
//                                lastMsg = message.message_content
//                            }
//                            recyclerView.smoothScrollToPosition(messages.size - 1)
//                        } catch (e: Exception) {
//                            Log.e("ChatActivity", "Error parsing message: ${e.message}")
//                        }
//                    }
//                }
//
//                updateChamberLastMessage(groupChatId, lastMsg)
//
//                // Save data to cache and update UI
//                val content = Gson().toJson(messages)
//                this@ChatActivity.openFileOutput(groupChatId, Context.MODE_PRIVATE).use {
//                    it.write(content.toByteArray())
//                }
//                messageAdapter.setMessages(messages)
//                recyclerView.scrollToPosition(messages.size - 1)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("ChatActivity", "Error fetching messages: ${error.message}")
//            }
//        })
//
//        messagesQuery.addChildEventListener(object: ChildEventListener {
//            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                try {
//                    val message = snapshot.getValue(Message::class.java)
//                    if (message != null) {
//                        if (message.message_type == "custom" && message.message_content == "gameCard") {
//                            message.message_content = message.game_content
//                        }else if (message.message_type=="photo"){
//                            Log.d("added",message.message_content)
//
//                        }
//                            messageAdapter.addMessage(message)
//
//
//                        recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
//                    }
//                } catch(e: Exception) {
//                    Log.e("NEW MESSAGE ERROR", e.toString())
//                }
//
//            }
//            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                try {
//                    val message = snapshot.getValue(Message::class.java)
//                    if (message != null) {
//                        if (message.message_type == "custom" && message.message_content == "gameCard") {
//                            message.message_content = message.game_content
//                        }
//                        Log.d("update",message.message_content)
//                        messageAdapter.messageChanged(message, message.message_id)
//                        recyclerView.smoothScrollBy(0, 20)
//                    }
//                } catch(e: Exception) {
//                    //TODO: Handle the error
//                }
//
//            }
//            override fun onChildRemoved(snapshot: DataSnapshot) {
//                try {
//                    val message = snapshot.getValue(Message::class.java)
//                    if(message != null) {
//                        messageAdapter.messageRemoved(message = message)
//                    }
//                } catch (e: Exception) {
//                    // TODO: Fix later
//                }
//            }
//            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                //Not required for now
//            }
//            override fun onCancelled(error: DatabaseError) {
//                //TODO: Handle the error
//            }
//        })
//
//        // Find the information bar
//        val infoBar = findViewById<LinearLayout>(R.id.infoBar)
//
//        val recyclerViewLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
//            val bottomDifference = oldBottom - bottom
//            if (bottomDifference > 0) { // Keyboard is shown
//                recyclerView.scrollBy(0, bottomDifference)
//            }
//        }
//        // Add the OnLayoutChangeListener to the RecyclerView
//        recyclerView.addOnLayoutChangeListener(recyclerViewLayoutChangeListener)
//
//        val titleTextView = findViewById<TextView>(R.id.groupTitle)
//        titleTextView.text = groupTitle
//
//        val sendButton = findViewById<Button>(R.id.buttonSend)
//        sendButton.setOnClickListener {
//            val editText = findViewById<EditText>(R.id.editTextMessage)
//
//            val text = editText.text.toString()
//            if (text.isBlank()) {
//                return@setOnClickListener
//            }
//            val replyView = findViewById<LinearLayout>(R.id.replyingToView)
//            val senderName = sharedPreferences.getString("displayName", "NONE")
//            val messageId = database.reference.child(groupChatId).push().key
//            val message = Message(
//                UID = uid,
//                message_content = editText.text.toString(),
//                message_type = "text",
//                sender_name = senderName!!,
//                message_id = messageId!!,
//                replyingTo = replyingTo
//            )
//
//            replyingTo = ""
//            replyView.visibility = View.GONE
//            editText.setText("")
//
//            database.reference
//                .child(groupChatId)
//                .child("messages")
//                .child(messageId)
//                .setValue(message.toMap())
//                .addOnSuccessListener {
//                    Log.i("beforeSend","success")
//                    var fcmtkn: String
//                    WelcomeActivity().getFcmToken {
//                        if (it != null) {
//                            if(it.isNotBlank()){
//                                fcmtkn = it.toString()
//                                sendNotification(message,fcmtkn)
//                            }
//                        }
//                    }
//                    messageAdapter.addMessage(message)
//                    logEvent(
//                        firebaseAnalytics = firebaseAnalytics,
//                        eventName = "message_sent",
//                        params = hashMapOf(
//                            "UID" to uid,
//                            "name" to senderName,
//                            "groupChatId" to groupChatId
//                        )
//                    )
//                    sendNotificationToInActiveMembers()
//                    recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
//
//                }
//                .addOnFailureListener { e ->
//                    Toast.makeText(this, "Error sending message: $e", Toast.LENGTH_SHORT).show()
//                }
//        }
//        addNotificationKeyListener()
//        //Uploading Image to Firebase
//        val addImageButton = findViewById<Button>(R.id.buttonAddImage)
//        val senderName = sharedPreferences.getString("displayName", "NONE")
//t
//    }
//
//    private fun exitChat(groupChatId: String) {
//        val userUID = sharedPreferences.getString("UID", auth.currentUser!!.uid)
//        val displayName = sharedPreferences.getString("name", "")
//        logEvent(
//            firebaseAnalytics = firebaseAnalytics,
//            eventName = "ended_chat",
//            params = hashMapOf(
//                "UID" to userUID!!,
//                "name" to displayName!!
//            )
//        )
//        hasLeftChat = true
//        // Remove the user from the chamber's member list in Firestore
//        firestore.collection("GroupChatIds").document(groupChatId)
//            .update("members", FieldValue.arrayRemove(userUID))
//            .addOnSuccessListener {
//                //Remove user from groupChat
//                userUID.let { userID ->
//                    database
//                        .reference
//                        .child(groupChatId)
//                        .child("users")
//                        .child("members")
//                        .child(userID)
//                        .removeValue()
//                }
//                reportUser(reason = "Left the chamber", selfReport = false)
//            }
//    }
//
//    override fun onPause() {
//        addNotificationKey()
//        super.onPause()
//    }
//
//    override fun onDestroy() {
//        addNotificationKey()
//        super.onDestroy()
//    }
//
//    override fun onResume() {
//        removeNotificationKey()
//        super.onResume()
//    }
//
//    override fun onRestart() {
//        removeNotificationKey()
//        super.onRestart()
//    }
//
//
//    // copy message
//    private fun copyMessage(message: Message) {
//        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        val clip = ClipData.newPlainText("Message", message.message_content)
//        clipboard.setPrimaryClip(clip)
//        Log.e("Holder", "Holder successfully copied: ${message.message_content}")
//
//    }
//
//    private fun updateChamberLastMessage(groupChatId: String, lastMsg: String) {
//        val chamberRef = firestore.collection("chambers").document(groupChatId)
//        chamberRef.update("lastMessage", lastMsg)
//    }
//
//    // report user
//    private  fun reportUser(against: String? = null, reason: String, selfReport: Boolean = true) {
//        val uid = auth.currentUser!!.uid
//
//        val report = hashMapOf(
//            "against" to (against ?: uid),
//            "by" to uid,
//            "groupChatId" to groupChatId,
//            "realHost" to "",
//            "messages" to messages.map {
//               it.toMap()
//            },
//            "reason" to reason,
//            "reportDate" to FieldValue.serverTimestamp(),
//            "ticketTaken" to false,
//            "selfReport" to selfReport,
//            "title" to "",
//            "description" to "",
//            "reportDate" to FieldValue.serverTimestamp(),
//        )
//        firestore.collection("Reports").add(report)
//            .addOnSuccessListener {
//                if(selfReport)
//                    Toast.makeText(this, "User reported", Toast.LENGTH_SHORT).show()
//            }
//    }
//    // block user
//    private fun blockUser(uid: String) {
//        val localUID = sharedPreferences.getString("uid", auth.currentUser?.uid) ?: auth.currentUser!!.uid // get uid from cache or firebase
//        val topicsList = (sharedPreferences.getString("topics", "") ?: "").split(",")
//
//        val currUserRef = firestore.collection("Accounts").document(localUID)
//        currUserRef.update("blockedUsers", FieldValue.arrayUnion(uid))
//        currUserRef.get().addOnSuccessListener { userData ->
//            val blockedUsers = (userData.data!!["blockedUsers"] as MutableList<String>?) ?: mutableListOf()
//            if(!blockedUsers.contains(uid))
//                blockedUsers.add(uid)
//            currUserRef.update(mapOf("blockedUsers" to blockedUsers))
//            for(topic in topicsList) {
//                if(topic.isNotBlank()){
//                    database.reference
//                        .child(topic)
//                        .child("users")
//                        .child(localUID)
//                        .child("blockedUsers")
//                        .setValue(blockedUsers)
//                }
//            }
//        }
//    }
//
//    private fun addNotificationKeyListener() {
//        val notificationKey = sharedPreferences.getString("notificationKey", "") ?: ""
//        val userRef = database
//            .reference
//            .child(groupChatId)
//            .child("users")
//            .child("members")
//            .child(auth.currentUser!!.uid)
//        userRef.child("notificationKey").setValue(null)
//        userRef.child("notificationKey").onDisconnect().setValue(notificationKey)
//
//        userRef.addValueEventListener(object: ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val user = snapshot.value
//                if(user == null) {
//                    userRef.child("notificationKey").onDisconnect().cancel()
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//            }
//
//        })
//    }
//
//
//    private fun addNotificationKey() {
//        val userRef = database.reference.child(groupChatId).child("users").child("members").child(auth.currentUser!!.uid)
//        userRef.get().addOnSuccessListener {
//            if(it.exists()) {
//                userRef
//                    .child("notificationKey")
//                    .setValue(getSharedPreferences("cache", Context.MODE_PRIVATE).getString("notificationKey", ""))
//            }
//        }
//    }
//
//    private fun removeNotificationKey() {
//        val userRef = database.reference.child(groupChatId).child("users").child("members").child(auth.currentUser!!.uid)
//        userRef.get().addOnSuccessListener {
//            if(it.exists()) {
//                userRef
//                    .child("notificationKey")
//                    .removeValue()
//            }
//        }
//    }
//    private fun sendNotificationToInActiveMembers() {
//        val currentNotifKey = sharedPreferences.getString("notificationKey", "") ?: ""
//        val notificationPayload = JSONObject()
//        val membersRef = database.reference.child(groupChatId).child("users").child("members")
//        membersRef.get().addOnSuccessListener { membersSnapshot ->
//            for(snapshot in membersSnapshot.children) {
//                val token: String? = snapshot.child("notificationKey").value as String?
//                if(!token.isNullOrBlank() && token != currentNotifKey) {
//                    try {
//                        notificationPayload.put("title", getSharedPreferences("cache", Context.MODE_PRIVATE).getString("displayName", "ANONYMOUS"))
//                        notificationPayload.put("body", "sent you a message")
//                        notificationPayload.put("groupChatId", groupChatId)
//                        notificationPayload.put("groupTitle", groupTitle)
//                        OkHttpHandler(notificationPayload, token).execute()
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//
//    }
//
//    private fun showSelfDialog(message: Message){
//        val dialog = Dialog(this, R.style.Dialog)
//        dialog.setContentView(R.layout.popup_message_options_self)
//
//        val  dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
//        dialogTitle.text = message.sender_name
//        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
//        dialogMessage.text = message.message_content
//
//        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)
//
//        // set dialog window's width and height
//        val window = dialog.window
//        val layoutParams = WindowManager.LayoutParams()
//        layoutParams.copyFrom(window?.attributes)
//        window?.attributes = layoutParams
//
//        copyButton.setOnClickListener {
//            copyMessage(message)
//            dialog.dismiss()
//        }
//
//        dialog.show()
//    }
//
//    private fun react(message: Message, emoji: String) {
//        database.reference
//            .child(groupChatId)
//            .child("messages")
//            .child(message.message_id)
//            .child("reactedWith")
//            .setValue(emoji)
//    }
//
//    private fun showDialog(message: Message) {
//
//        val dialog = Dialog(this, R.style.Dialog)
//        dialog.setContentView(R.layout.popup_message_options_others)
//
//        val dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
//        dialogTitle.text = message.sender_name
//        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
//        dialogMessage.text = message.message_content
//
//        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)
//        val replyButton = dialog.findViewById<Button>(R.id.buttonReply)
//        val reportButton = dialog.findViewById<Button>(R.id.buttonReport)
//        val blockButton = dialog.findViewById<Button>(R.id.buttonBlock)
//        val rateUserButton = dialog.findViewById<Button>(R.id.buttonRate)
//        val reactionEmojisView = dialog.findViewById<LinearLayout>(R.id.reactionEmojis)
//        val emojiPickerView = findViewById<EmojiPickerView>(R.id.reaction_emoji_picker)
//        val replyView = findViewById<LinearLayout>(R.id.replyingToView)
//        val replyContentView = findViewById<TextView>(R.id.replyContentView)
//        val cancelReplyButton = findViewById<ImageButton>(R.id.cancelReplyButton)
//
//        for(emoji in reactionEmojis) {
//            val emojiButton = TextView(this)
//            emojiButton.text = emoji
//            emojiButton.setTextColor(Color.BLACK)
//            emojiButton.textSize = 24.0f
//            emojiButton.setPadding(8, 8, 8, 8)
//            emojiButton.setOnClickListener {
//                if(emoji != "+") {
//                    react(message, emoji)
//                } else {
//                    emojiPickerView.visibility = View.VISIBLE
//                    emojiPickerView.setOnEmojiPickedListener {
//                        dialog.dismiss()
//                        emojiPickerView.visibility = View.GONE
//                        react(message, it.emoji)
//                    }
//                }
//                recyclerView.smoothScrollBy(0, 20)
//                dialog.dismiss()
//            }
//            reactionEmojisView.addView(emojiButton)
//        }
//
//        // set dialog window's width and height
//        val window = dialog.window
//        val layoutParams = WindowManager.LayoutParams()
//        layoutParams.copyFrom(window?.attributes)
//        window?.attributes = layoutParams
//
//        copyButton.setOnClickListener {
//            copyMessage(message)
//            dialog.dismiss()
//        }
//
//        replyButton.setOnClickListener {
//            dialog.dismiss()
//            replyView.visibility = View.VISIBLE
//            replyingTo = message.sender_name
//            replyContentView.text = message.sender_name
//            cancelReplyButton.setOnClickListener {
//                replyContentView.text = ""
//                replyView.visibility = View.GONE
//            }
//        }
//
//        rateUserButton.setOnClickListener {
//            //Reward stars to the user
//            showRatingDialog(dialog, message.UID, message.sender_name)
//        }
//
//        // set report button's click listener
//        reportButton.setOnClickListener {
//            showReportDialog(dialog, against = message.UID, againstName = message.sender_name)
//        }
//
//        // set block button's click listener
//        blockButton.setOnClickListener {
//            dialog.setContentView(R.layout.confirm_block)
//            val blockDialogTitle = dialog.findViewById<TextView>(R.id.blockDialogTitle)
//            blockDialogTitle.text = getString(R.string.block_user_dialog_title, message.sender_name)
//            val confirmButton = dialog.findViewById<Button>(R.id.buttonConfirmBlock)
//            val cancelButton = dialog.findViewById<Button>(R.id.buttonCancelBlock)
//            confirmButton.setOnClickListener {
//                blockUser(message.UID)
//                dialog.dismiss()
//                showInfoDialog()
//
//            }
//            cancelButton.setOnClickListener {
//                dialog.dismiss()
//                showDialog(message)
//            }
//        }
//
//        // show Dialog
//        dialog.show()
//    }
//
//    private fun showRatingDialog(dialog: Dialog, userToRateUID: String, userToRateName: String) {
//        dialog.setContentView(R.layout.dialog_rate_user)
//
//        val heading = dialog.findViewById<TextView>(R.id.title_rate_user)
//        val ratingBar = dialog.findViewById<RatingBar>(R.id.user_review_bar)
//        val cancelButton = dialog.findViewById<Button>(R.id.button_rating_cancel)
//        val confirmButton = dialog.findViewById<Button>(R.id.button_rating_confirm)
//
//        heading.text = getString(R.string.report_user_title_text, userToRateName)
//
//        cancelButton.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        confirmButton.setOnClickListener {
//            val stars = ratingBar.rating
//            rateUser(userToRateUID, stars.toDouble())
//            if(stars == 5.0f) {
//                askForPlayStoreReview(dialog)
//            } else if(stars <= 3.0f) {
//                blockUser(uid = userToRateUID)
//                showReportDialog(
//                    dialog = dialog,
//                    against = userToRateUID,
//                    againstName = userToRateName,
//                )
//            } else {
//                dialog.dismiss()
//            }
//        }
//    }
//
//    private fun rateUser(userToRate: String, starRating: Double) {
//        val uid = sharedPreferences.getString("uid", auth.currentUser!!.uid)
//        val displayName = sharedPreferences.getString("displayName", "")
//        var reviewCountFlag = true
//        var starRatingChange = starRating
//
//        val collectionRef = firestore.collection("StarReviews")
//
//        val review = UserRatingModel(
//            from = uid!!,
//            to = userToRate,
//            stars = starRating
//        )
//
//        collectionRef
//            .whereEqualTo("To", userToRate)
//            .whereEqualTo("From", uid)
//            .orderBy("timestamp", Query.Direction.DESCENDING)
//            .limit(1)
//            .get()
//            .addOnSuccessListener { documents ->
//                if(!documents.isEmpty) {
//                    val userReview = documents.documents[0]
//                    starRatingChange -= userReview.getDouble("Stars")!!
//                    reviewCountFlag = false
//                }
//
//                collectionRef
//                    .whereEqualTo("To", userToRate)
//                    .orderBy("timestamp", Query.Direction.DESCENDING)
//                    .limit(1)
//                    .get()
//                    .addOnSuccessListener { previousRatings ->
//                        val latestReview = previousRatings.documents.getOrNull(0)
//                        var totalStars = latestReview?.getDouble("TotalStars") ?: 0.0
//                        var reviewsCount = latestReview?.getLong("ReviewsCount") ?: 0
//
//                        totalStars += starRatingChange
//
//                        if (reviewCountFlag) {
//                            reviewsCount++
//                        }
//
//                        logEvent(
//                            firebaseAnalytics = firebaseAnalytics,
//                            eventName = "user_rated",
//                            params = hashMapOf(
//                                "UID" to uid,
//                                "name" to displayName!!,
//                                "user_rated" to starRating
//                            )
//                        )
//
//                        review.totalStars = totalStars
//                        review.reviewCount = reviewsCount.toInt()
//                        review.averageStars = totalStars / reviewsCount
//                        collectionRef.add(review.toMap())
//                    }
//            }
//    }
//
//    private fun askForPlayStoreReview(dialog: Dialog) {
//        dialog.setContentView(R.layout.dialog_ask_rating_playstore)
//
//        val confirmButton = dialog.findViewById<TextView>(R.id.getPlayStoreReviewButton)
//        val dismissButton = dialog.findViewById<TextView>(R.id.dismissDialogButton)
//
//        dismissButton.setOnClickListener { dialog.dismiss() }
//        confirmButton.setOnClickListener {
//            dialog.dismiss()
//            val appId = "com.google.android.youtube"
//            try {
//                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")))
//            } catch (e: ActivityNotFoundException) {
//                // If the Play Store is not installed, open the web browser
//                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId")))
//            }
//        }
//    }
//
//    private fun showReportDialog(dialog: Dialog, against: String, againstName: String) {
//        dialog.setContentView(R.layout.dialog_report_options)
//
//        val titleTextView = dialog.findViewById<TextView>(R.id.textReportTitle)
//        titleTextView.text = getString(R.string.reporting_message, againstName)
//
//        val optionsLayout = dialog.findViewById<LinearLayout>(R.id.reportReasonsLayout)
//
//        for(reason in reportReasons) {
//            val optionButton = TextView(this)
//            optionButton.text = reason
//            optionButton.setTextColor(getColor(R.color.red))
//            optionButton.textSize = 18.0f
//            optionButton.setPaddingRelative(16, 16, 16, 16)
//            optionButton.gravity = Gravity.CENTER_HORIZONTAL
//            optionButton.setOnClickListener {
//                reportUser(against = against, reason = reason)
//                dialog.dismiss()
//            }
//            optionsLayout.addView(optionButton)
//        }
//
//        val cancelButton = TextView(this)
//        cancelButton.text = "Cancel"
//        cancelButton.setTextColor(Color.BLACK)
//        cancelButton.textSize = 18.0f
//        cancelButton.setPaddingRelative(16, 16, 16, 16)
//        cancelButton.gravity = Gravity.CENTER_HORIZONTAL
//        cancelButton.setOnClickListener {
//            dialog.dismiss()
//        }
//        optionsLayout.addView(cancelButton)
//        // show Dialog
//        dialog.show()
//    }
//
//    private fun showInfoDialog(cancellable: Boolean = true) {
//
//        val dialog = Dialog(this, R.style.Dialog)
//        dialog.setContentView(R.layout.dialog_info)
//        dialog.setCancelable(cancellable)
//        dialog.setCanceledOnTouchOutside(cancellable)
//        val leaveChamberOptionsLayout = dialog.findViewById<LinearLayout>(R.id.leave_chamber_options)
//
//        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
//        val uid = sharedPreferences.getString("uid", auth.currentUser?.uid)
//        val senderName = sharedPreferences.getString("displayName", "NONE")
//        var message = Message(
//            UID = uid!!,
//            message_content = "$senderName left the chat. Reason: ",
//            message_type = "custom",
//            sender_name = senderName ?: ""
//        )
//
//
//        val messagesRef = database.getReference(groupChatId).child("messages")
//
//        for ((heading, reason) in chamberLeavingOptions) {
//            val optionButton = TextView(this)
//            optionButton.text = heading
//            optionButton.setTextColor(getColor(R.color.primary))
//            optionButton.textSize = 18.0f
//            optionButton.setPaddingRelative(16, 16, 16, 16)
//            optionButton.gravity = Gravity.CENTER_HORIZONTAL
//            optionButton.setOnClickListener {
//                message = message.copy(message_content = message.message_content + reason)
//                messagesRef
//                    .push()
//                    .setValue(message.toMap())
//                    .addOnSuccessListener {
//                        exitChat(groupChatId)
//                        finish()
//                        goToMainActivity()
//                    }
//            }
//            leaveChamberOptionsLayout.addView(optionButton)
//        }
//        if(cancellable) {
//            val cancelButton = TextView(this)
//            cancelButton.text = "CANCEL"
//            cancelButton.setTextColor(Color.BLACK)
//            cancelButton.textSize = 18.0f
//            cancelButton.setPaddingRelative(16, 16, 16, 16)
//            cancelButton.gravity = Gravity.CENTER_HORIZONTAL
//            cancelButton.setOnClickListener {
//                dialog.dismiss()
//            }
//            leaveChamberOptionsLayout.addView(cancelButton)
//        }
//
//        dialog.show()
//
//    }
//
//    private fun goToMainActivity() {
//        val intent = Intent(this, MainActivity::class.java)
//        startActivity(intent)
//        finish()
//    }
//
//    private fun sendNotification(message: Message, currSendToken:String) {
//        val uid = message.UID
//        val content = message.message_content
//        val sender = message.sender_name
//        Log.i("message", "$uid $content $sender")
//        Log.i("beforeSend","inside sendNotification")
//        val curUid = FirebaseAuth.getInstance().currentUser?.uid
//        // Retrieve the display name from SharedPreferences
//        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
//        val displayName = sharedPreferences.getString("displayName", "Anonymous") ?: "Anonymous"
//        Log.i("beforeSend","inside sendNotification shared prefs")
//        //retriev th name of current user
//        // Query the Firestore collection "Accounts" to retrieve FCM tokens
//        val firestore = Firebase.firestore
//        Log.i("beforeSend","firestore is initialized")
//        firestore.collection("Accounts")
//            .get()
//            .addOnSuccessListener { querySnapshot ->
//                for (document in querySnapshot) {
//                    val fcmToken = document.data["FCMTOKEN"] as? String ?: ""
//                    if (document.id != curUid && currSendToken != fcmToken && fcmToken.isNotEmpty()) {
//                        val notification = JSONObject()
//                        val notificationData = JSONObject()
//                        notificationData.put("title", groupTitle)
//                        notificationData.put("body", "$displayName: ${message.message_content}")
//                        notificationData.put("groupChatId", groupChatId)
//                        notificationData.put("groupTitle", groupTitle)
//                        notificationData.put("Authorname", authorName)
//                        notificationData.put("AuthorUID", authorUID)
//                        notification.put("to", fcmToken)
//                        notification.put("data", notificationData)
//
//                        callApi(notification)
//                    }
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("FirestoreError", "Error retrieving FCM tokens: $e")
//            }
//
//    }
//
//    private fun callApi(jsonObject: JSONObject) {
//        var token :String
//
//        Log.i("beforeSend","inside callAPi")
//        val JSON = "application/json".toMediaType()
//        val client = OkHttpClient()
//
//
//        WelcomeActivity().getFcmToken {
//            if (it != null) {
//                if(it.isNotBlank() && it.isNotBlank()){
//                    token = it
//                    Log.i("beforeSend","inside callAPi token$token")
//                }
//            }
//        }
//
//        val body = jsonObject.toString().toRequestBody(JSON)
//        val url = "https://fcm.googleapis.com/fcm/send"
//        val request = Request.Builder()
//            .url(url)
//            .post(body)
//            .header(name = "Authorization","Bearer AAAAlM4gQ_E:APA91bHUCWkcutsNbe-f8djemRyNbsnTC9Gr57f5L8VTmTHuu-ymGtbEIUkACu-IQYrFxH8Uv5aZmyrYhKAMqvhZeT8X471DS1eRQWVqYN7C-jf4hQBJi-0kYTWDeN6XOfsDOWyD2AiS")
//            .build()
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.i("HttpFailure",e.message.toString())
//                Log.i("HttpFailure",call.request().toString())
//            }
//            override fun onResponse(call: Call, response: Response) {
//                Log.i("response", response.message)
//                response.body?.toString()?.let { Log.i("response", it) }
//                response.body?.close()
//                Log.i("beforeSend","inside callAPi onresponse")
//            }
//        })
//    }
//
//
//
//}
