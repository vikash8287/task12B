package com.company.chamberly.viewmodels

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import com.company.chamberly.constant.Gender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileViewModel(application: Application): AndroidViewModel(application = application)  {
    private var chosenGender = Gender.MALE_GENDER_INT //MALE_GENDER_INT
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val uid = firebaseAuth.currentUser?.uid
    private lateinit var sharedPreferences: SharedPreferences
    private var isListener: Boolean = false
    init{

    }


    data class UserInfo(
        var Coins: Int? = 0
    )
}