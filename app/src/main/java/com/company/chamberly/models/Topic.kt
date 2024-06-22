package com.company.chamberly.models

import com.google.firebase.firestore.FieldValue

data class Topic(
    var AuthorName: String = "",
    var AuthorUID: String = "",
    var TopicID: String = "",
    var TopicTitle: String = "",
    var forGender: String = "male",
    var lflWeight: Int = 15,
    var lfvWeight: Int = 15,
    var lflCount: Int = 15,
    var lfvCount: Int = 15,
    var timestamp:  Any = FieldValue.serverTimestamp(),
    var weight: Int = 60,
)

fun Topic.toMap(): Map<String, Any> {
    val topicMap = HashMap<String, Any>()
    topicMap["AuthorName"] = this.AuthorName
    topicMap["AuthorUID"] = this.AuthorUID
    topicMap["TopicTitle"] = this.TopicTitle
    topicMap["TopicID"] = this.TopicID
    topicMap["forGender"] = this.forGender
    topicMap["lflWeight"] = this.lflWeight
    topicMap["lfvWeight"] = this.lfvWeight
    topicMap["timestamp"] = this.timestamp
    topicMap["weight"] = this.weight
    return topicMap
}
