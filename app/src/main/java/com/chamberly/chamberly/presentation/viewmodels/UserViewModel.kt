package com.chamberly.chamberly.presentation.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chamberly.chamberly.OkHttpHandler
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.Chamber
import com.chamberly.chamberly.models.Match
import com.chamberly.chamberly.models.Message
import com.chamberly.chamberly.models.Topic
import com.chamberly.chamberly.models.toMap
import com.chamberly.chamberly.presentation.states.AppState
import com.chamberly.chamberly.presentation.states.UserState
import com.chamberly.chamberly.utils.DatabaseManager
import com.chamberly.chamberly.utils.Entitlement
import com.chamberly.chamberly.utils.REVENUECAT_API_KEY
import com.chamberly.chamberly.utils.Role
import com.chamberly.chamberly.utils.TaskScheduler
import com.chamberly.chamberly.utils.logEvent
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
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class UserViewModel(application: Application): AndroidViewModel(application = application) {

    private val _userState = MutableLiveData(UserState())
    val userState: LiveData<UserState> = _userState

    private val _appState = MutableLiveData(AppState(
        isAppEnabled = true,
        isAppUpdated = true,
        areExperimentalFeaturesEnabled = false
    ))
    val appState: LiveData<AppState> = _appState

    private val _chamberID = MutableLiveData<String>()
    val chamberID: LiveData<String> = _chamberID

    private val _maxAllowedTopics: MutableLiveData<Long> = MutableLiveData(25)
    val maxAllowedTopics: LiveData<Long> = _maxAllowedTopics

    private val _pendingTopics: MutableLiveData<MutableList<String>> =
        MutableLiveData(mutableListOf())
    val pendingTopics: LiveData<MutableList<String>> = _pendingTopics

    private var _timeForPenalty: Long = 15L

    private val _blockedUsers: MutableLiveData<MutableList<String>> =
        MutableLiveData(mutableListOf())
    val blockedUsers: LiveData<MutableList<String>> = _blockedUsers

    private val _matches: MutableLiveData<MutableList<Match>> = MutableLiveData(mutableListOf())
    val matches: LiveData<MutableList<Match>> = _matches
    // This map will contain the topic title corresponding to the topic ID
    val pendingTopicTitles = mutableMapOf<String, String>()


    private val auth = Firebase.auth
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val firestore = Firebase.firestore
    private val realtimeDatabase = Firebase.database
    private val messaging = FirebaseMessaging.getInstance()
    private var currentOffering: Offering? = null
    private val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
    private var versionName: String = pInfo.versionName
    private val checkedUsers: MutableSet<String> = mutableSetOf()
    private val eligibleUsers: MutableMap<String,MutableList<MutableMap<String, Any>>> =
        mutableMapOf()
    private lateinit var databaseManager: DatabaseManager
    private val taskScheduler: TaskScheduler = TaskScheduler()
    var completedMatches: MutableLiveData<Pair<String, String>> =
        MutableLiveData(Pair("", ""))

    init {
        _pendingTopics.value =
            sharedPreferences
                .getString("topics", "")!!
                .split(",")
                .toMutableList()
        _pendingTopics.value?.removeAll(listOf(""))
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
                    showToast("Authentication failed")
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
                    if(isNewUser) {
                        logEventToAnalytics("first_time_user")
                    } else {
                        logEventToAnalytics("account_recreated")
                    }
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
                showToast("Welcome to Chamberly!")
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
            "Coins" to 0,
            "gender" to "male",
            "age" to 0,
            "biography" to "",
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
                if (sharedPreferences.getBoolean("isListener", false)) Role.LISTENER
                else Role.VENTOR
            _userState.value = UserState(
                UID = uid,
                displayName = displayName,
                role = role
            )
            databaseManager = DatabaseManager(uid, displayName)
            firestore
                .collection("Accounts")
                .document(uid)
                .get()
                .addOnSuccessListener {
                    val blockedUsers = it["blockedUsers"] as List<String>? ?: emptyList()
                    _blockedUsers.postValue(blockedUsers.toMutableList())
                }
            setupUXListeners()
            setNotificationToken()
            getUserRestrictions()
            getUserRating()
            getUserChambers()
            attachTopicRequestListeners()
            if (appState.value?.areExperimentalFeaturesEnabled == true) {
                setPaywallStatus()
            }
            true
        }
    }

    private fun setupUXListeners() {
        realtimeDatabase
            .reference
            .child("UX_Android")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data =
                        try { snapshot.value as Map<String, Any> }
                        catch (_: Exception) { emptyMap() }
                    val isAppEnabled =
                        try { data["shouldAuthenticateUser"] as Boolean }
                        catch (_: Exception) { true }
                    val latestAvailableVersion =
                        try { data["latestAndroidAppVersion"] as String }
                        catch (_: Exception) { "1.0.0" }
                    _maxAllowedTopics.postValue(
                        try { data["pendingChambersNotSubbedLimit"] as Long }
                        catch (_: Exception) { 15L }
                    )
                    _timeForPenalty =
                        try { data["timeForPenalty"] as Long }
                        catch (_: Exception) { 15L }

                    _appState.postValue(
                        _appState.value?.copy(
                            isAppEnabled = isAppEnabled,
                            isAppUpdated = !canUpdate(versionName, latestAvailableVersion)
                        )
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
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

    private fun setPaywallStatus() {
        Purchases.configure(
            PurchasesConfiguration.Builder(
                getApplication(),
                REVENUECAT_API_KEY
            )
                .appUserID(userState.value!!.UID)
                .build()
        )
        Purchases.sharedInstance.getOfferings(object: ReceiveOfferingsCallback {
            override fun onError(error: PurchasesError) {
                showToast("An error occurred while checking subscription status")
            }
            override fun onReceived(offerings: Offerings) {
                currentOffering = offerings.current
            }
        })
        Purchases.sharedInstance.getCustomerInfo(object: ReceiveCustomerInfoCallback {
            override fun onError(error: PurchasesError) {
                showToast("An error occurred while checking subscription status")
            }
            override fun onReceived(customerInfo: CustomerInfo) {
                if(customerInfo.entitlements["ChamberlyPlus"]?.isActive == true) {
                    _userState.postValue(
                        _userState.value!!.copy(entitlement = Entitlement.CHAMBERLY_PLUS)
                    )
                }
            }
        })
    }

    fun getUserChambers(
        callback: (List<Chamber>) -> Unit = {}
    ) {
        firestore
            .collection("GroupChatIds")
            .whereArrayContains("members", userState.value!!.UID)
            .limit(20)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("CHAMBERS CHECKING", querySnapshot.documents.toString())
                val chambers = querySnapshot.documents.mapNotNull {
                    it.toObject(Chamber::class.java)
                }
                Log.d("CHAMBERS CHECKING", chambers.toString())
                for (chamber in chambers) {
                    for (member in chamber.members) {
                        if (member != userState.value!!.UID) {
                            checkedUsers.add(member)
                        }
                    }
                }
                _userState.postValue(
                    _userState.value!!.copy(
                        chambers = chambers.toMutableList()
                    )
                )
                Log.d("Chambers", chambers.toString() + "\n" + checkedUsers.toString())
            }
    }

    fun createChamber(
        chamberTitle: String,
        callback: (String) -> Unit = {}
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
                chamberDataRef.child("title").setValue(chamberTitle)
                chamberDataRef.child("messages").push().setValue("")
                chamberDataRef.child("timestamp").setValue(ServerValue.TIMESTAMP)

                val messageId = chamberDataRef.child("messages").push().key

                val welcomeMessage = Message(
                    message_id = messageId.toString(),
                    UID = "system",
                    message_content = getApplication<Application>()
                        .getString(R.string.welcome_message),
                    message_type = "custom",
                    sender_name = "system"
                )

                chamberDataRef
                    .child("messages/$messageId")
                    .setValue(welcomeMessage.toMap())

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
                        callback(chamber.groupChatId)
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
        if (userState.value!!.isRestricted) {
            return
        }
        val topic = Topic(
            AuthorName = userState.value!!.displayName,
            AuthorUID = userState.value!!.UID,
            TopicTitle = topicTitle,
            weight = 60,
        )

        val userData = mapOf(
            "isReserved" to false,
            "lfl" to (userState.value!!.role == Role.VENTOR),
            "lfv" to (userState.value!!.role == Role.LISTENER),
            "isAndroid" to true,
            "isSubbed" to false,
            "restricted" to userState.value!!.isRestricted,
            "notificationKey" to userState.value!!.notificationKey,
            "penalty" to 0,
            "timestamp" to ServerValue.TIMESTAMP,
            "blockedUsers" to _blockedUsers.value!!
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
                topicDataRef
                    .child("users")
                    .child(topic.AuthorUID)
                    .setValue(userData)

                _pendingTopics.value!!.add(topic.TopicID)
                pendingTopicTitles[topic.TopicID] = topicTitle
                with(sharedPreferences.edit()) {
                    putString("topics",pendingTopics.value!!.joinToString(","))
                    apply()
                }
                attachListenerForTopic(topic.TopicID)
                logEventToAnalytics("topic_started")
                logEventToAnalytics("Started_Procrastinating")
                callback()
            }
    }

    fun waitOnTopic(topicID: String, topicTitle: String) {
        val userData = mapOf(
            "isAndroid" to true,
            "isReserved" to false,
            "isSubscribed" to false,
            "notificationKey" to (userState.value?.notificationKey ?: ""),
            "lfl" to (userState.value!!.role == Role.VENTOR),
            "lfv" to (userState.value!!.role == Role.LISTENER),
            "penalty" to 0,
            "isWorker" to true,
            "restricted" to userState.value!!.isRestricted,
            "timestamp" to ServerValue.TIMESTAMP,
            "blockedUsers" to blockedUsers.value
        )
        realtimeDatabase
            .reference
            .child(topicID)
            .child("users")
            .child(userState.value!!.UID)
            .setValue(userData)
        val updatedIDs = _pendingTopics.value!!
        updatedIDs.add(topicID)
        _pendingTopics.postValue(updatedIDs)
        pendingTopicTitles[topicID] = topicTitle
        with(sharedPreferences.edit()) {
            putString("topics", pendingTopics.value!!.joinToString(","))
            apply()
        }
        waitAsWorker(topicID, topicTitle)
//        attachListenerForTopic(topicID = topicID)
        logEventToAnalytics("swiped_right_${userState.value!!.role}")
        logEventToAnalytics("swiped_on_card")
        logEventToAnalytics("Started_Procrastinating")
    }

    fun dismissTopic() {
        logEventToAnalytics("swiped_left")
        logEventToAnalytics("swiped_on_card")
    }

    // TODO: Improve the implementation, move it to data layer
    private fun waitAsWorker(
        topicID: String,
        topicTitle: String,
    ) {
        var title = topicTitle
        if(topicTitle.isBlank()) {
            firestore
                .collection("TopicIds")
                .document(topicID)
                .get()
                .addOnSuccessListener {
                    pendingTopicTitles[topicID] = it.data?.get("TopicTitle") as String? ?: ""
                    title = pendingTopicTitles[topicID] ?: ""
                }
        }
        val lookingFor =
            if(userState.value!!.role == Role.LISTENER) "lfl"
            else                                        "lfv"
        realtimeDatabase
            .reference
            .child("$topicID/users")
            .get()
            .addOnSuccessListener { snapshot ->
                val procrastinators = snapshot.value as Map<String, Any>?
                if(procrastinators == null) {
                    realtimeDatabase
                        .reference
                        .child("$topicID/users/${userState.value!!.UID}/isWorker")
                        .setValue(false)
                    attachListenerForTopic(topicID)
                    return@addOnSuccessListener
                }
                val users = mutableListOf<Map<String, Any>>()
                procrastinators.forEach { (uid, userData) ->
                    val userMap = (userData as Map<String, Any>).toMutableMap()
                    val userCheckedBy =
                        try { (userMap["checkedBy"] as List<String>).toMutableList() }
                        catch (_: Exception) { mutableListOf() }
                    Log.d("CHECKED BY", userCheckedBy.toString())
                    if(uid != userState.value!!.UID &&
                        (userMap["isReserved"] ?: false) == false &&
                        (userMap["restricted"] ?: false) == false &&
                        userMap[lookingFor] == true &&
                        (userMap["isWorker"] ?: false) == false &&
                        userState.value!!.UID !in userCheckedBy &&
                        uid !in checkedUsers
                    ) {
                        userCheckedBy.add(userState.value!!.UID)
                        userMap["UID"] = uid
                        userMap["checkedBy"] = userCheckedBy
                        users.add(userMap)
                        if (eligibleUsers[topicID] == null) {
                            eligibleUsers[topicID] = mutableListOf()
                        }
                        eligibleUsers[topicID]!!.add(userMap)
                    }
                }
                if (eligibleUsers[topicID] != null) {
                    eligibleUsers[topicID]!!
                        .sortBy { if(it["isSubscribed"] == true) 0 else 1 }
                    sendRequestsSequentially(topicID, title)
                } else {
                    realtimeDatabase
                        .reference
                        .child("$topicID/users/${userState.value!!.UID}/isWorker")
                        .setValue(false)
                    attachListenerForTopic(topicID = topicID)
                }
            }
            .addOnFailureListener {
                attachListenerForTopic(topicID = topicID)
            }
    }

    private fun sendRequestsSequentially(
        topicID: String,
        topicTitle: String,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            for (user in eligibleUsers[topicID]!!) {
                val job = launch { sendRequest(topicID, topicTitle, user, this) }
                job.join()
            }
        }
        val currUserRef =
            realtimeDatabase
                .reference
                .child("$topicID/users/${userState.value!!.UID}")
        currUserRef
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    currUserRef
                        .child("isWorker")
                        .setValue(false)
                    attachListenerForTopic(topicID)
                }
            }
    }

    private suspend fun sendRequest(
        topicID: String,
        topicTitle: String,
        user: Map<String, Any>,
        coroutineScope: CoroutineScope
    ) {
        val reservedUserRef =
            realtimeDatabase
                .reference
                .child("$topicID/users/${user["UID"]}")
        val currentUserRef =
            realtimeDatabase
                .reference
                .child("$topicID/users/${userState.value!!.UID}")
        try {
            val userSnapshot = reservedUserRef.get().await()
            val userData = userSnapshot.value as Map<String, Any>
            val penalty = try { userData["penalty"] as Long } catch(_: Exception) { 0L }
            if(userSnapshot.exists() && (userData["isReserved"] ?: false) == false) {
                val isReadyListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isReady = snapshot.value as Boolean?
                        if (isReady == true) {
                            createChamber(
                                chamberTitle = topicTitle,
                                callback = { chamberID ->
                                    reservedUserRef.onDisconnect().cancel()
                                    currentUserRef.onDisconnect().cancel()
                                    currentUserRef.updateChildren(
                                        mapOf(
                                            "groupChatId" to chamberID,
                                            "groupTitle" to topicTitle
                                        )
                                    )
                                    eligibleUsers.remove(topicID)
                                    completedMatches.postValue(Pair(chamberID, topicTitle))
                                    val updatedTopics = _pendingTopics.value!!
                                    updatedTopics.remove(topicID)
                                    _pendingTopics.postValue(updatedTopics)
                                    getUserChambers()
                                    taskScheduler.invalidateTimer(topicID)
                                }
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Not required for now
                    }
                }

                reservedUserRef
                    .onDisconnect()
                    .updateChildren(
                        mapOf(
                            "reservedBy" to null,
                            "isReserved" to false,
                        )
                    )

                currentUserRef
                    .onDisconnect()
                    .updateChildren(
                        mapOf(
                            "reserving" to null,
                            "isWorker" to false
                        )
                    )

                reservedUserRef.updateChildren(
                    mapOf(
                        "checkedBy" to
                                (user["checkedBy"] ?: listOf(userState.value!!.UID)),
                        "isReserved" to true,
                        "isReady" to false,
                        "reservedBy" to userState.value!!.UID
                    )
                )

                reservedUserRef
                    .child("isReserved")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val isReserved = snapshot.value as Boolean? ?: false
                            if (!isReserved) {
                                reservedUserRef.removeEventListener(this)
                                currentUserRef.onDisconnect().cancel()
                                reservedUserRef.onDisconnect().cancel()
                                currentUserRef
                                    .child("reserving")
                                    .setValue(null)
                                taskScheduler.invalidateTimer(topicID)
                                return
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Not needed for now
                        }

                    })

                reservedUserRef
                    .child("isReady")
                    .addValueEventListener(isReadyListener)

                sendNotification(token = user["notificationKey"].toString())

                currentUserRef
                    .updateChildren(
                        mapOf(
                            "reserving" to user["UID"].toString()
                        )
                    )

                checkedUsers.add(user["UID"].toString())
                taskScheduler.scheduleTask(
                    topicID = topicID,
                    forUID = user["UID"].toString(),
                    timeInterval = _timeForPenalty * 1000L,
                    repeats = false
                ) {
                    reservedUserRef.onDisconnect().cancel()
                    currentUserRef.onDisconnect().cancel()
                    reservedUserRef
                        .child("isReady")
                        .removeEventListener(isReadyListener)
                    if (user["isAndroid"] == null || user["isAndroid"] == false) {
                        addMissedMatch(topicID, topicTitle, user)
                    }
                    if (penalty == 1L) {
                        // User has accumulated too much penalty.
                        // Remove them as procrastinator in this topic.
                        reservedUserRef
                            .removeValue()
                        return@scheduleTask
                    } else {
                        reservedUserRef
                            .updateChildren(
                                mapOf(
                                    "isReserved" to false,
                                    "reservedBy" to null,
                                    "penalty" to (penalty + 1)
                                )
                            )
                    }

                    currentUserRef
                        .updateChildren(
                            mapOf(
                                "reserving" to null
                            )
                        )
                }
            }
        } catch (e: Exception) {
            Log.d("An error occurred", "Error message: ${e.localizedMessage}")
        }
    }

    private fun addMissedMatch(topicID: String, topicTitle: String, user: Map<String, Any>) {
        val ref =
            realtimeDatabase
                .reference
                .child("missed_matches_${user["UID"]}")

        ref.updateChildren(mapOf("checkedMissedMatches" to false))

        val data = mapOf(
            "UID" to userState.value!!.UID,
            "avatarName" to (1..380).random().toString(),
            "name" to userState.value!!.displayName,
            "notificationKey" to userState.value!!.notificationKey,
            "selectedRole" to userState.value!!.role.toString(),
            "timestamp" to ServerValue.TIMESTAMP,
            "topicID" to topicID,
            "topicTitle" to topicTitle,
        )

        ref.child(userState.value!!.UID).setValue(data)

    }

    private fun attachTopicRequestListeners() {
        for (topic in _pendingTopics.value!!) {
            if (topic.isBlank()) {
                continue
            }
            attachListenerForTopic(topicID = topic)
        }
    }

    private fun attachListenerForTopic(topicID: String) {
        Log.d("LISTENER", "HERE $topicID")
        val userRef =
            realtimeDatabase
                .reference
                .child(topicID)
                .child("users")
                .child(userState.value!!.UID)
        userRef
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()) {
                        _pendingTopics.value!!.remove(topicID)
                        with(sharedPreferences.edit()) {
                            putString("topics", _pendingTopics.value!!.joinToString(","))
                            apply()
                        }
                        return
                    }
                    val userData = snapshot.value as Map<String, Any>
                    val isReserved = userData["isReserved"] as Boolean? ?: false
                    if (isReserved) {
                        // Start matching
                        if(matches.value!!.indexOfFirst { it.topicID == topicID } == -1) {
                            val updatedMatches = _matches.value!!
                            updatedMatches.add(
                                Match(
                                    reservedByUID = userData["reservedBy"].toString(),
                                    reservedByName = "User name",
                                    topicID = topicID,
                                    topicTitle = pendingTopicTitles[topicID] ?: ""
                                )
                            )
                            _matches.postValue(updatedMatches)
                        }
                    } else {
                        // Match expired
                        val updatedMatches = _matches.value!!
                        val didMatchExpire = updatedMatches.removeIf {it.topicID == topicID}
                        _matches.postValue(updatedMatches)
                        if(didMatchExpire) {
                            showToast("The match has expired")
                            logEventToAnalytics("match_timeout")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("MATCHING ERROR", error.toException().toString())
                }
            })
    }

    fun acceptMatch(match: Match) {
        val usersRef = realtimeDatabase
            .reference
            .child("${match.topicID}/users")
        usersRef
            .child("${userState.value!!.UID}/isReady")
            .setValue(true)
        usersRef
            .child("${match.reservedByUID}/groupChatId")
            .setValue("")
        usersRef
            .child("${match.reservedByUID}/groupTitle")
            .setValue(match.topicTitle)
        usersRef
            .child(match.reservedByUID)
            .child("groupChatId")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    usersRef
                        .child(userState.value!!.UID)
                        .child("reservedBy")
                        .get()
                        .addOnSuccessListener {
                            val reservedUID = it.value as? String
                            if (reservedUID.isNullOrBlank()) {
                                return@addOnSuccessListener
                            } else {
                                val groupChatId = snapshot.value as String?
                                if (!groupChatId.isNullOrBlank()) {
                                    logEventToAnalytics("New_Match")
                                    logEventToAnalytics("accepted_match")

                                    realtimeDatabase
                                        .reference
                                        .child(groupChatId)
                                        .child("users")
                                        .child("members")
                                        .child(userState.value!!.UID)
                                        .child("name")
                                        .setValue(userState.value!!.displayName)

                                    usersRef.child(match.reservedByUID).removeValue()
                                    usersRef.child(userState.value!!.UID).removeValue()

                                    firestore
                                        .collection("GroupChatIds")
                                        .document(groupChatId)
                                        .update(
                                            "locked", true,
                                            "members",
                                            FieldValue.arrayUnion(userState.value!!.UID)
                                        )

                                    firestore
                                        .collection("GroupChatIds")
                                        .document(groupChatId)
                                        .update("locked", true)
                                        .addOnSuccessListener {
                                            firestore
                                                .collection("users")
                                                .document(userState.value!!.UID)
                                                .update(
                                                    "chambers",
                                                    FieldValue.arrayUnion(groupChatId)
                                                )
                                            val updatedMatches = _matches.value!!
                                            updatedMatches.removeIf { it.topicID == match.topicID }
                                            _matches.postValue(updatedMatches)
                                            openChamber(groupChatId)
                                        }
                                }
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Matching Failed")
                }
            })
    }

    fun denyMatch(match: Match) {
        realtimeDatabase
            .reference
            .child(match.topicID)
            .child("users")
            .child(userState.value!!.UID)
            .updateChildren(
                mapOf(
                    "isReserved" to false,
                    "reservedBy" to null
                )
            )

        val updatedMatches = _matches.value!!
        updatedMatches.removeIf { it.topicID == match.topicID }
        _matches.postValue(updatedMatches)
        logEventToAnalytics("skipped_match")
        showToast("Match skipped")
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

    private fun getUserRestrictions() {
        val userRef = firestore
            .collection("Accounts")
            .document(userState.value!!.UID)
        userRef
            .update(mapOf("timestamp" to FieldValue.serverTimestamp()))
        userRef
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val currentTime = snapshot.getTimestamp("timestamp")
                        ?: return@addSnapshotListener
                    firestore
                        .collection("Restrictions")
                        .document(userState.value!!.UID)
                        .addSnapshotListener { value, _ ->
                            if (value != null && value.exists()) {
                                val restrictedUntil = value.getTimestamp("restrictedUntil")
                                    ?: currentTime
                                val isRestricted = restrictedUntil > currentTime
                                _userState.postValue(
                                    _userState.value!!.copy(
                                        isRestricted = isRestricted
                                    )
                                )
                                updateTopicRestrictions(isRestricted)
                            }
                        }
                }
            }
    }

    private fun updateTopicRestrictions(isRestricted: Boolean) {
        val updatedTopics = _pendingTopics.value!!
        _pendingTopics.value?.let {
            for (topic in it) {
                if(topic.isNotBlank()) {
                    val userRef = realtimeDatabase
                        .reference
                        .child(topic)
                        .child("users")
                        .child(userState.value!!.UID)
                    userRef.get().addOnSuccessListener { userData ->
                        if (userData.exists()) {
                            userRef
                                .child("restricted")
                                .setValue(isRestricted)
                        } else {
                            updatedTopics.remove(topic)
                        }
                    }
                }
            }
        }
        if (updatedTopics.size != _pendingTopics.value!!.size) {
            _pendingTopics.postValue(updatedTopics)
        }
    }

    fun setRole(role: Role) {
        val selectedRole =
            if (sharedPreferences.getBoolean("isListener", false)) { Role.LISTENER }
            else { Role.VENTOR }
        if (role != selectedRole) {
            firestore
                .collection("Accounts")
                .document(_userState.value!!.UID)
                .update("selectedRole", role.toString())

            with(sharedPreferences.edit()) {
                putBoolean("isListener", role == Role.LISTENER)
                apply()
            }
            _userState.value = _userState.value!!.copy(role = role)
            stopProcrastination()
        }
    }

    fun stopProcrastination(callback: () -> Unit = {}) {
        stopWorking()
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
        _pendingTopics.postValue(mutableListOf())
        pendingTopicTitles.clear()
        with(sharedPreferences.edit()) {
            remove("topics")
            apply()
        }
        callback()
    }

    private fun stopWorking() {
//        TODO: Stop working on all topics
//        taskScheduler.invalidateAllTimers()
//        for (topic in eligibleUsers.keys) {
//            realtimeDatabase
//                .reference
//                .child("$topic/users/${userState.value!!.UID}/isWorker")
//                .setValue(false)
//
//        }
    }

    fun openChamber(chamberID: String) {
        _chamberID.value = chamberID
    }

    fun closeChamber() {
        _chamberID.value = ""
    }

    fun blockUser(UID: String) {
        if(UID == userState.value!!.UID) {
            return
        }
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

    fun subscribe(activity: Activity) {
        try {
            val aPackage = currentOffering?.availablePackages?.firstOrNull()
            aPackage?.let { pkg ->
                val purchaseParams = PurchaseParams
                    .Builder(activity, pkg)
                    .build()
                Purchases.sharedInstance.purchaseWith(
                    purchaseParams,
                    onError = { error: PurchasesError, userCancelled: Boolean ->
                        showToast(
                            if(userCancelled) { "Purchase Cancelled" }
                            else { error.message }
                        )
                    },
                    onSuccess = { purchase: StoreTransaction?, customerInfo: CustomerInfo ->
                        if (customerInfo.entitlements["ChamberlyPlus"]?.isActive == true) {
                            _userState.postValue(
                                _userState.value!!.copy(
                                    entitlement = Entitlement.CHAMBERLY_PLUS
                                )
                            )
                            showToast("Purchase successful! You are now using Chamberly Plus")
                        } else {
                            showToast("Something unexpected happened")
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Log.d("SUBSCRIPTION ERROR", e.message.toString())
        }
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
                    showToast("Account deleted")
                } else {
                    showToast("Failed to delete account")
                }
                _userState.value = UserState()
            }
        } else {
            showToast("Account deleted")
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

    private fun sendNotification(token: String) {
        if(token.isBlank()) { return }
        try {
            val notificationPayload = JSONObject()
            val dataPayload = JSONObject()
            notificationPayload.put(
                "title",
                "New match!"
            )
            notificationPayload.put(
                "body",
                "You have upto $_timeForPenalty seconds to accept."
            )
            dataPayload.put(
                "groupChatId",
                "nil"
            )

            OkHttpHandler(
                getApplication() as Context,
                token,
                notification = notificationPayload,
                data = dataPayload
            ).executeAsync()
        } catch(e: Exception) {
            Log.e("Error sending notification", e.message.toString())
        }
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

    private fun showToast(message: String) {
        Toast.makeText(
            getApplication(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}