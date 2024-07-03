package com.chamberly.chamberly.presentation.viewmodels

import android.app.Application
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chamberly.chamberly.OkHttpHandler
import com.chamberly.chamberly.models.Message
import com.chamberly.chamberly.models.UserRatingModel
import com.chamberly.chamberly.models.toMap
import com.chamberly.chamberly.presentation.states.ChamberState
import com.chamberly.chamberly.utils.compressImageFile
import com.chamberly.chamberly.utils.logEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.json.JSONObject

class ChamberViewModel(application: Application): AndroidViewModel(application = application) {

    private val _chamberState = MutableLiveData<ChamberState>()
    val chamberState: LiveData<ChamberState> = _chamberState
    private val _messages = MutableLiveData<MutableList<Message>>()
    val messages: LiveData<List<Message>> = _messages as LiveData<List<Message>>
private val storage = Firebase.storage
    private val realtimeDatabase = Firebase.database
    private val firestore = Firebase.firestore
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val sharedPreferences =
        application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    val memberNames: MutableMap<String, String> = mutableMapOf()

    fun setChamber(chamberID: String, UID: String) {
        Log.d("CHAMBER1", chamberID)
        firestore.collection("GroupChatIds")
            .document(chamberID)
            .get()
            .addOnSuccessListener { chamberSnapshot ->
                Log.d("CHAMBER1", chamberSnapshot.data.toString())
                if(chamberSnapshot.data != null) {
                    _chamberState.value = getChamberFromSnapshot(chamberSnapshot.data!!)
                    Log.d("CHAMBER", _chamberState.value.toString())
                    for (member in chamberState.value!!.members) {
                        firestore
                            .collection("Accounts")
                            .document(member)
                            .get()
                            .addOnSuccessListener { memberSnapshot ->
                                memberNames[member] =
                                    memberSnapshot.data?.get("Display_Name").toString()
                            }
                    }
                    removeNotificationKey(UID)
                    getMessages()
                }
            }
            .addOnFailureListener {
                Log.d("CHAMBEREXCEPTION", it.toString())
            }
    }

    private fun getChamberFromSnapshot(chamberData: Map<String, Any>): ChamberState {
        return ChamberState(
            chamberID = chamberData["groupChatId"].toString(),
            chamberTitle = chamberData["groupTitle"].toString(),
            members = chamberData["members"] as List<String>
        )
    }

    private fun getMessages() {
        val messagesQuery =
            realtimeDatabase
                .getReference(_chamberState.value!!.chamberID)
                .child("messages")
                .orderByKey()
                .limitToLast(40)

        messagesQuery.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _messages.value = mutableListOf()
                for(childSnapshot in snapshot.children) {
                    if (childSnapshot.value is Map<*, *>) {
                        try {
                            val message = childSnapshot.getValue(Message::class.java)
                            if (message != null) {
                                if (message.message_type == "custom" && message.message_content == "gameCard") {
                                    message.message_content = message.game_content
                                } else if (message.message_type == "photo") {
                                    //message.message_content = "Images are not available to display on Android"
                                }
                                addMessage(message)
                            }
                        } catch (e: Exception) {
                            Log.d("EXCEPTION", e.toString())
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // TODO: Network error, will handle later
            }
        })

        messagesQuery
            .addChildEventListener(object: ChildEventListener {
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
                            //TODO:do  some stuff
                            //   message.message_content = "Images are not available to display on Android."
                            }
                            if(!(messages.value!!.contains(message))) {
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

                            //    message.message_content = "Images are not available to display on Android"
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
            })
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
                sendNotificationToInactiveMembers()
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
        updatedMessages.add(message)
        _messages.postValue(updatedMessages)
    }

    private fun changeMessage(message: Message) {
        val updatedMessages = _messages.value!!
        val index = updatedMessages.indexOfFirst {
            it.message_id == message.message_id
        }

        if (index != -1) {
            updatedMessages[index] = message
            _messages.postValue(updatedMessages)
        }
    }

    private fun removeMessage(message: Message) {
        val updatedMessages = _messages.value!!
        val index = updatedMessages.indexOfFirst {
            it.message_id == message.message_id
        }
        if(index != -1) {
            updatedMessages.removeAt(index)
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
            }
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
        firestore
            .collection("GroupChatIds")
            .document(chamberState.value!!.chamberID)
            .update("members", FieldValue.arrayRemove(UID))
            .addOnSuccessListener {
                realtimeDatabase
                    .reference
                    .child(chamberState.value!!.chamberID)
                    .child("users")
                    .child("members")
                    .child(UID)
                    .removeValue()
            }

        logEventToAnalytics(eventName = "ended_chat")
    }

    fun clear(uid: String, notificationKey: String) {
        addNotificationKey(uid, notificationKey)
        Log.d("HERE", "VIEWMODEL CLEARED")
        _messages.value?.clear()
        _chamberState.value = ChamberState(
            chamberID = "",
            chamberTitle = ""
        )
    }


