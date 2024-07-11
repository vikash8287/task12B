package com.chamberly.chamberly.presentation.viewmodels

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chamberly.chamberly.OkHttpHandler
import com.chamberly.chamberly.models.Message
import com.chamberly.chamberly.models.UserRatingModel
import com.chamberly.chamberly.models.toMap
import com.chamberly.chamberly.presentation.states.ChamberState
import com.chamberly.chamberly.utils.logEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import java.util.Calendar
import java.util.Date

class ChamberViewModel(application: Application): AndroidViewModel(application = application) {

    private val _chamberState = MutableLiveData<ChamberState>()
    val chamberState: LiveData<ChamberState> = _chamberState

    private val _messages = MutableLiveData<MutableMap<String, MutableList<Message>>>()
    val messages: LiveData<MutableMap<String, MutableList<Message>>> = _messages

    var messageLimitCount: Long = 40L

    private val realtimeDatabase = Firebase.database
    private val firestore = Firebase.firestore
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val sharedPreferences =
        application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    val memberNames: MutableMap<String, String> = mutableMapOf()
    var otherUserNotificationKey: String = ""
    private var messageUpdateListener: ChildEventListener? = null
    private var messagesQuery: com.google.firebase.database.Query? = null

    init {
        realtimeDatabase
            .reference
            .child("UX_Android/numberOfMessagesInChamberLimit")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageLimitCount =
                        try { snapshot.value as Long }
                        catch (_: Exception) { 40L }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Not needed for now
                }
            })
    }

    fun setChamber(chamberID: String, UID: String) {
        if(chamberID.isBlank()) {
            return
        }
        realtimeDatabase
            .reference
            .child(chamberID)
            .get()
            .addOnSuccessListener { chamberSnapshot ->
                val data = chamberSnapshot.value as Map<String, Any>
                val users = data["users"] as Map<String, Any>
                val members = users["members"] as Map<String, Any>
                _chamberState.value = ChamberState(
                    chamberID = chamberID,
                    chamberTitle = data["title"].toString(),
                    members = members.keys.toList()
                )
                for (member in members) {
                    memberNames[member.key] =
                        try { (member.value as Map<String, Any>)["name"].toString() }
                        catch (_: Exception) { "User" }
                    if(member.key != UID) {
                        realtimeDatabase
                            .reference
                            .child("$chamberID/users/members/${member.key}/notificationKey")
                            .addValueEventListener(object: ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val notificationKey = snapshot.value as? String
                                    otherUserNotificationKey = notificationKey ?: ""
                                }

                                override fun onCancelled(error: DatabaseError) { }

                            })
                    }
                }
                removeNotificationKey(UID)
                getMessages()
            }
    }

    private fun getMessages() {
        messagesQuery =
            realtimeDatabase
                .getReference(_chamberState.value!!.chamberID)
                .child("messages")
                .orderByKey()
                .limitToLast(40)

        messagesQuery!!.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _messages.value = mutableMapOf()
                for(childSnapshot in snapshot.children) {
                    if (childSnapshot.value is Map<*, *>) {
                        try {
                            val message = childSnapshot.getValue(Message::class.java)
                            if (message != null) {
                                if (message.message_type == "custom" && message.message_content == "gameCard") {
                                    message.message_content = message.game_content
                                } else if (message.message_type == "photo") {
                                    message.message_content = "Images are not available to display on Android"
                                }
                                addMessage(message)
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                //Network error, will handle later
            }
        })

        messageUpdateListener = object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val message = snapshot.getValue(Message::class.java)
                    if (message != null) {
                        if (
                            message.message_type == "custom" &&
                            message.message_content == "gameCard"
                        ) {
                            message.message_content = message.game_content
                        } else if (message.message_type == "photo") {
                            message.message_content = "Images are not available to display on Android."
                        }
                        if((messages.value!![chamberState.value!!.chamberID]?.contains(message)) != true) {
                            addMessage(message = message)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val message = snapshot.getValue(Message::class.java)
                    if (message != null) {
                        if (
                            message.message_type == "custom" &&
                            message.message_content == "gameCard"
                        ) {
                            message.message_content = message.game_content
                        } else if (message.message_type == "photo") {
                            message.message_content = "Images are not available to display on Android"
                        }
                        changeMessage(message)
                    }
                } catch (e: Exception) {
                    Log.e("Message update error", e.message.toString())
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                try {
                    val message = snapshot.getValue(Message::class.java)
                    if(message != null) {
                        removeMessage(message)
                    }
                } catch (e: Exception) {
                    Log.e("Message deletion error", e.message.toString())
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Not needed for now
            }

            override fun onCancelled(error: DatabaseError) {
                // Not needed for now
            }
        }

        messagesQuery!!
            .addChildEventListener(messageUpdateListener!!)
    }

    fun sendMessage(
        message: Message,
        successCallback: () -> Unit = {}
    ) {

        val messageRef = realtimeDatabase
            .getReference(_chamberState.value!!.chamberID)
            .child("messages")

        val messageID = messageRef.push().key
        message.message_id = messageID!!
        messageRef
            .child(messageID)
            .setValue(message.toMap())
            .addOnSuccessListener {
                successCallback()
                updateChamberDataFields()
            }

        logEventToAnalytics(
            eventName = "message_sent",
            params = hashMapOf(
                "groupChatId" to chamberState.value!!.chamberID
            )
        )
    }

    private fun addMessage(message: Message) {
        val updatedMessages = _messages.value!!
        if(updatedMessages[chamberState.value!!.chamberID] == null) {
            updatedMessages[chamberState.value!!.chamberID] = mutableListOf()
        }
        updatedMessages[chamberState.value!!.chamberID]?.add(message)
        Handler(Looper.getMainLooper()).postDelayed({
            _messages.postValue(updatedMessages)
        }, 400)
//        _messages.postValue(updatedMessages)
    }

    private fun changeMessage(message: Message) {
        val updatedMessages = _messages.value!!
        val index = updatedMessages[chamberState.value!!.chamberID]!!.indexOfFirst {
            it.message_id == message.message_id
        }

        if (index != -1) {
            updatedMessages[chamberState.value!!.chamberID]!![index] = message
            _messages.postValue(updatedMessages)
        }
    }

    private fun removeMessage(message: Message) {
        val updatedMessages = _messages.value!!
        val index = updatedMessages[chamberState.value!!.chamberID]!!.indexOfFirst {
            it.message_id == message.message_id
        }
        if(index != -1) {
            updatedMessages[chamberState.value!!.chamberID]!!.removeAt(index)
            _messages.postValue(updatedMessages)
        }
    }

    fun react(messageId: String, reaction: String) {
        realtimeDatabase
            .reference
            .child(_chamberState.value!!.chamberID)
            .child("messages")
            .child(messageId)
            .child("reactedWith")
            .setValue(reaction)
    }

    fun rateUser(
        userToRate: String,
        starRating: Double,
        UID: String
    ) {
        var reviewCountFlag = true
        var starRatingChange = starRating

        val collectionRef = firestore.collection("StarReviews")

        val review = UserRatingModel(
            from = UID,
            to = userToRate,
            stars = starRating
        )

        collectionRef
            .whereEqualTo("To", userToRate)
            .whereEqualTo("From", UID)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userReview = documents.documents[0]
                    starRatingChange -= userReview.getDouble("Stars")!!
                    reviewCountFlag = false
                }

                collectionRef
                    .whereEqualTo("To", userToRate)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { previousRatings ->
                        val latestReview = previousRatings.documents.getOrNull(0)
                        var totalStars = latestReview?.getDouble("TotalStars") ?: 0.0
                        var reviewsCount = latestReview?.getLong("ReviewsCount") ?: 0

                        totalStars += starRatingChange

                        if(reviewCountFlag) {
                            reviewsCount++
                        }

                        logEventToAnalytics(
                            eventName = "user_rated",
                            params = hashMapOf(
                                "user_rated" to starRating
                            )
                        )
                        review.totalStars = totalStars
                        review.reviewCount = reviewsCount.toInt()
                        review.averageStars = totalStars / reviewsCount
                        collectionRef.add(review.toMap())
                    }
            }
    }

    fun reportUser(report: HashMap<String, Any>) {
        firestore
            .collection("Reports")
            .add(report)
            .addOnSuccessListener {
                if(report["selfReport"] as Boolean) {
                    Toast.makeText(
                        getApplication(),
                        "User reported",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (report["reason"] == "Sexual Behaviour") {
                    //Restrict the other user
                    //This reason won't be encountered in a self report so report["UID"] is to be banned
                    firestore
                        .collection("Accounts")
                        .document(report["by"].toString())
                        .update("timestamp", FieldValue.serverTimestamp())
                        .addOnSuccessListener {
                            // This delay is to ensure that the timestamp is updated properly
                            // in firestore, removing it may cause timestamp to be null
                            Handler(Looper.getMainLooper()).postDelayed({
                                firestore
                                    .collection("Accounts")
                                    .document(report["by"].toString())
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        val timestamp =
                                            try { snapshot.getDate("timestamp") as Date }
                                            catch (_: Exception) { null }
                                        if(timestamp != null) {
                                            val calendar = Calendar.getInstance()
                                            calendar.time = timestamp
                                            calendar.add(Calendar.YEAR, 100)
                                            val restrictedUntil = calendar.time
                                            firestore
                                                .collection("Restrictions")
                                                .document(report["against"].toString())
                                                .update("restrictedUntil", restrictedUntil)
                                        }
                                    }
                            }, 500)
                        }
                }
            }
            .addOnFailureListener { }
    }

    fun sendExitMessage(message: Message, callback: () -> Unit) {
        val messageRef =
            realtimeDatabase
                .getReference(chamberState.value!!.chamberID)
                .child("messages")
        val messageId = messageRef.push().key!!

        messageRef
            .child(messageId)
            .setValue(message.copy(message_id = messageId).toMap())
            .addOnSuccessListener { callback() }
    }

    fun exitChamber(UID: String) {
        val chamberID = chamberState.value!!.chamberID
        val myChambersRef =
            firestore
                .collection("MyChambers")
                .document(UID)
        realtimeDatabase
            .reference
            .child("${chamberID}/users/members/$UID")
            .removeValue()
        myChambersRef
            .get()
            .addOnSuccessListener { chamberSnapshot ->
                val data = chamberSnapshot.data
                if(data != null) {
                    val myChambersN = (data["MyChambersN"] as Map<String, Any>).toMutableMap()
                    myChambersN.remove(chamberID)
                    myChambersRef.update("MyChambersN", myChambersN)
                }
            }

        logEventToAnalytics(eventName = "ended_chat")
    }

    fun clear(uid: String, notificationKey: String) {
        addNotificationKey(uid, notificationKey)
        _messages.value?.clear()
        _chamberState.value = ChamberState(
            chamberID = "",
            chamberTitle = ""
        )
        messageUpdateListener?.let {
            messagesQuery?.removeEventListener(it)
        }
    }

    private fun sendNotification(token: String) {
        val notificationPayload = JSONObject()
        val dataPayload = JSONObject()
        try {
            notificationPayload.put(
                "title",
                sharedPreferences.getString("displayName", "")
            )
            notificationPayload.put(
                "body",
                "sent you a message"
            )
            dataPayload.put(
                "groupChatId",
                _chamberState.value!!.chamberID
            )
            OkHttpHandler(
                getApplication() as Context,
                token,
                notification = notificationPayload,
                data = dataPayload
            ).executeAsync()
        } catch (e: Exception) {
            Log.e("Error sending notifications", e.message.toString())
        }
    }

    private fun updateChamberDataFields() {
        val members = chamberState.value!!.members
        val selfUID = Firebase.auth.currentUser!!.uid

        val otherUID =
            if(members.size > 1) {
                if (members[0] == selfUID) members[1]
                else members[0]
            } else {
                ""
            }
        val selfChambersRef =
            firestore
                .collection("MyChambers")
                .document(selfUID)
        selfChambersRef
            .get()
            .addOnSuccessListener {
                val data = it.data!!
                val myChambers =
                    (data["MyChambersN"] as Map<String, Map<String, Any>>).toMutableMap()
                myChambers[chamberState.value!!.chamberID] = mapOf(
                    "groupChatId" to chamberState.value!!.chamberID,
                    "messageRead" to true,
                    "timestamp" to FieldValue.serverTimestamp(),
                )
            }
        if (otherUID.isNotBlank()) {
            val otherChambersRef = firestore
                .collection("MyChambers")
                .document(otherUID)
            otherChambersRef
                .get()
                .addOnSuccessListener {
                    val data = it.data!!
                    val myChambers =
                        (data["MyChambersN"] as Map<String, Map<String, Any>>).toMutableMap()
                    myChambers[chamberState.value!!.chamberID] = mapOf(
                        "groupChatId" to chamberState.value!!.chamberID,
                        "messageRead" to otherUserNotificationKey.isBlank(),
                        "timestamp" to FieldValue.serverTimestamp(),
                    )
                    otherChambersRef.update(
                        "MyChambersN", myChambers
                    )
                }
            if (otherUserNotificationKey.isNotBlank()) {
                sendNotification(otherUserNotificationKey)
            }
        }
    }

    fun addNotificationKey(
        UID: String,
        notificationKey: String
    ) {
        if(notificationKey.isBlank()) {
            return
        }
        if(chamberState.value != null && chamberState.value!!.chamberID.isNotBlank()) {
            realtimeDatabase
                .reference
                .child(chamberState.value!!.chamberID)
                .child("users")
                .child("members")
                .child(UID)
                .child("notificationKey")
                .setValue(notificationKey)
        }

    }

    fun removeNotificationKey(
        UID: String
    ) {
        realtimeDatabase
            .reference
            .child(_chamberState.value!!.chamberID)
            .child("users")
            .child("members")
            .child(UID)
            .child("notificationKey")
            .removeValue()
    }

    private fun logEventToAnalytics(eventName: String, params: HashMap<String, Any> = hashMapOf()) {
        params["UID"] = sharedPreferences.getString("uid", "") ?: ""
        params["name"] = sharedPreferences.getString("displayName", "") ?: ""
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = eventName,
            params = params
        )
    }


}