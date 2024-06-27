package com.company.chamberly.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

import com.company.chamberly.R
import com.company.chamberly.activities.MainActivity

const val notificationID = 1
const val title = "Reminder"
const val message = "Check up on your chamber?"
class ReminderNotification: BroadcastReceiver(){
val channelID = "com.company.chamberly.broadcast_receiver.reminder"
    override fun onReceive(context: Context, intent: Intent) {

        val notificationIntent = Intent(context, MainActivity::class.java) // Replace with your main activity class
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)



        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.dp)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =    NotificationChannel(
                channelID,
                "Chamberly Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
manager.createNotificationChannel(notificationChannel)
        }
        // Show the notification using the manager
        manager.notify(notificationID, notification.build())


    }
}