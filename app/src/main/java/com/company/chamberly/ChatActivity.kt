package com.company.chamberly

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException


// TODO: Check caps and lowercase in both database and firestore

class ChatActivity : ComponentActivity(){
    // TODO: add chat cache
    private lateinit var cacheFile : File   // cache file
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    //private lateinit var chamber: Chamber
    private lateinit var groupChatId: String
    private lateinit var groupTitle :String
    private lateinit var authorName :String
    private lateinit var authorUID : String
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var messages = mutableListOf<Message>() // message list
    private val auth = Firebase.auth                // get current user
    private val database = Firebase.database        // realtime database
    private val firestore = Firebase.firestore      // firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE) // get shared preferences
        val currentUser = auth.currentUser // get current user
        val uid = sharedPreferences.getString("uid", "") ?: currentUser?.uid // get uid

        messageAdapter = MessageAdapter(uid!!) // create message adapter
        //chamber = intent.getSerializableExtra("chamber") as Chamber // get chamber
        groupChatId = intent.getStringExtra("groupChatId") ?: "" // Default to empty string if null
        groupTitle = intent.getStringExtra("groupTitle") ?: ""
        authorName = intent.getStringExtra("authorName") ?: ""
        authorUID = intent.getStringExtra("authorUID") ?: ""

        recyclerView = findViewById(R.id.recyclerViewMessages)         // get recycler view
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        messageAdapter.setOnMessageLongClickListener(object : MessageAdapter.OnMessageLongClickListener {
            override fun onMessageLongClick(message: Message) {
                showDialog(message)
            }
            override fun onSelfLongClick(message: Message) {
                showSelfDialog(message)
            }
        })
        recyclerView.adapter = messageAdapter

        val infoButton = findViewById<ImageButton>(R.id.infoButton)


        infoButton.setOnClickListener{
            showInfoDialog()
        }


