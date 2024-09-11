package com.chamberly.chamberly.models

import com.google.firebase.firestore.FieldValue

data class LeaderBoard(
    val name: String = "",
    val uid: String = "",
    val avatarName:String="",
    var auxiCoins: Int = 0,
    var lastUpdated: Any = FieldValue.serverTimestamp(),
    var earnedToday: Int = 0,
    var earnedThisWeek: Int = 0,
    var earnedThisMonth: Int = 0,
    var todayRank:Int=0,
    var weekRank:Int=0,
    var monthRank:Int=0,
    var todayChangeRank:Int=0,
    var weekChangeRank:Int=0,
    var monthChangeRank:Int=0
)


