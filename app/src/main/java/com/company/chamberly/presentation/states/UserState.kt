package com.company.chamberly.presentation.states

import com.company.chamberly.utils.Entitlement
import com.company.chamberly.utils.Role

data class UserState(
    val UID: String = "",
    val displayName: String = "",
    val notificationKey: String = "",
    val entitlement: Entitlement = Entitlement.REGULAR,
    val role: Role = Role.VENTOR,
    val userRating: Float = 0.0f,
    val isRestricted: Boolean = false,
    val numRatings: Int = 0,
    var coins: Int = 0
)
