package com.charmberly.chamberly.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chamberly.chamberly.constant.Gender
import com.chamberly.chamberly.fragments.ProfileFragment
import com.chamberly.chamberly.models.Message
import com.chamberly.chamberly.models.ProfileInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileViewModel(application: Application) : AndroidViewModel(application = application) {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val realtimeDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    val uid = firebaseAuth.currentUser?.uid
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    private val cachedDir = application.cacheDir
    val currUserRef = firestore.collection("Accounts").document(uid!!)
    val _profileInfo: MutableLiveData<ProfileInfo> by lazy {
        MutableLiveData<ProfileInfo>()
    }

    val _tempAge = MutableLiveData<Int>()
    val _tempGender = MutableLiveData<Int>()

    data class UserInfo(
        var Coins: Int? = 0
    )

    fun setUpProfileInfo() {
        getUserInfoFromDatabase()
        getUserRatingFromFirestore()
        _profileInfo.value = ProfileInfo(
            name = getNameFromSharePreference(),
            coins = getCoinsFromSharePreference(),
            isListener = getIsListenerFromSharedPreference(),
            rating = getRatingFromSharePreference(),
            gender = getGenderFromSharePreference(),
            age = getAgeFromSharePreference(),
            bio = getBioTextFromSharePreference(),
            noOfPeople = getReviewCountFromSharedPreference()
        )

        _tempAge.value = _profileInfo.value?.age
        _tempGender.value = _profileInfo.value?.gender
    }

    private fun getBioTextFromSharePreference(): String {
        return sharedPreferences.getString("bio", "default")!!
    }

    private fun getRatingFromSharePreference(): Float {
        return sharedPreferences.getFloat("rating", 0f)
    }

    fun getNameFromSharePreference(): String {
        return sharedPreferences.getString("displayName", "Name") ?: ""

    }

    fun getGenderFromSharePreference(): Int {
        return sharedPreferences.getInt("gender", Gender.FEMALE_GENDER_INT)

    }

    fun getAgeFromSharePreference(): Int {
        return sharedPreferences.getInt("age", 24)

    }
    private fun getReviewCountFromSharedPreference(): Int {
        return sharedPreferences.getInt("reviewCount", 0)
    }


    private fun getCoinsFromSharePreference(): Int {
        return sharedPreferences.getInt("Coins", 0)

    }

    private fun getIsListenerFromSharedPreference(): Boolean {

        val isListener = sharedPreferences.getBoolean("isListener", false)
        return isListener


    }

    fun setGenderToFirestore(genderData: String, chosenGender: Int) {
        val editor = sharedPreferences.edit()

        currUserRef.update("gender", genderData).addOnSuccessListener {
            Log.d("genderUpdate", "Success")
            editor.putInt("gender", chosenGender)
            editor.apply()

        }.addOnFailureListener {
            Log.d("genderUpdate", "Failure")

        }

        currUserRef.update("firstGender", genderData).addOnSuccessListener {
            editor.putInt("firstGender", chosenGender)
            editor.apply()
            Log.d("firstGenderUpdate", "Success")
        }.addOnFailureListener {
            Log.d("firstGenderUpdate", "Failure")

        }
    }

    private fun setRoleToDatabase(role: String) {
        val editor = sharedPreferences.edit()
        val isListener = _profileInfo.value?.isListener!!
        Log.d("value", isListener.toString())
        currUserRef.update("selectedRole", role).addOnSuccessListener {
            editor.putBoolean("isListener", isListener)
            editor.apply()
            Log.d("selectRoleUpdate", "Success")
        }.addOnFailureListener {
            Log.d("selectRoleUpdate", "Failed")
            Log.d("selectRoleUpdate", it.message.toString())


        }
    }

    fun setAgeToFirestore(age: Int) {
        val editor = sharedPreferences.edit()
        currUserRef.update("age", age).addOnSuccessListener {
            editor.putInt("age", age)
            editor.apply()
            Log.d("ageUpdate", "Success")
        }.addOnFailureListener {
            Log.d("ageUpdate", "Failed")
            Log.d("ageUpdate", it.message.toString())

        }

    }

    fun updateSectionInAccountFirestore(section: String, key: String, state: Boolean) {

        val privacyChild = HashMap<String, Boolean>()
        privacyChild.put(section + "." + key, state)
        currUserRef.update(privacyChild as Map<String, Any>).addOnSuccessListener {
            val editor = sharedPreferences.edit()
            editor.putBoolean(key, state)
            editor.apply()
            Log.d(key + "Update", "Success")
        }.addOnFailureListener {
            Log.d(key + "Update", "Failure")
            Log.d(key + "Update", it.message.toString())

        }

    }

    private fun getUserRatingFromFirestore() {
        val editor = sharedPreferences.edit()

        firestore.collection("StarReviews").whereEqualTo("To",uid).orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().addOnSuccessListener {
                document->
            if(document.size()==0){
               // numberOfPeopleView.setText("(0)")
                updateNoOfPeople(0)
                updateRating(0f)
                return@addOnSuccessListener
            }
            val doc= document.documents[0].data
            val averageRating =  doc?.get("AverageStars")
            val numberOfPeople= doc?.get("ReviewsCount")

           // numberOfPeopleView.setText("(${numberOfPeople.toString()})")
           // ratingBar.rating = (averageRating as Double).toFloat()

            updateNoOfPeople((numberOfPeople as Long).toInt())
            updateRating((averageRating as Double).toFloat())

            editor.putInt("reviewCount",numberOfPeople.toString().toInt())
            editor.putFloat("rating", (averageRating as Double).toFloat())
            editor.apply()

            Log.d("Data",doc.toString())
        }.addOnFailureListener {
            Log.d("FirestoreError",it.message.toString())
        }
    }

    fun getSettingInfoWithBooleanFromSharePreference(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)

    }

    fun setFeedbackToFirestore(feedback: String, afterTaskDone: () -> Unit) {
        val feedbackText = "Android: ${feedback}"
        val feedbackRef = firestore.collection("Feedback").document()
        val uid = firebaseAuth.currentUser?.uid!!
        val displayName = getNameFromSharePreference()
        feedbackRef.set(
            mapOf(
                "byName" to displayName,
                "byUID" to uid,
                "feedbackData" to feedbackText,
                "timestamp" to FieldValue.serverTimestamp()
            )
        ).addOnSuccessListener {
            Log.d("feedbackSent", "Success")
            afterTaskDone()

        }.addOnFailureListener {
            Log.d("feedbackSent", "Failed")
            afterTaskDone()


        }
    }

    fun deleteAccount() {
        val user = firebaseAuth.currentUser!!
        val currUserAccountRef = firestore.collection("Accounts").document(uid!!)
        val displayCurrentUserDoc = firestore.collection("Display_Names").whereEqualTo("UID", uid)
        findChambersAndSendingLeaveMessage(currUserAccountRef, uid)
        deleteAccountFromFirebaseAuth(user)
        deleteAccountFromFirestore(currUserAccountRef, displayCurrentUserDoc)
        cachedDir.delete()
    }

    private fun deleteAccountFromFirebaseAuth(user: FirebaseUser) {
        user.delete().addOnSuccessListener {

            Log.d("AccountDeletedFromFirebaseAuth", "Success")

        }.addOnFailureListener {
            Log.d("AccountDeletedFromFirebaseAuth", "Failed")
            Log.d("AccountDeletedFromFirebaseAuth", it.message.toString())

        }
    }

    private fun deleteAccountFromFirestore(
        currentUserRef: DocumentReference,
        displayCurrentUserDoc: Query
    ) {
        currentUserRef.delete().addOnSuccessListener {
            Log.d("AccountInfoDeletedFromFirestore", "Success")
        }.addOnFailureListener {
            Log.d("AccountInfoDeletedFromFirestore", "Failed")

        }
        displayCurrentUserDoc.get().addOnSuccessListener {
            for (doc in it) {
                doc.reference.delete().addOnSuccessListener {
                    Log.d("DisplayInfoDeletedFromFirestore", "Success")
                }.addOnFailureListener {
                    Log.d("DisplayInfoDeletedFromFirestore", it.message.toString())

                }
            }
        }


    }


    private fun findChambersAndSendingLeaveMessage(currentUserRef: DocumentReference, UID: String) {
        currentUserRef.get().addOnSuccessListener {
            val data = it.data?.get("chambers")
            if (data == null) return@addOnSuccessListener
            val chambers = data as List<String>
            for (chamber in chambers) {
                sendLeaveMessage(chamber, UID)
            }
        }.addOnFailureListener {
            Log.d("FetchingChambers", "")
        }
    }

    private fun sendLeaveMessage(chamberID: String, UID: String) {
        val chamberDataRef = realtimeDatabase
            .reference
            .child(chamberID)

        val systemMessage = Message(
            "Chamberly",
            "Leave reason: Deleted their account",
            "system",
            "Chamberly"
        )



        chamberDataRef
            .child("messages")
            .push()
            .setValue(systemMessage)
            .addOnSuccessListener {
                Log.d("LeaveMessageSend", "Success")
            }.addOnFailureListener {
                Log.d("LeaveMessageSend", "Failed")
                Log.d("LeaveMessageSend", it.message.toString())

            }
    }
    private fun setBioToDatabase(bio: String) {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.update("bio", bio).addOnSuccessListener {
            editor.putString("bio", bio)
            editor.apply()

            Log.d("bioUpdate", "Success")
        }.addOnFailureListener {
            Log.d("bioUpdate", "Failure")

        }

    }

    private fun updateCoins(coins: Int) {
        _profileInfo.value = _profileInfo.value!!.copy(coins = coins)

    }

    private fun updateRating(rating: Float) {
        _profileInfo.value = _profileInfo.value!!.copy(rating = rating)

    }

    fun updateBio(bio: String) {
        _profileInfo.value = _profileInfo.value!!.copy(bio = bio)
        Log.d("bio",bio)
        setBioToDatabase(bio)

    }

    fun updateGender(gender: Int) {
        _profileInfo.value = _profileInfo.value!!.copy(gender = gender)
        setGenderToFirestore(getGenderText(gender), gender)
    }

    private fun getGenderText(gender: Int): String {
        return when (gender) {
            Gender.MALE_GENDER_INT -> "male"
            Gender.FEMALE_GENDER_INT -> "female"
            Gender.OTHER_GENDER_INT -> "other"
            else -> "male"
        }
    }

    fun updateIsListener(value: Boolean) {
        _profileInfo.value = _profileInfo.value!!.copy(isListener = value)
        if (value) setRoleToDatabase("listener") else setRoleToDatabase("ventor")
    }

    fun updateTempGender(value: Int) {
        _tempGender.value = value
    }

    fun updateAge(value: Int) {
        _profileInfo.value = _profileInfo.value?.copy(age = value)
        setAgeToFirestore(value)

    }

    fun updateTempAge(age: Int) {
        _tempAge.value = age
    }
    fun updateNoOfPeople(value:Int){
        _profileInfo.value = _profileInfo.value?.copy(noOfPeople = value)
    }
    private fun getUserInfoFromDatabase() {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val coins = document.data?.get("Coins") as Long

                editor.putInt("Coins",coins.toInt())
                updateCoins(coins.toInt())
                editor.apply()
            }
        }.addOnFailureListener {
            Log.d("getCoins", "Failed")
            Log.d("getCoins", it.toString())

        }
    }

}