        //load cache file
        cacheFile = File(this.cacheDir, groupChatId)
        if(cacheFile.exists()){
            //load data from the file
            //the content of the file is a JSON string
            val content = this.openFileInput(groupChatId).bufferedReader().use { it.readText() }
            // Convert the content to a list of Message and update the UI
            Gson()
            val type = object : TypeToken<List<Message>>() {}.type
            messages= Gson().fromJson(content, type)
            messageAdapter.notifyDataSetChanged()    // update the UI
        }

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            // Explicitly start MainActivity
            val intent = Intent(this, ActiveChambersActivity::class.java)
            startActivity(intent)
        }

        val messagesRef = database.getReference(groupChatId).child("messages")
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear() // Clear the list

                var lastMsg = ""

                for (childSnapshot in snapshot.children) {
                    // Check if the data can be converted to a Message object
                    if (childSnapshot.getValue() is Map<*, *>) {
                        try {
                            val message = childSnapshot.getValue(Message::class.java)
                            if (message != null) {
                                messages.add(message)
                                lastMsg = message.message_content
                            }
                        } catch (e: Exception) {
                            Log.e("ChatActivity", "Error parsing message: ${e.message}")
                        }
                    }
                }

                updateChamberLastMessage(groupChatId, lastMsg)

                // Save data to cache and update UI
                val content = Gson().toJson(messages)
                this@ChatActivity.openFileOutput(groupChatId, Context.MODE_PRIVATE).use {
                    it.write(content.toByteArray())
                }
                messageAdapter.setMessages(messages)
                recyclerView.scrollToPosition(messages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Error fetching messages: ${error.message}")
            }
        })



        // Find the information bar
        val infoBar = findViewById<LinearLayout>(R.id.infoBar)

        val titleTextView = findViewById<TextView>(R.id.groupTitle)
        titleTextView.text = groupTitle

        val sendButton = findViewById<Button>(R.id.buttonSend)
        sendButton.setOnClickListener {
            val editText = findViewById<EditText>(R.id.editTextMessage)
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", currentUser?.uid)
            val senderName = sharedPreferences.getString("displayName", "NONE")
            val message = Message(uid!!, editText.text.toString(), "text", senderName!!)

            database.getReference(groupChatId).child("messages").push().setValue(message)
                .addOnSuccessListener {
                    Log.i("beforeSend","success")
                    var fcmtkn: String
                    WelcomeActivity().getFcmToken {
                        if (it != null) {
                            if(it.isNotBlank() && it.isNotBlank()){
                                fcmtkn = it.toString()
                                sendNotification(message,fcmtkn)
                            }
                        }
                    }

                    editText.setText("")
                    messages.add(message)
                    Log.i("beforeSend","addMessage")
                    // TODO add into cache
                    recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                    Log.i("beforeSend","scroll")
                    Log.i("beforeSend","ooo")

                    Log.i("beforeSend","ooo")

                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error sending message: $e", Toast.LENGTH_SHORT).show()
                }
        }

        // Add a listener to check if the user's username exists in the "members" node
        database.reference.child(groupChatId).child("Users").child("members")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val uid = sharedPreferences.getString("uid", auth.currentUser?.uid)
                    if (!snapshot.hasChild(uid!!)) {
                        // User's username does not exist in "members" node, exit the chat
                        exitChat(groupChatId)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatActivity", "Error checking chat membership: ${error.message}")
                }
            })

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitChat(groupChatId)
            }
        }
        this@ChatActivity.onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }



    private fun exitChat(groupChatId: String) {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid

        // Send a system message indicating that the user is leaving
        sendExitSystemMessage(groupChatId, userUID)

        // Remove the user from the chamber's member list in Firestore
        firestore.collection("GroupChatIds").document(groupChatId)
            .update("members", FieldValue.arrayRemove(userUID))
            .addOnSuccessListener {
                // Check if the chamber should be deleted
                checkAndHandleChamberDeletion(groupChatId)
            }
    }

    // Function to send a system message about user exit
    private fun sendExitSystemMessage(groupChatId: String, userUID: String?) {
        val exitMessage = userUID?.let {
            Message("system", "Your companion has exited the chat.", "system",
                "Chamberly",

            )
        }
        database.getReference(groupChatId).child("messages").push().setValue(exitMessage)
    }

    // Function to check and handle the deletion of the chamber if necessary
    private fun checkAndHandleChamberDeletion(groupChatId: String) {
        firestore.collection("GroupChatIds").document(groupChatId).get()
            .addOnSuccessListener { documentSnapshot ->
                val members = documentSnapshot["members"] as? List<*>
                if (members.isNullOrEmpty()) {
                    deleteChamber(groupChatId)
                }
            }
    }

    // copy message
    private fun copyMessage(message: Message) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", message.message_content)
        clipboard.setPrimaryClip(clip)
        Log.e("Holder", "Holder successfully copied: ${message.message_content}")

    }

    private fun updateChamberLastMessage(groupChatId: String, lastMsg: String) {
        val chamberRef = firestore.collection("chambers").document(groupChatId)
        chamberRef.update("lastMessage", lastMsg)
            .addOnSuccessListener { Log.d("ChatActivity", "Last message updated") }
            .addOnFailureListener { e -> Log.w("ChatActivity", "Error updating last message", e) }
    }

    // report user
    private  fun reportUser(message: Message, reason: String){
        // Todo: get chamber info early

        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val UID = sharedPreferences.getString("uid", auth.currentUser?.uid)


        val report = hashMapOf(
            "against" to message.UID,
            "by" to UID,
            "groupChatId" to groupChatId,
            "realHost" to "",
            "reason" to reason,
            "reportDate" to FieldValue.serverTimestamp(),
            "realHost" to authorName,
            "ticketTaken" to false
            //"Title" to ?
        )
        firestore.collection("Reports").add(report)
            .addOnSuccessListener {
                Toast.makeText(this, "User reported", Toast.LENGTH_SHORT).show()
            }
    }
    // block user
    private fun blockUser(message: Message) {
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val localUID = sharedPreferences.getString("uid", auth.currentUser?.uid)// get uid from cache or firebase

        val uid = message.UID

        if(authorUID == localUID){
            // delete the group chat id from the user's list
            database.reference.child(groupChatId).child("Users").child("members").child(uid).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "User banned", Toast.LENGTH_SHORT).show()
                }
        }
    }



    override fun onDestroy() {
        onBackPressedCallback.remove()
        super.onDestroy()
    }

    private fun showSelfDialog(message: Message){
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.dialog_self_message_options)

        val  dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
        dialogTitle.text = message.sender_name
        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
        dialogMessage.text = message.message_content

        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)

        // set dialog window's width and height
        val window = dialog.window
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        //layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        //layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams

        copyButton.setOnClickListener {
            copyMessage(message)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDialog(message: Message) {

        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val localUID = sharedPreferences.getString("uid", auth.currentUser?.uid)// get uid from cache or firebase

        val dialog = Dialog(this, R.style.Dialog)

        if(localUID == authorUID){
            dialog.setContentView(R.layout.dialog_host_message_options)
            val blockButton = dialog.findViewById<Button>(R.id.buttonBlock)

            // set block button's click listener
            blockButton.setOnClickListener {
                blockUser(message)
                dialog.dismiss()
            }
        }
        else{
            dialog.setContentView(R.layout.dialog_message_options)
        }

        val dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
        dialogTitle.text = message.sender_name
        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
        dialogMessage.text = message.message_content

        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)
        val reportButton = dialog.findViewById<Button>(R.id.buttonReport)


        // set dialog window's width and height
        val window = dialog.window
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        //layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        //layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams

        // set copy button's click listener
        copyButton.setOnClickListener {
            copyMessage(message)
            dialog.dismiss()
        }

        // set report button's click listener
        reportButton.setOnClickListener {
            showReportDialog(message)
            dialog.dismiss()
        }

        // show Dialog
        dialog.show()
    }

    private fun showReportDialog(message: Message) {
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.dialog_report_options)

        val titleTextView = dialog.findViewById<TextView>(R.id.textReportTitle)
        titleTextView.text = "Reporting ${message.sender_name}"
        val harassmentButton = dialog.findViewById<Button>(R.id.buttonHarassment)
        val inappropriateBehaviorButton =
            dialog.findViewById<Button>(R.id.buttonInappropriateBehavior)
        val unsupportiveBehaviorButton =
            dialog.findViewById<Button>(R.id.buttonUnsupportiveBehavior)
        val spammingButton = dialog.findViewById<Button>(R.id.buttonSpamming)
        val annoyingButton = dialog.findViewById<Button>(R.id.buttonAnnoying)

        harassmentButton.setOnClickListener {
            reportUser(message, "Harassment")
            dialog.dismiss()
        }
        inappropriateBehaviorButton.setOnClickListener {
            reportUser(message, "Inappropriate Behavior")
            dialog.dismiss()
        }
        unsupportiveBehaviorButton.setOnClickListener {
            reportUser(message, "Unsupportive Behavior")
            dialog.dismiss()
        }
        spammingButton.setOnClickListener {
            reportUser(message, "Spamming")
            dialog.dismiss()
        }
        annoyingButton.setOnClickListener {
            reportUser(message, "Annoying")
            dialog.dismiss()
        }

        // show Dialog
        dialog.show()
    }
    private fun showInfoDialog() {

        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.dialog_info)

        val deleteButton = dialog.findViewById<Button>(R.id.btn_delete_chamber)
        //TODO("Not yet implemented")

        deleteButton.setOnClickListener {
            deleteChamber(groupChatId)
            dialog.dismiss()
        }

        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val UID = sharedPreferences.getString("uid", auth.currentUser?.uid)

        // Show the dialog
        if(UID==authorUID)
        {
            dialog.show()
        }else{
            exitChat(groupChatId)
            goToMainActivity()
        }

    }

    private fun deleteChamber(groupChatId: String) {
        val messagesRef = database.getReference(groupChatId)

        messagesRef.removeValue()
            .addOnSuccessListener {
                firestore.collection("GroupChatIds").document(groupChatId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Chamber deleted", Toast.LENGTH_SHORT).show()
                        goToMainActivity()
                    }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }

        // Delete from Firestore
        firestore.collection("GroupChatIds").document(groupChatId).delete()

        // Delete from Realtime Database
        database.getReference(groupChatId).removeValue()

        // Go back to the main activity
        goToMainActivity()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun sendNotification(message: Message,currSendToken:String) {
        val uid = message.UID
        val content = message.message_content
        val sender = message.sender_name
        Log.i("message", "$uid $content $sender")
        Log.i("beforeSend","inside sendNotification")
        val curUid = FirebaseAuth.getInstance().currentUser?.uid
        // Retrieve the display name from SharedPreferences
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val displayName = sharedPreferences.getString("displayName", "Default Display Name") ?: "Default Display Name"
        Log.i("beforeSend","inside sendNotification shared prefs")
        //retriev th name of current user
        // Query the Firestore collection "Accounts" to retrieve FCM tokens
        val firestore = Firebase.firestore
        Log.i("beforeSend","firestore is initialized")
        firestore.collection("Accounts")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val fcmToken = document.data["FCMTOKEN"] as? String ?: ""
                    if (document.id != curUid && currSendToken != fcmToken && fcmToken.isNotEmpty()) {
                        val notification = JSONObject()
                        val notificationData = JSONObject()
                        notificationData.put("title", groupTitle)
                        notificationData.put("body", "$displayName: ${message.message_content}")
                        notificationData.put("groupChatId", groupChatId)
                        notificationData.put("groupTitle", groupTitle)
                        notificationData.put("authorName", authorName)
                        notificationData.put("authorUID", authorUID)
                        notification.put("to", fcmToken)
                        notification.put("data", notificationData)

                        callApi(notification)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error retrieving FCM tokens: $e")
            }

    }



    private fun callApi(jsonObject: JSONObject) {


// Generate an access token for FCM
        var token :String

        Log.i("beforeSend","inside callAPi")
        val JSON = "application/json".toMediaType()
        val client = OkHttpClient()


        WelcomeActivity().getFcmToken {
            if (it != null) {
                if(it.isNotBlank() && it.isNotBlank()){
                    token = it
                    Log.i("beforeSend","inside callAPi token$token")
                }
            }
        }

        val body = jsonObject.toString().toRequestBody(JSON)
        val url = "https://fcm.googleapis.com/fcm/send"
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header(name = "Authorization","Bearer AAAAlM4gQ_E:APA91bHUCWkcutsNbe-f8djemRyNbsnTC9Gr57f5L8VTmTHuu-ymGtbEIUkACu-IQYrFxH8Uv5aZmyrYhKAMqvhZeT8X471DS1eRQWVqYN7C-jf4hQBJi-0kYTWDeN6XOfsDOWyD2AiS")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("HttpFailure",e.message.toString())
                Log.i("HttpFailure",call.request().toString())
            }
            override fun onResponse(call: Call, response: Response) {
                Log.i("response", response.message)
                response.body?.toString()?.let { Log.i("response", it) }
                response.body?.close()
                Log.i("beforeSend","inside callAPi onresponse")
            }
        })
    }
}


