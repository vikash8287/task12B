package com.company.chamberly.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.company.chamberly.models.Match
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class TopicRequestRecyclerViewAdapter(
    matches: List<Match> = emptyList(),
    private val acceptRequest: (Match) -> Unit,
    private val denyRequest: (Match) -> Unit
): RecyclerView.Adapter<TopicRequestRecyclerViewAdapter.ViewHolder>() {

    private val topicsList = mutableListOf<Match>()
    private val firestore = Firebase.firestore
    init {
        topicsList.addAll(matches)
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val topicTitle: TextView = view.findViewById(R.id.topic_request_title)
        val message: TextView = view.findViewById(R.id.topic_request_message)
        val acceptButton: ImageButton = view.findViewById(R.id.accept_request)
        val denyButton: ImageButton = view.findViewById(R.id.deny_request)
        val loader: LinearProgressIndicator = view.findViewById(R.id.loader)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_topic, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return topicsList.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = topicsList[position]
        val topicId = topic.topicID
        val topicTitle = topic.topicTitle
        val reservedBy = topic.reservedByUID

        holder.loader.visibility = if (topic.loading) View.VISIBLE else View.GONE
        holder.acceptButton.isEnabled = !topic.loading
        holder.denyButton.isEnabled = !topic.loading

        firestore
            .collection("Accounts")
            .document(reservedBy)
            .get()
            .addOnSuccessListener {
                val reservedByName = it.data?.get("Display_Name").toString()
                holder.message.text = "Reserved by: $reservedByName"
                topicsList[position] = topic.copy(reservedByName = reservedByName)
            }
        holder.topicTitle.text = topicTitle
        holder.acceptButton.setOnClickListener {
            topicsList[position].loading = true
            notifyItemChanged(position)
            acceptRequest(topicsList[position])

        }
        holder.denyButton.setOnClickListener {
            topicsList[position].loading = true
            notifyItemChanged(position)
            denyRequest(topicsList[position])
        }
    }

    fun addItem(topic: Match) {
        for(item in topicsList) {
            if(item.topicID == topic.topicID) {
                return
            }
        }
        topicsList.add(topic)
        notifyItemInserted(topicsList.size - 1)
    }

    fun removeItem(topicID: String) {
        val ind = topicsList.indexOfFirst { it.topicID == topicID }
        if(ind != -1) {
            topicsList.removeAt(ind)
            notifyItemRemoved(ind)
        }
    }

    fun clear() {
        topicsList.clear()
        notifyDataSetChanged()
    }

    fun updateList(list: List<Match>) {
        val newSet = list.map { it.topicID }.toSet()

        val iterator = topicsList.listIterator()

        while (iterator.hasNext()) {
            val match = iterator.next()
            if (match.topicID !in newSet) {
                val index = iterator.previousIndex()
                iterator.remove()
                notifyItemRemoved(index)
            }
        }

        val topicsSet = topicsList.map { it.topicID }.toSet()
        list.forEachIndexed { index, match ->
            if (match.topicID !in topicsSet) {
                topicsList.add(match)
                notifyItemInserted(topicsList.size - 1)
            }
        }
    }
}