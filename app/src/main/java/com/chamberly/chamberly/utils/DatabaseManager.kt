package com.chamberly.chamberly.utils

import com.google.firebase.database.FirebaseDatabase

class DatabaseManager(
    private val UID: String,
    private val displayName: String
) {
    private val realtimeDatabase = FirebaseDatabase.getInstance().reference

    fun cancelDisconnectOperationForWorker(topicID: String) {
        realtimeDatabase.child("$topicID/users/$UID").onDisconnect().cancel()
    }

    fun addGroupChatToNegotiator(
        topicID: String,
        groupChatID: String,
        onComplete: (Boolean) -> Unit
    ) {
        val update = mapOf("groupChatId" to groupChatID)
        realtimeDatabase
            .child("$topicID/users/$UID")
            .updateChildren(update)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }
}