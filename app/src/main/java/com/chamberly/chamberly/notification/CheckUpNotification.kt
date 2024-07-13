package com.chamberly.chamberly.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.activities.MainActivity

class CheckUpNotification: BroadcastReceiver() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences

    private val notificationID = 2
    private val maxNoOfDay = 45
    private val channelID = "com.company.chamberly.broadcast_receiver.checkup"

    override fun onReceive(context: Context, intent: Intent) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        sharedPreferences = context.getSharedPreferences("cache", Context.MODE_PRIVATE)
        pushingNotification(context)
    }

    private fun pushingNotification(context: Context) {
        val noOfDays = sharedPreferences.getInt("dayCount",0)
        if(noOfDays > maxNoOfDay) {
            with(sharedPreferences.edit()) {
                putInt("dayCount", 0)
                commit()
            }
        } else {
            with(sharedPreferences.edit()) {
                putInt("dayCount", noOfDays + 1)
                commit()
            }
        }
        settingUpNotification(context,noOfDays)
    }

    private fun settingUpNotification(context: Context, noOfDays: Int) {
        val initialNotifications = listOf(
            NotificationItem(dayOffset= 1, message= "How are you so far? 🥹"),
            NotificationItem(dayOffset= 2, message= "Ready to vent today? ✨"),
            NotificationItem(dayOffset= 3, message= "Connect and share your journey 🤝"),
        )

        val week2To4Notifications = listOf(
            NotificationItem(dayOffset= 6, message= "How are you feeling today?"),
            NotificationItem(dayOffset= 10, message= "You matter to us, feel like venting?"),
            NotificationItem(dayOffset= 17, message= "Bored or sad lately? 🤔 Whatever it is, let's chat!"),
        )

        val week5AndBeyondNotifications = listOf(
            NotificationItem(dayOffset= 24, message= "Want to find a new venting partner? 💬"),
            NotificationItem(dayOffset= 38, message= "How have you been this week? 🤗"),
            NotificationItem(dayOffset= 45, message= "Feel like venting about something? 🥹"),
        )
        val allNotifications =
            initialNotifications + week2To4Notifications + week5AndBeyondNotifications

        for(notification in allNotifications) {
            if (notification.dayOffset == noOfDays) {
                scheduleNotification(context = context,notificationItem = notification)
            }
        }
    }
    private fun scheduleNotification(context: Context,notificationItem: NotificationItem){
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                3,
                notificationIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            )

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.dp)
            .setContentTitle("Chamberly")
            .setContentText(notificationItem.message)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelID,
                "Chamberly Checkup" ,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(notificationChannel)
        }
        manager.notify(notificationID, notification.build())
    }

    private data class NotificationItem(val dayOffset: Int, val message: String)
}