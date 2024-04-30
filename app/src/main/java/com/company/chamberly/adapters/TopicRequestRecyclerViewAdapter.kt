package com.company.chamberly.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.company.chamberly.activities.MainActivity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class TopicRequestRecyclerViewAdapter(
    topics: List<String>,
    private val acceptRequest: (String, String, String, String) -> Unit,
    private val denyRequest: (String, String, String) -> Unit
): RecyclerView.Adapter<TopicRequestRecyclerViewAdapter.ViewHolder>() {

    private val dataList = mutableListOf<String>()

    private val firestore = Firebase.firestore
    init {
        dataList.addAll(topics)
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
        return dataList.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = dataList[position].split("::")
        val topicId = topic[0]
        val topicTitle = topic[1]
        var reservedBy = ""
        firestore.collection("Accounts").document(topic[2]).get().addOnSuccessListener { userSnapshot ->
            reservedBy = userSnapshot.data?.get("Display_Name").toString()
            holder.message.text = "Reserved by: $reservedBy"
        }
        holder.topicTitle.text = topicTitle
        holder.acceptButton.setOnClickListener {
            acceptRequest(topicId, topicTitle, topic[2], reservedBy)
        }
        holder.denyButton.setOnClickListener {
            denyRequest(topicId, topicTitle, topic[2])
        }
    }

    fun addItem(topic: String) {
        for(item in dataList) {
            if(item == topic) {
                return
            }
        }
        dataList.add(topic)
        notifyItemInserted(dataList.size - 1)
    }

    fun removeItem(topic: String) {
        val ind = dataList.indexOf(topic)
        dataList.remove(topic)
        notifyItemRemoved(ind)
    }
}