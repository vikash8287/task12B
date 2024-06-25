package com.chamberly.chamberly.models

data class Match(
    val reservedByUID: String,
    val reservedByName: String = "",
    val topicTitle: String,
    val topicID: String,
    var loading: Boolean = false
)
