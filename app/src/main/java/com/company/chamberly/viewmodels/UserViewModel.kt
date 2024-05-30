package com.company.chamberly.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.company.chamberly.models.Chamber
import com.company.chamberly.models.Message
import com.company.chamberly.models.Topic
import com.company.chamberly.models.toMap
import com.company.chamberly.states.AppState
import com.company.chamberly.states.UserState
import com.company.chamberly.utils.Role
import com.company.chamberly.utils.logEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserViewModel(application: Application): AndroidViewModel(application = application) {

    private val _userState = MutableLiveData<UserState>()
    val userState: LiveData<UserState> = _userState

    private val _appState = MutableLiveData<AppState>()
    val appState: LiveData<AppState> = _appState

    private val _chamberID = MutableLiveData<String>()
    val chamberID: LiveData<String> = _chamberID

    private val _maxAllowedTopics: MutableLiveData<Long> = MutableLiveData(25)
    val maxAllowedTopics: LiveData<Long> = _maxAllowedTopics

    private val _pendingTopics: MutableLiveData<MutableList<String>> =
        MutableLiveData(mutableListOf())
    val pendingTopics: LiveData<MutableList<String>> = _pendingTopics

    // This map will contain the topic title corresponding to the topic ID
    val pendingTopicTitles = mutableMapOf<String, String>()


    private val auth = Firebase.auth
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val firestore = Firebase.firestore
    private val realtimeDatabase = Firebase.database
    private val messaging = FirebaseMessaging.getInstance()

    init {
        _appState.value = AppState()
        realtimeDatabase
            .reference
            .child("UX_Android")
            .child("shouldAuthenticateUser")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isAppEnabled = (snapshot.value as Boolean?) ?: true
                    _appState.value = _appState.value?.copy(isAppEnabled = isAppEnabled)
                }

                override fun onCancelled(error: DatabaseError) {
                   // TODO: Will do later
                }

            })

        realtimeDatabase
            .reference
            .child("UX_Android")
            .child("latestAndroidAppVersion")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latestAvailableVersion = (snapshot.value as String?) ?: "1.0.0"
                    try {
                        val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                        val versionCode = pInfo.versionName
                        _appState.value = _appState.value?.copy(
                            isAppUpdated = !canUpdate(versionCode, latestAvailableVersion)
                        )
                    } catch(e: Exception) {
                        // No need to do anything for now.
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    //Most likely a network issue has occurred. Will implement later.
                }
            })

        realtimeDatabase
            .reference
            .child("UX_Android")
            .child("pendingChambersNotSubbedLimit")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _maxAllowedTopics.value = (snapshot.value as Long?) ?: 25
                }
                override fun onCancelled(error: DatabaseError) {
                    //No need to implement for now
                }
            })

        _pendingTopics.value =
            sharedPreferences
                .getString("topics", "")!!
                .split(",")
                .toMutableList()
        _pendingTopics.value?.removeAll(listOf(""))