    private fun sendNotificationToInactiveMembers() {
        val notificationKey =
            sharedPreferences
                .getString("notificationKey", "") ?: ""
        val notificationPayload = JSONObject()
        val dataPayload = JSONObject()
        realtimeDatabase
            .reference
            .child(_chamberState.value!!.chamberID)
            .child("users")
            .child("members")
            .get()
            .addOnSuccessListener { membersSnapshot ->
                for(snapshot in membersSnapshot.children) {
                    val token = snapshot.child("notificationKey").value as String?
                    if(!token.isNullOrBlank() && token != notificationKey) {
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
                }
            }
    }

    fun addNotificationKey(
        UID: String,
        notificationKey: String
    ) {
        if(chamberState.value != null && chamberState.value!!.chamberID.isNotBlank()) {
            Log.d("CHAMBER ID", chamberState.value!!.toString())
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
    private  fun getCurrentDateInCestTimeZone():String{
        val now = Clock.System.now()
        val timeZone = TimeZone.of("Europe/Berlin")
        val now_in_cest = now.toLocalDateTime(timeZone)
        val day = now_in_cest.dayOfMonth.toString().padStart(2, '0')
        val month = now_in_cest.month.toString() // Use shortName for abbreviated month name
        val year = now_in_cest.year.toString()
        val hours = now_in_cest.hour.toString().padStart(2, '0')
        val minutes = now_in_cest.minute.toString().padStart(2, '0')
        val seconds = now_in_cest.second.toString().padStart(2, '0')
        val timezone  = "CEST"

        val formattedDateTime = "$day $month $year $hours:$minutes:$seconds $timezone"
        return formattedDateTime
    }
    fun postImage(uri: Uri,UID: String,senderName: String){
        val todayDateInCest = getCurrentDateInCestTimeZone()
        val messageId =   addImageInfoToRealtimeDatabase(todayDateInCest,UID,senderName)
        CoroutineScope(Dispatchers.Default).launch {
            compressImageAndUploadItToStorage(uri = uri,messageId,getApplication<Application>().applicationContext,UID)
        }

    }
    private suspend fun compressImageAndUploadItToStorage(uri: Uri, messageId: String,context: Context,UID: String) {

        val fileDescriptor: AssetFileDescriptor =
            context.contentResolver.openAssetFileDescriptor(uri, "r")!!
        val fileSize = fileDescriptor.getLength()
        fileDescriptor.close()
        if (fileSize > 400000) {
            val tempImageFile = CoroutineScope(Dispatchers.Default).async {
                compressImageFile(context = context, uri) {
                    it.resolution(1280, 720)
                    it.quality(80)
                    it.size(400000)
                }
            }
            postImageToStorage(tempImageFile.await().toUri(),messageId,UID)
        } else {
            postImageToStorage(uri, messageId,UID)
        }
        Log.d("size", fileSize.toString())

    }
    private fun getUrl(uri: Uri): String {

        return uri.toString()
    }
    private fun postImageToStorage(uri: Uri, messageId: String,UID: String) {
        val storageRef: StorageReference = storage.reference
        val uid = UID
        val todayDateInCest = getCurrentDateInCestTimeZone()
        val filename = "photo_message_" + uid + "_" + todayDateInCest + "_png"
        val path = storageRef.child("message_image/${filename}")

        val uploadTask = path.putFile(uri)

        uploadTask.addOnFailureListener { error(it) }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                path.downloadUrl.addOnSuccessListener {

                    updateImageUrl(getUrl(it), messageId)

                }.addOnFailureListener {
                    Log.d("FirebaseStorageError", it.message.toString())
                }
            } else {
                Log.d("FirebaseStorageError", task.exception?.toString()!!)
            }

        }

    }
    private fun updateImageUrl(imageUrl:String,messageId:String){
        // TODO:chamber state
        val ref = realtimeDatabase.getReference(chamberState.value!!.chamberID).child("messages").child(messageId).child("message_content")
        ref.setValue(imageUrl).addOnSuccessListener {
            Log.d("realtimeDatabaseImageUrlUpdate","Success")

        }.addOnFailureListener {
            Log.d("realtimeDatabaseImageUrlUpdate",it.message.toString())

        }

    }
    private fun addImageInfoToRealtimeDatabase(todayDateInCest: String,UID:String,senderName:String):String {


        val ref = realtimeDatabase.getReference(_chamberState.value!!.chamberID).child("messages")
        val key = ref.push().key
        val messageId = key!!
        val data = Message(
            UID = UID,
            message_content = "",
            message_date = todayDateInCest,
            message_id = messageId,
            message_type = "photo",
            sender_name = senderName
        )
        ref.child(messageId).setValue(data.toMap())
            .addOnSuccessListener {
                Log.i("beforeSend", "success")
            }
        return key
    }
     suspend fun compressThumbnail(uri: Uri, previewImage: ImageView, callback: () -> Unit) {


        val tempImageFile = CoroutineScope(Dispatchers.Default).async {
            compressImageFile(context = getApplication<Application>().applicationContext, uri) {
                it.resolution(100, 100)
                it.quality(30)
                it.size(40000)
            }
        }
        CoroutineScope(Dispatchers.Main).launch{

            previewImage.setImageURI(Uri.fromFile(tempImageFile.await()))
            callback()
        }
    }
}