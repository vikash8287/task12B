package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.Chamber
import com.chamberly.chamberly.models.Message
import com.chamberly.chamberly.presentation.adapters.ChambersRecyclerViewAdapter
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActiveChambersFragment : Fragment() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val database = Firebase.database
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_active_chambers, container, false)
        val emptyStateView = view.findViewById<RelativeLayout>(R.id.emptyStateView)
        val layoutManager = LinearLayoutManager(requireContext())
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvChambers)
        val adapter = ChambersRecyclerViewAdapter { chamber ->
            // Handle click, navigate to ChatActivity
            userViewModel.openChamber(chamber.groupChatId)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(requireContext(), layoutManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)

        fetchChambers { chambers ->
            if (chambers.isNotEmpty()) {
                adapter.updateChambers(chambers)
                recyclerView.visibility = View.VISIBLE
                emptyStateView.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                emptyStateView.visibility = View.VISIBLE
            }
        }
        return view
    }

    private fun getLastMessage(lastMessage: Message?): String {
        return if(lastMessage == null) {
            "No messages"
        } else if(lastMessage.message_type == "text" || lastMessage.message_type == "system") {
            lastMessage.message_content
        } else {
            lastMessage.message_type
        }
    }

    private fun fetchLastMessagesForChambers(chambers: List<Chamber>, callback: (List<Chamber>) -> Unit) {
        val lastMessageTasks = chambers.map { chamber ->
            fetchLastMessageForChamber(chamber)
        }

        Tasks.whenAllSuccess<DataSnapshot>(lastMessageTasks)
            .addOnSuccessListener { lastMessages ->
                lastMessages.forEachIndexed { index, dataSnapshot ->
                    val lastMessage =
                        try { dataSnapshot.children.firstOrNull()?.getValue(Message::class.java) }
                        catch(_: Exception) { Message(message_content = "No messages") }
                    chambers[index].lastMessage = getLastMessage(lastMessage)
                }
                callback(chambers)
            }
    }

    private fun fetchLastMessageForChamber(chamber: Chamber): Task<DataSnapshot> {
        return database.reference.child(chamber.groupChatId)
            .child("messages").orderByKey().limitToLast(1).get()
    }

    private fun fetchChambers(callback: (List<Chamber>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("GroupChatIds")
                .whereArrayContains("members", userId)
                .limit(20)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val chambers = querySnapshot.documents.mapNotNull { it.toObject(Chamber::class.java) }
                    fetchLastMessagesForChambers(chambers, callback)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error fetching chambers: ${exception.message}", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
        } else {
            callback(emptyList()) // No user logged in
        }
    }
}