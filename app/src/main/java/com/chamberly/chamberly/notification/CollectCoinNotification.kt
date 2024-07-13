package com.chamberly.chamberly.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CollectCoinNotification: Service() {
    private val firestore = Firebase.firestore
    private val firebaseAuth = Firebase.auth
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uid = firebaseAuth.currentUser?.uid
        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.addSnapshotListener { snapshot, e ->
            if(e!=null){
                Log.d("CollectCoinError", e.message.toString())
                return@addSnapshotListener
            }
            val timeStamp = snapshot?.data?.get("DailyCoinsTimestamp") as FieldValue
            val currentTime = FieldValue.serverTimestamp()
            if(timeStamp==currentTime) { }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return  null
    }
}