package com.company.chamberly.presentation.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.company.chamberly.models.Chamber
import com.company.chamberly.models.Match
import com.company.chamberly.models.Message
import com.company.chamberly.models.Topic
import com.company.chamberly.models.toMap
import com.company.chamberly.presentation.states.AppState
import com.company.chamberly.presentation.states.UserState
import com.company.chamberly.utils.Entitlement
import com.company.chamberly.utils.REVENUECAT_API_KEY
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

    private val _blockedUsers: MutableLiveData<MutableList<String>> =
        MutableLiveData(mutableListOf())
    val blockedUsers: LiveData<MutableList<String>> = _blockedUsers

    private val _matches: MutableLiveData<MutableList<Match>> = MutableLiveData(mutableListOf())
    val matches: LiveData<MutableList<Match>> = _matches
    // This map will contain the topic title corresponding to the topic ID
    val pendingTopicTitles = mutableMapOf<String, String>()


    private val auth = Firebase.auth
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val firestore = Firebase.firestore
    private val realtimeDatabase = Firebase.database
    private val messaging = FirebaseMessaging.getInstance()
    private var currentOffering: Offering? = null

    init {
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
//                        logEventToAnalytics("")
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
                if (sharedPreferences.getBoolean("isListener", false)) { Role.LISTENER }
                else { Role.VENTOR }
            _userState.value = UserState(
                UID = uid,
                displayName = displayName,
                role = role
            )
            firestore
                .collection("Accounts")
                .document(uid)
                .get()
                .addOnSuccessListener {
                    val blockedUsers = it["blockedUsers"] as List<String>? ?: emptyList()
                    _blockedUsers.postValue(blockedUsers.toMutableList())
                }

            // TesterIDs is an array in realtime database to enable testing of features
            realtimeDatabase
                .reference
                .child("testerIDs")
                .child(uid)
                .get()
                .addOnCompleteListener {
                    //If the task is successful, the user is a tester. Enable areExperimentalFeaturesEnabled
                    Log.d("TESTING", "${it.result.exists()} ${it.result} $uid")
                    if(it.isSuccessful && it.result.exists()) {
                        _appState.postValue(
                            _appState.value!!.copy(areExperimentalFeaturesEnabled = true)
                        )
                        setPaywallStatus()
                    }
                }
            setNotificationToken()
            getUserRestrictions()
            getUserRating()
            attachTopicRequestListeners()
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
                if(customerInfo.entitlements.get("ChamberlyPlus")?.isActive == true) {
                    _userState.postValue(
                        _userState.value!!.copy(entitlement = Entitlement.CHAMBERLY_PLUS)
                    )
                }
            }
        })
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
                logEventToAnalytics("topic_started")
                logEventToAnalytics("Started_Procrastinating")
                callback()
            }
    }
    fun waitOnTopic(topicID: String, topicTitle: String) {
        val userData = mapOf(
            "isAndroid" to true,
            "isReserved" to false,
            "isSubscribed" to true,
            "notificationKey" to (userState.value?.notificationKey ?: ""),
            "lfl" to (userState.value!!.role == Role.VENTOR),
            "lfv" to (userState.value!!.role == Role.LISTENER),
            "penalty" to 0,
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
        attachListenerForTopic(topicID = topicID)
        logEventToAnalytics("swiped_right_${userState.value!!.role}")
        logEventToAnalytics("swiped_on_card")
        logEventToAnalytics("Started_procrastinating")
    }

    fun dismissTopic() {
        logEventToAnalytics("swiped_left")
        logEventToAnalytics("swiped_on_card")
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
        val userRef =
            realtimeDatabase
                .reference
                .child(topicID)
                .child("users")
                .child(userState.value!!.UID)
        Log.d("MATCH FUNCTION", topicID)
        userRef
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("MATCHING", snapshot.value.toString())
                    if(!snapshot.exists()) {
                        _pendingTopics.value!!.remove(topicID)
                        with(sharedPreferences.edit()) {
                            putString("topics", _pendingTopics.value!!.joinToString(","))
                            apply()
                        }
                        return
                    }
                    val userData = snapshot.value as Map<String, Any>
                    val isReserved = userData["isReserved"] as Boolean
                    if (isReserved) {
                        // Start matching
                        Log.d("MATCH FOUND", "$topicID:$userData")
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
                            Log.d("MATCH FOUND", updatedMatches.toString())
                        }
                    } else {
                        // Match expired
                        Log.d("MATCH NOT FOUND", "REMOVE MATCH $userData")
                        val updatedMatches = _matches.value!!
                        val didMatchExpire = updatedMatches.removeIf {it.topicID == topicID}
                        _matches.postValue(updatedMatches)
                        if(didMatchExpire) {
                            showToast("The match has expired")
                        }
                        Log.d("MATCH NOT FOUND", _matches.value!!.toString())
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
            .child(match.topicID)
            .child("users")
        usersRef
            .child(userState.value!!.UID)
            .child("isReady")
            .setValue(true)
        usersRef
            .child(match.reservedByUID)
            .child("groupChatId")
            .setValue("")
        usersRef
            .child(match.reservedByUID)
            .child("groupTitle")
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
                                showToast("Matching failed")
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
            val updatedMatches = _matches.value!!
            updatedMatches.removeIf { it.topicID == match.topicID }
            _matches.postValue(updatedMatches)
    }

    fun denyMatch(match: Match) {
        val userRef = realtimeDatabase
            .reference
            .child(match.topicID)
            .child("users")
            .child(userState.value!!.UID)

        userRef
            .child("isReserved")
            .setValue(false)

        userRef
            .child("reservedBy")
            .removeValue()
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
        _pendingTopics.value?.let {
            for (topic in it) {
                if(topic.isNotBlank()) {
                    realtimeDatabase
                        .reference
                        .child(topic)
                        .child("users")
                        .child(userState.value!!.UID)
                        .child("restricted")
                        .setValue(isRestricted)
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
        _pendingTopics.postValue(mutableListOf())
        pendingTopicTitles.clear()
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
        // Show a toast with the message
        Toast.makeText(
            getApplication(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}