package com.chamberly.chamberly.presentation.viewmodels

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import java.util.Calendar
import java.util.Date

class ChamberViewModel(application: Application): AndroidViewModel(application = application) {

    private val _chamberState = MutableLiveData<ChamberState>()
    val chamberState: LiveData<ChamberState> = _chamberState

    private val _messages = MutableLiveData<MutableMap<String, MutableList<Message>>>()
    val messages: LiveData<MutableMap<String, MutableList<Message>>> = _messages
private val storage = Firebase.storage
    private val realtimeDatabase = Firebase.database
    private val firestore = Firebase.firestore
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val sharedPreferences =
        application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    val memberNames: MutableMap<String, String> = mutableMapOf()
    var otherUserNotificationKey: String = ""
    private var messageUpdateListener: ChildEventListener? = null
    private var messagesQuery: com.google.firebase.database.Query? = null

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
                                 //   message.message_content = "Images are not available to display on Android"
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
                         //   message.message_content = "Images are not available to display on Android."
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
                         //   message.message_content = "Images are not available to display on Android"
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
     fun getChamberMetadata(callback: (result: List<List<String>>) -> Unit) {
        val membersRef = realtimeDatabase.getReference().child(_chamberState.value!!.chamberID).child("users").child("members")
        val result : MutableList<MutableList<String>> = mutableListOf()
        membersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                for (memberSnapshot in dataSnapshot.children) {
                  Log.d("value",memberSnapshot.toString())
                    val memberList : MutableList<String> = mutableListOf()
                    val memberId = memberSnapshot.key
                    val memberName = memberSnapshot.child("name").value as? String
                    //result[memberName] = memberId
                    Log.i("memberId",memberId.toString())
                    memberList.add(memberId.toString())
                    memberList.add(memberName.toString())
                    result.add(memberList)
                }
                getChatMemberRatings(result, callback)


            }
            override fun onCancelled(databaseError: DatabaseError) {
                println("Database error: ${databaseError.message}")
            }
        })

    }

    private fun getChatMemberRatings(chatMemberList: List<MutableList<String>>, callback: (result: List<List<String>>) ->Unit) {
        var temp = chatMemberList
        var count =0
        for (memberInfo in temp) {
            firestore.collection("StarReviews")
                .whereEqualTo("To", memberInfo[0])
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.documents[0]
                        val averageStars = document.getDouble("AverageStars")
                        if (averageStars != null)
                            memberInfo.add(averageStars.toString())
                        else
                            memberInfo.add("0.0")

                        val reviewsCount = document.getLong("ReviewsCount")
                        if (reviewsCount != null)
                            memberInfo.add(reviewsCount.toString())
                        else
                            memberInfo.add("0")

                        Log.i("afterValue",count.toString())

                    }else
                    {
                        memberInfo.add("0.0")
                        memberInfo.add("0")

                    }
                    count++

                       if(count==temp.size)callback(chatMemberList)
                }
                .addOnFailureListener { exception ->
                    // Handle any errors here
                    println("Error getting documents: $exception")
                }
        }
    }


}