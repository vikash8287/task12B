package com.company.chamberly

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.company.chamberly.activities.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.internal.notify


private const val channelName = "My Notification Channel"
private const val channelId = "channel_id"
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("token", token)

        // Get the current user's UID or identifier
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            // Define a reference to the Cloud Firestore
            val firestore = Firebase.firestore
            val userRef = firestore.collection("Accounts").document(userId)
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("notificationKey", token)
            editor.apply()
            // Update the FCM token for the current user
            userRef.update("FCMTOKEN", token)
                .addOnSuccessListener {
                    Log.i("FCM Token Updated", "Token updated successfully")
                }
                .addOnFailureListener {
                    Log.e("FCM Token Update Failed", it.message ?: "An error occurred while updating FCM token")
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Handle incoming message here
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]
        val groupChatId = remoteMessage.data["groupChatId"]
        val groupTitle = remoteMessage.data["groupTitle"]
        val authorUid = remoteMessage.data["AuthorUID"]
        val authorName = remoteMessage.data["Authorname"]
        // You can customize the notification's behavior here
        //  if(ChatActivity().isFinishing || ChatActivity().isDestroyed){
        sendNotification(title, body, groupChatId, groupTitle, authorUid, authorName)
    }
    private fun isAppInForeground():Boolean{
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return (appProcessInfo.importance== ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
    }
    private fun sendNotification(title: String?, messageBody: String?,groupChatId:String?,groupTitle:String?,authorUid:String?,authorName:String?) {
        val requestCode = groupChatId.hashCode()
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("GroupChatId", groupChatId)
        intent.putExtra("GroupTitle", groupTitle)
        intent.putExtra("Authorname",authorName)
        intent.putExtra("AuthorUID",authorUid)
        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification_badge)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NotificationChannel setup (for Android O and above)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(requestCode, notificationBuilder.build())
    }
}