package com.company.chamberly.models

data class Message @JvmOverloads constructor(
    var UID: String ="",
    var message_content: String = "",
    var message_type: String = "",
    var sender_name: String = "",
    var message_id: String = "",
    var game_content: String = ""
)

fun messageToMap(message: Message): Map<String, Any> {
    val messageMap = HashMap<String, Any>()
    messageMap["UID"] = message.UID
    messageMap["message_content"] = message.message_content
    messageMap["message_type"] = message.message_type
    messageMap["sender_name"] = message.sender_name
    messageMap["message_id"] = message.message_id
    return messageMap
}