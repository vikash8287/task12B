package com.chamberly.chamberly.models

import java.io.Serializable

data class ActiveChatInfoModel(
    var groupChatID: String ="",
    var groupChatName: String ="",
    var activeChatMemberLimit: Int =2,
    var memberInfoList: List<List<String>>
): Serializable

