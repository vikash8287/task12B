package com.company.chamberly.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class TopicRequestRecyclerViewAdapter(
    topicIds: List<String>,
    topicsTitles: List<String>,
    reserverIds: List<String>,
    private val acceptRequest: (String, String, String, String) -> Unit,
    private val denyRequest: (String, String, String) -> Unit
): RecyclerView.Adapter<TopicRequestRecyclerViewAdapter.ViewHolder>() {

    private val topicIdsList = mutableListOf<String>()
    private val topicTitlesList = mutableListOf<String>()
    private val reserverIdsList = mutableListOf<String>()

    private val firestore = Firebase.firestore
    init {
        topicIdsList.addAll(topicIds)
        topicTitlesList.addAll(topicsTitles)
        reserverIdsList.addAll(reserverIds)
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val topicTitle: TextView = view.findViewById(R.id.topic_request_title)
        val message: TextView = view.findViewById(R.id.topic_request_message)
        val acceptButton: ImageButton = view.findViewById(R.id.accept_request)
        val denyButton: ImageButton = view.findViewById(R.id.deny_request)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_topic, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return topicIdsList.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topicId = topicIdsList[position]
        val topicTitle = topicTitlesList[position]
        val reservedBy = reserverIdsList[position]
        holder.message.text = "Reserved by: $reservedBy"
        firestore
            .collection("Accounts")
            .document(reservedBy)
            .get()
            .addOnSuccessListener {
                holder.message.text = "Reserved by: ${it.data?.get("Display_Name")}"
            }
        holder.topicTitle.text = topicTitle
        holder.acceptButton.setOnClickListener {
            acceptRequest(topicId, topicTitle, reservedBy, reservedBy)
        }
        holder.denyButton.setOnClickListener {
            denyRequest(topicId, topicTitle, reservedBy)
        }
    }

    fun addItem(topicID: String, topicTitle: String, reservedBy: String) {
        for(item in topicIdsList) {
            if(item == topicID) {
                return
            }
        }
        topicIdsList.add(topicID)
        topicTitlesList.add(topicTitle)
        reserverIdsList.add(reservedBy)
        notifyItemInserted(topicIdsList.size - 1)
    }

    fun removeItem(topic: String) {
        val ind = topicIdsList.indexOf(topic)
        if(ind != -1) {
            topicIdsList.remove(topic)
            topicTitlesList.removeAt(ind)
            reserverIdsList.removeAt(ind)
            notifyItemRemoved(ind)
        }
    }
}