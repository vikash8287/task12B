package com.chamberly.chamberly.models

import com.google.firebase.firestore.FieldValue

data class UserRatingModel(
    var from: String = "",
    var to: String = "",
    var stars: Double = 0.0,
    var timestamp: FieldValue = FieldValue.serverTimestamp(),
    var totalStars: Double = 0.0,
    var reviewCount: Int = 0,
    var averageStars: Double = 0.0,
)

fun UserRatingModel.toMap(): Map<String, Any> {
    val map = HashMap<String, Any>()
    map["From"] = this.from
    map["To"] = this.to
    map["Stars"] = this.stars
    map["timestamp"] = this.timestamp
    map["TotalStars"] = this.totalStars
    map["ReviewsCount"] = this.reviewCount
    map["AverageStars"] = this.averageStars
    return map
}
