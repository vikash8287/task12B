package com.chamberly.chamberly.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserAccount(
    val displayName: String = "",
    val email: String = "",
    val uid: String = "",
    val age: Int = 0,
    val biography: String = "",
    val blockedUser: List<String> = emptyList(),
    val gender: String = "male",
    val isModerator: Boolean = false,
    val platform: String = "android",
    val selectedRole: String = "listener",
    @ServerTimestamp val timestamp: Date? = null
)