//        attachTopicRequestListeners()
    }
    fun registerUser(
        displayName: String,
        role: Role,
        onComplete: () -> Unit
    ) {
        auth
            .signInAnonymously()
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val user = auth.currentUser
                    with(sharedPreferences.edit()) {
                        putBoolean("hasLoggedIn", true)
                        putString("uid", user!!.uid)
                        apply()
                    }
                } else {
                    Toast.makeText(
                        getApplication(),
                        "Authentication failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val user = auth.currentUser
                if(user != null) {
                    val isNewUser = sharedPreferences.getBoolean("isNewUser", true)
                    with(sharedPreferences.edit()) {
                        putString("uid", user.uid)
                        putString("displayName", displayName)
                        putBoolean("isListener", role == Role.LISTENER)
                        apply()
                    }
                    createDisplayNameDocument(
                        displayName = displayName,
                        uid = user.uid
                    )
                    createAccountDocument(
                        displayName = displayName,
                        uid = user.uid,
                        role = role
                    )
                    setRestriction(
                        uid = user.uid
                    )
                    loginUser()
                onComplete()
            }
        }
    }

    private fun createDisplayNameDocument(
        displayName: String,
        uid: String,
    ) {
        val displayNameData = mapOf(
            "Display_Name" to displayName,
            "Email" to "$uid@chamberly.net",
            "UID" to uid
        )
        firestore
            .collection("Display_Names")
            .document(displayName)
            .set(displayNameData)
            .addOnCompleteListener {
                Toast.makeText(
                    getApplication(),
                    "Welcome to Chamberly!",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }

    private fun createAccountDocument(
        displayName: String,
        uid: String,
        role: Role
    ) {
        val account = mapOf(
            "UID" to uid,
            "Display_Name" to displayName,
            "Email" to "$uid@chamberly.net",
            "platform" to "android",
            "isModerator" to false,
            "timestamp" to FieldValue.serverTimestamp(),
            "selectedRole" to role.toString()
        )

        firestore
            .collection("Accounts")
            .document(uid)
            .set(account)
    }

    private fun setRestriction(uid: String) {
        firestore
            .collection("Restrictions")
            .document(uid)
            .set(mapOf(
                "UID" to uid,
                "restrictedUntil" to FieldValue.serverTimestamp()
            ))
    }

    fun loginUser(): Boolean {
        val hasLoggedIn = sharedPreferences.getBoolean("hasLoggedIn", false)
        return if(!hasLoggedIn || auth.currentUser == null) {
            false
        } else {
            val uid = sharedPreferences.getString("uid", "") ?: ""
            val displayName = sharedPreferences.getString("displayName", "") ?: ""
            val role =
                if (sharedPreferences.getBoolean("isListener", false)) { Role.LISTENER }
                else { Role.VENTOR }
            _userState.value = UserState(
                UID = uid,
                displayName = displayName,
                role = role
            )
            setNotificationToken()
            getUserRating()
            true
        }
    }

    private fun setNotificationToken() {
        messaging.token.addOnCompleteListener { task ->
            if(!task.isSuccessful) {
                Log.w("FCMTOKEN", "FCMTOKEN fetch failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            with(sharedPreferences.edit()) {
                putString("notificationKey", token)
                apply()
            }
            _userState.value = _userState.value!!.copy(notificationKey = token)

        }
        messaging.isAutoInitEnabled = true
    }

    fun createChamber(
        chamberTitle: String,
        callback: () -> Unit = {}
    ) {
        val chamber = Chamber(
            AuthorName = userState.value!!.displayName,
            AuthorUID = userState.value!!.UID,
            groupTitle = chamberTitle
        )

        val chamberRef =
            firestore
                .collection("GroupChatIds")
                .document()

        chamber.groupChatId = chamberRef.id

        chamberRef.set(chamber.toMap())
            .addOnSuccessListener {
                val chamberDataRef =
                    realtimeDatabase.getReference(chamber.groupChatId)

                chamberDataRef.child("host").setValue(chamber.AuthorUID)
                chamberDataRef.child("messages").push().setValue("")
                chamberDataRef.child("timestamp").setValue(ServerValue.TIMESTAMP)

                chamberRef.update("members", FieldValue.arrayUnion(userState.value!!.UID))
                firestore
                    .collection("Accounts")
                    .document(userState.value!!.UID)
                    .update("chambers", FieldValue.arrayUnion(chamber.groupChatId))

                chamberDataRef
                    .child("users")
                    .child("members")
                    .child(userState.value!!.UID)
                    .child("name")
                    .setValue(userState.value!!.displayName)
                    .addOnSuccessListener {
                        callback()
                        openChamber(chamberID = chamber.groupChatId)
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                    }
            }
    }

    fun joinChamber(chamberID: String) {
        val chamberDataRef = realtimeDatabase
            .reference
            .child(chamberID)

        val systemMessage = Message(
            "Chamberly",
            "${userState.value!!.displayName} joined the chat. \n\nPlease be patient for others to respond :)",
            "system",
            "Chamberly"
        )

        firestore
            .collection("GroupChatIds")
            .document(chamberID)
            .update(
                "locked", true,
                "members", FieldValue.arrayUnion(userState.value!!.UID)
            )

        chamberDataRef
            .child("messages")
            .push()
            .setValue(systemMessage)
            .addOnSuccessListener {
                chamberDataRef
                    .child("users")
                    .child("members")
                    .child(userState.value!!.UID)
                    .child("name")
                    .setValue(userState.value!!.displayName)
                    .addOnSuccessListener {
                        firestore
                            .collection("Accounts")
                            .document(userState.value!!.UID)
                            .update("chambers", FieldValue.arrayUnion(chamberID))
                        openChamber(chamberID)
                    }
            }
    }

    fun isChamberVacant(
        members: List<String>,
        membersLimit: Int
    ): Boolean {
        return members.size < membersLimit && !members.contains(userState.value!!.UID)
    }

    fun createTopic(
        topicTitle: String,
        callback: () -> Unit = {}
    ) {
        val topic = Topic(
            AuthorName = userState.value!!.displayName,
            AuthorUID = userState.value!!.UID,
            TopicTitle = topicTitle,
            weight = 60,
        )

        val collectionRef = firestore.collection("TopicIds")
        val topicRef = collectionRef.document()
        topic.TopicID = topicRef.id

        topicRef.set(topic.toMap())
            .addOnSuccessListener {
                val topicDataRef =
                    realtimeDatabase
                        .getReference(topic.TopicID)
                topicDataRef.child("authorUID").setValue(topic.AuthorUID)
                topicDataRef.child("authorName").setValue(topic.AuthorName)
                topicDataRef.child("timestamp").setValue(ServerValue.TIMESTAMP)
                topicDataRef.child("topicTitle").setValue(topic.TopicTitle)
                val userRef = topicDataRef.child("users").child(topic.AuthorUID)
                userRef.child("isReserved").setValue(false)
                userRef.child("lfl").setValue(userState.value!!.role == Role.VENTOR)
                userRef.child("lfv").setValue(userState.value!!.role == Role.LISTENER)
                userRef.child("isAndroid").setValue(true)
                userRef.child("isSubbed").setValue(false)
                userRef.child("restricted").setValue(false)
                userRef.child("notificationKey").setValue(userState.value!!.notificationKey)
                userRef.child("penalty").setValue(0)
                userRef.child("timestamp").setValue(ServerValue.TIMESTAMP)
//                userRef.child("blockedUsers").setValue()

                _pendingTopics.value!!.add(topic.TopicID)
                with(sharedPreferences.edit()) {
                    putString("topics",pendingTopics.value!!.joinToString(","))
                    apply()
                }
                logEventToAnalytics("topic_started")
                logEventToAnalytics("Started_Procrastinating")
                callback()
            }
    }
    fun waitOnTopic(topicID: String) {
        _pendingTopics.value!!.add(topicID)
        with(sharedPreferences.edit()) {
            putString("topics", pendingTopics.value!!.joinToString(","))
            apply()
        }
        val userData = mapOf(
            "isAndroid" to true,
            "isReserved" to false,
            "isSubscribed" to true,
            "notificationKey" to (userState.value?.notificationKey ?: ""),
            "lfl" to (userState.value!!.role == Role.VENTOR),
            "lfv" to (userState.value!!.role == Role.LISTENER),
            "penalty" to 0,
            "timestamp" to ServerValue.TIMESTAMP
        )
        realtimeDatabase
            .reference
            .child(topicID)
            .child("users")
            .child(userState.value!!.UID)
            .setValue(userData)

        logEventToAnalytics("swiped_right_${userState.value!!.role}")
        logEventToAnalytics("swiped_on_card")
        logEventToAnalytics("Started_procrastinating")
    }

    fun dismissTopic() {
        logEventToAnalytics("swiped_left")
        logEventToAnalytics("swiped_on_card")
    }
    private fun attachTopicRequestListeners() {
//        Log.d("Pending topics", savedTopics.toString())
        for (topic in _pendingTopics.value!!) {
            if (topic.isBlank()) {
                continue
            }
            val topicRef = realtimeDatabase.reference.child(topic)
            val userRef = topicRef.child("users").child(_userState.value!!.UID)

            userRef
                .child("isReserved")
                .addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isReserved = snapshot.value as Boolean?
                        if(isReserved == null) {
                            _pendingTopics.value!!.remove(topic)
                            with(sharedPreferences.edit()) {
                                putString(
                                    "topics",
                                    _pendingTopics.value!!.joinToString(",")
                                )
                                apply()
                            }
                        } else {
//                            userRef.child("restricted").setValue(false)
                            if (isReserved) {
                                var topicTitle = ""
                                var reserverID = ""
                                topicRef.child("topicTitle")
                                    .get()
                                    .addOnSuccessListener { topicTitleSnapshot ->
                                        topicTitle = topicTitleSnapshot.value.toString()
                                        topicRef
                                            .child("users")
                                            .child(userState.value!!.UID)
                                            .child("reservedBy")
                                            .get()
                                            .addOnSuccessListener { reservedBySnapshot ->
                                                reserverID = reservedBySnapshot.value.toString()
                                            }
                                    }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) { }

                })
        }
    }

    private fun getUserRating() {
        val isRestricted = sharedPreferences.getBoolean("isRestricted", false)
        firestore
            .collection("StarReviews")
            .whereEqualTo("To", _userState.value!!.UID)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener {documents ->
                if(!documents.isEmpty) {
                    val review = documents.documents[0]
                    val userRating = (review.getDouble("AverageStars") ?: 0.0).toFloat()
                    val ratingsCount = (review.getLong("ReviewsCount") ?: 0).toInt()
                    _userState.value = _userState.value!!.copy(
                        userRating = userRating,
                        numRatings = ratingsCount
                    )
                    if (shouldBanUser(userRating = userRating, ratingsCount = ratingsCount)) {
                        if(!isRestricted) {
                            logEventToAnalytics("star_reviews_auto_restriction")
                            with(sharedPreferences.edit()) {
                                putBoolean("isRestricted", true)
                                apply()
                            }
                            banUser()
                        }
                    } else if(isRestricted) {
                        unbanUser()
                    }
                }
            }
    }

    fun setRole(role: Role) {
        val selectedRole =
            if (sharedPreferences.getBoolean("isListener", false)) { Role.LISTENER }
            else { Role.VENTOR }
        if (role != selectedRole) {
            val currentUserRef = firestore
                .collection("Accounts")
                .document(_userState.value!!.UID)
            currentUserRef
                .update("selectedRole",
                    if(role == Role.LISTENER) { "listener" }
                    else { "ventor" }
                )
            with(sharedPreferences.edit()) {
                putBoolean("isListener", role == Role.LISTENER)
                apply()
            }
            _userState.value = _userState.value!!.copy(role = role)
            stopProcrastination()
        }
    }

    fun stopProcrastination(callback: () -> Unit = {}) {
        val topicsList = sharedPreferences.getString("topics", "")!!.split(",")
        for(topic in topicsList) {
            if(topic.isNotBlank()) {
                realtimeDatabase
                    .reference
                    .child(topic)
                    .child("users")
                    .child(_userState.value!!.UID)
                    .removeValue()
            }
        }
        _pendingTopics.value!!.clear()
        with(sharedPreferences.edit()) {
            remove("topics")
            apply()
        }
        callback()
    }

    fun openChamber(chamberID: String) {
        _chamberID.value = chamberID
    }

    fun closeChamber() {
        _chamberID.value = ""
    }

    fun blockUser(UID: String) {
        val topicsList =
            (sharedPreferences.getString("topics", "") ?: "")
                .split(",")

        val currUserRef = firestore
            .collection("Accounts")
            .document(userState.value!!.UID)
        currUserRef.update("blockedUsers", FieldValue.arrayUnion(UID))
        currUserRef.get().addOnSuccessListener { userData ->
            val blockedUsers =
                (userData.data!!["blockedUsers"] as MutableList<String>?)
                    ?: mutableListOf()

            if (!blockedUsers.contains(UID)) {
                blockedUsers.add(UID)

                currUserRef.update(mapOf("blockedUsers" to blockedUsers))

                for(topic in topicsList) {
                    if(topic.isNotBlank()) {
                        realtimeDatabase.reference
                            .child(topic)
                            .child("users")
                            .child(userState.value!!.UID)
                            .child("blockedUsers")
                            .setValue(blockedUsers)
                    }
                }
            }
        }
    }

    private fun shouldBanUser(userRating: Float, ratingsCount: Int): Boolean {
        return userRating <= 3.5f && ratingsCount >= 6
    }

    private fun banUser() {

    }

    private fun unbanUser() {

    }

    fun deleteAccount() {
        val user = auth.currentUser
        logEventToAnalytics(eventName = "account_deleted")
        if (user != null) {
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    with(sharedPreferences.edit()) {
                        clear()
                        putBoolean("isNewUser", false)
                        apply()
                    }
                    Toast.makeText(getApplication(), "Account deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Failed to delete account", Toast.LENGTH_SHORT).show()
                }
                _userState.value = UserState()
            }
        } else {
            Toast.makeText(getApplication(), "Account deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canUpdate(
        currentVersionCode: String,
        latestVersionCode: String
    ): Boolean {
        val currentVersion = currentVersionCode.split(".").map { it.toInt() }
        val latestVersion = latestVersionCode.split(".").map { it.toInt() }
        for(i in 0..2) {
            if (currentVersion[i] > latestVersion[i]) {
                return false
            } else if (currentVersion[i] < latestVersion[i]) {
                return true
            }
        }
        return false
    }

    fun logEventToAnalytics(eventName: String, params: HashMap<String, Any> = hashMapOf()) {
        params["UID"] = _userState.value!!.UID
        params["name"] = _userState.value!!.displayName
        logEvent(
            firebaseAnalytics = firebaseAnalytics,
            eventName = eventName,
            params = params
        )
    }
}