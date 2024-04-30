package com.company.chamberly.models

data class Message @JvmOverloads constructor(
    var UID: String ="",
    var message_content: String = "",
    var message_type: String = "",
    var sender_name: String = "",
    var message_id: String = "",
    var game_content: String = "",
    var reactedWith: String = "",
    var replyingTo: String = ""
)

fun Message.toMap(): Map<String, Any> {
    val messageMap: HashMap<String, Any> = hashMapOf(
        "UID" to this.UID,
        "message_content" to this.message_content,
        "message_type" to this.message_type,
        "sender_name" to this.sender_name,
        "message_id" to this.message_id,
    )
    if(reactedWith.isNotBlank()) {
        messageMap["reactedWith"] = this.reactedWith
    }
    if(replyingTo.isNotBlank()) {
        messageMap["replyingTo"] = this.replyingTo
    }
    return messageMap
}