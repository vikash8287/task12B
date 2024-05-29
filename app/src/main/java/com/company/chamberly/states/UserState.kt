package com.company.chamberly.states

import com.company.chamberly.utils.Role

data class UserState(
    val UID: String = "",
    val displayName: String = "",
    val notificationKey: String = "",
    val role: Role = Role.VENTOR,
    val userRating: Float = 0.0f,
    val numRatings: Int = 0,
    var coins: Int = 0
)
