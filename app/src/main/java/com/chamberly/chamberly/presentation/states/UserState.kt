package com.chamberly.chamberly.presentation.states

import com.chamberly.chamberly.models.Chamber
import com.chamberly.chamberly.utils.Entitlement
import com.chamberly.chamberly.utils.Role

data class UserState(
    val UID: String = "",
    val displayName: String = "",
    val notificationKey: String = "",
    val entitlement: Entitlement = Entitlement.REGULAR,
    val role: Role = Role.VENTOR,
    val userRating: Float = 0.0f,
    val isRestricted: Boolean = false,
    val numRatings: Int = 0,
    var coins: Int = 0,
    var chambers: MutableList<Chamber> = mutableListOf()
)
