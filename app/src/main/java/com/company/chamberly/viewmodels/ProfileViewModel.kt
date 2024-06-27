package com.company.chamberly.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.company.chamberly.constant.Gender
import com.company.chamberly.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileViewModel(application: Application): AndroidViewModel(application = application)  {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val realtimeDatabase:FirebaseDatabase = FirebaseDatabase.getInstance()
    val uid = firebaseAuth.currentUser?.uid
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    private val cachedDir = application.cacheDir
    private var isListener: Boolean = false


    data class UserInfo(
        var Coins: Int? = 0
    )
     fun getNameFromSharePreference(): String {
        return sharedPreferences.getString("displayName", "Name") ?: ""

    }
     fun getGenderFromSharePreference(): Int {
        return sharedPreferences.getInt("gender", Gender.FEMALE_GENDER_INT)

    }
     fun getAgeFromSharePreference(): Int {
        return sharedPreferences.getInt("age", 24)

    }
     fun setGenderToFirestore(genderData: String, chosenGender:Int) {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
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
    fun setAgeToFirestore(age: Int) {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.update("age", age).addOnSuccessListener {
            editor.putInt("age", age)
            editor.apply()
            Log.d("ageUpdate", "Success")
        }.addOnFailureListener {
            Log.d("ageUpdate", "Failure")

        }

    }
    fun updateSectionInAccountFirestore(section:String, key:String, state:Boolean){

        val currUserRef = firestore.collection("Accounts").document(uid!!)
   val privacyChild=HashMap<String,Boolean>()
        privacyChild.put(section+"."+key,state)
        currUserRef.update(privacyChild as Map<String, Any>).addOnSuccessListener {
           val editor = sharedPreferences.edit()
            editor.putBoolean(key, state)
            editor.apply()
            Log.d(key+"Update", "Success")
        }.addOnFailureListener {
            Log.d(key+"Update", "Failure")

        }

    }
    fun getSettingInfoWithBooleanFromSharePreference(key: String):Boolean {
        return sharedPreferences.getBoolean(key, false)

    }
    fun setFeedbackToFirestore(feedback:String,afterTaskDone:()->Unit){
        val feedbackText = "Android: ${feedback}"
        val feedbackRef = firestore.collection("Feedback").document()
        val uid =firebaseAuth.currentUser?.uid!!
        val displayName = getNameFromSharePreference()
        feedbackRef.set(mapOf(
            "byName" to displayName,
            "byUID" to uid,
            "feedbackData" to feedbackText,
            "timestamp" to FieldValue.serverTimestamp()
        )).addOnSuccessListener {
          Log.d("feedbackSent","Success")
            afterTaskDone()

        }.addOnFailureListener {
            Log.d("feedbackSent","Failed")
            afterTaskDone()


        }
    }
    fun deleteAccount(){
        val user = firebaseAuth.currentUser!!
        val currUserAccountRef = firestore.collection("Accounts").document(uid!!)
val displayCurrentUserDoc = firestore.collection("Display_Names").whereEqualTo("UID",uid)
findChambersAndSendingLeaveMessage(currUserAccountRef,uid)
        deleteAccountFromFirebaseAuth(user)
deleteAccountFromFirestore(currUserAccountRef,displayCurrentUserDoc)
      cachedDir.delete()
    }
  private  fun deleteAccountFromFirebaseAuth(user:FirebaseUser,){
        user.delete().addOnSuccessListener {

            Log.d("AccountDeletedFromFirebaseAuth","Success")

        }.addOnFailureListener {
            Log.d("AccountDeletedFromFirebaseAuth","Failed")
            Log.d("AccountDeletedFromFirebaseAuth",it.message.toString())

        }
    }
  private  fun deleteAccountFromFirestore(currentUserRef:DocumentReference,displayCurrentUserDoc:Query){
        currentUserRef.delete().addOnSuccessListener {
            Log.d("AccountInfoDeletedFromFirestore","Success")
        }.addOnFailureListener {
            Log.d("AccountInfoDeletedFromFirestore","Failed")

        }
        displayCurrentUserDoc.get().addOnSuccessListener {
            for(doc in it){
                doc.reference.delete().addOnSuccessListener {
                    Log.d("DisplayInfoDeletedFromFirestore","Success")
                }.addOnFailureListener {
                    Log.d("DisplayInfoDeletedFromFirestore",it.message.toString())

                }
            }
        }


    }

   private fun findChambersAndSendingLeaveMessage(currentUserRef:DocumentReference,UID:String){
        currentUserRef.get().addOnSuccessListener {
            val data =it.data?.get("chambers")
            if(data==null) return@addOnSuccessListener
val chambers = data as List<String>
            for(chamber in chambers){
             sendLeaveMessage(chamber,UID)
            }
        }.addOnFailureListener {
            Log.d("FetchingChambers","")
        }
    }

   private fun sendLeaveMessage(chamberID: String,UID:String) {
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
             Log.d("LeaveMessageSend","Success")
            }.addOnFailureListener {
                Log.d("LeaveMessageSend","Failed")
                Log.d("LeaveMessageSend", it.message.toString())

            }
    }

}