package com.company.chamberly.models

import com.google.firebase.firestore.FieldValue

data class Topic(
    var AuthorName: String = "",
    var AuthorUID: String = "",
    var TopicID: String = "",
    var TopicTitle: String = "",
    var forGender: String = "male",
    var lflCount: Int = 0,
    var lfvCount: Int = 0,
    var timestamp:  Any = FieldValue.serverTimestamp(),
    var weight: Int = 60,
)

fun topicToMap(topic: Topic): Map<String, Any> {
    val topicMap = HashMap<String, Any>()
    topicMap["AuthorName"] = topic.AuthorName
    topicMap["AuthorUID"] = topic.AuthorUID
    topicMap["TopicTitle"] = topic.TopicTitle
    topicMap["TopicID"] = topic.TopicID
    topicMap["forGender"] = topic.forGender
    topicMap["lflCount"] = topic.lflCount
    topicMap["lfvCount"] = topic.lfvCount
    topicMap["timestamp"] = topic.timestamp
    topicMap["weight"] = topic.weight
    return topicMap
}
