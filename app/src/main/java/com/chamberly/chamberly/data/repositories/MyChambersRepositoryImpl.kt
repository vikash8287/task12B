package com.chamberly.chamberly.data.repositories

import com.chamberly.chamberly.domain.repositories.MyChambersRepository
import com.chamberly.chamberly.models.Chamber
import com.chamberly.chamberly.models.ChamberPreview
import com.chamberly.chamberly.models.Message
import com.chamberly.chamberly.utils.Resource
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MyChambersRepositoryImpl: MyChambersRepository {
    private val firestore = Firebase.firestore
    private val realtimeDatabase = Firebase.database
    override fun getMyChambers(uid: String): Flow<Resource<List<ChamberPreview>>> {
        return flow {
            emit(Resource.Loading())
            firestore
                .collection("MyChambers")
                .document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        val data = snapshot.data
                        val myChambers =
                            try {
                                data?.get("MyChambersN") as? Map<String, Any>
                            } catch (_: Exception) {
                                emptyMap()
                            }

                        if (myChambers.isNullOrEmpty()) {
                            // Emit Resource.Success with empty list
                        } else {
                            val chambers =
                                try { myChambers.values.toList() as List<MutableMap<String, Any>> }
                                catch (_: Exception) { emptyList() }

                            for (chamber in chambers) {
                                firestore
                                    .collection("GroupChatIds")
                                    .document(chamber["groupChatId"].toString())
                                    .get()
                                    .addOnSuccessListener { chamberSnapshot ->
                                        val chamberDetails =
                                            chamberSnapshot.toObject(Chamber::class.java)
                                        if (chamberDetails != null) {
                                            realtimeDatabase
                                                .reference
                                                .child(chamber["groupChatId"].toString())
                                                .child("messages")
                                                .orderByKey()
                                                .limitToLast(1)
                                                .get()
                                                .addOnSuccessListener { message ->
                                                    val lastMessage =
                                                        try {
                                                            message.children.firstOrNull()
                                                                ?.getValue(Message::class.java)
                                                        }
                                                        catch (_: Exception) { Message() }
                                                    val chamberPreview = ChamberPreview(
                                                        chamberID = chamber["groupChatId"].toString(),
                                                        chamberTitle = chamberDetails.groupTitle,
                                                        lastMessage = lastMessage,
                                                        messageRead = chamber["messageRead"] as Boolean,
                                                        timestamp = chamber["timestamp"]
                                                    )
                                                }
                                            //Create a list of chamberPreview for all chambers and emit Resource.Success with the list
                                        }
                                    }
                            }
                        }
                    }
                }
        }
    }

    override fun addChamber(uid: String, chamber: ChamberPreview) {
        firestore
            .collection("MyChambers")
            .document(uid)
            .get()
            .addOnSuccessListener {
                val myChambers =
                    ((it["MyChambersN"] as? Map<String, Any>) ?: mapOf()).toMutableMap()
                myChambers[chamber.chamberID] = chamber
                // TODO: Update in firestore
            }
    }
}