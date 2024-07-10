package com.chamberly.chamberly.models

data class ChamberPreview(
    val chamberID: String,
    val chamberTitle: String,
    val lastMessage: Message?,
    val messageRead: Boolean,
    val timestamp: Any?,
)