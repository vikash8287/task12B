package com.chamberly.chamberly.models

import com.chamberly.chamberly.constant.Gender

data class ProfileInfo(
    val name:String="",
    val coins:Int=0,
    val isListener:Boolean=false,
    val rating:Float=0f,
    val age:Int=24,
    val gender:Int= Gender.MALE_GENDER_INT,
    val bio:String="",
    val noOfPeople:Int = 0
)

