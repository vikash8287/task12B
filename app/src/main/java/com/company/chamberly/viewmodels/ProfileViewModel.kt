package com.company.chamberly.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.company.chamberly.constant.Gender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileViewModel(application: Application): AndroidViewModel(application = application)  {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val uid = firebaseAuth.currentUser?.uid
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("cache", Context.MODE_PRIVATE)
    private var isListener: Boolean = false
    init{

    }


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
     fun setGenderToDatabase(genderData: String,chosenGender:Int) {
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
    fun setAgeToDatabase(age: Int) {
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
    fun updateSectionInAccount(section:String, key:String, state:Boolean){

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
}