package com.chamberly.chamberly.presentation.states

data class ChamberState(
    val chamberID: String,
    val chamberTitle: String,
    val members: List<String> = emptyList(),
//    val messages: List
)

