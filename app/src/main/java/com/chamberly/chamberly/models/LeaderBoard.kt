package com.chamberly.chamberly.models

import com.google.firebase.firestore.FieldValue

data class LeaderBoard(
    val name: String = "",
    val uid: String = "",
    val avatarName:String="",
    var lastTodayUpdated: Any = FieldValue.serverTimestamp(),
    var lastWeekUpdated:Any = FieldValue.serverTimestamp(),
    var lastMonthUpdated:Any = FieldValue.serverTimestamp(),
    var earnedToday: Int = 0,
    var earnedThisWeek: Int = 0,
    var earnedThisMonth: Int = 0,
    var todayRank:Int=0,
    var weekRank:Int=0,
    var monthRank:Int=0,
    var prevTodayRank:Int=0,
    var prevWeekRank:Int=0,
    var prevMonthRank:Int=0
)


