package com.chamberly.chamberly.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R

class PendingTopicsListAdapter: RecyclerView.Adapter<PendingTopicsListAdapter.ViewHolder>() {

    private var topics = mutableListOf<String>()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val topicTitle = view.findViewById<TextView>(R.id.topic_title)
        val topicIndex = view.findViewById<TextView>(R.id.topic_index)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_pending_topic,
                parent,
                false
            )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.topicIndex.text = "${position + 1}. "
        holder.topicTitle.text = topics[position]
    }

    override fun getItemId(position: Int): Long =
        topics[position].hashCode().toLong()

    override fun getItemCount(): Int = topics.size

    fun addItems(titles: List<String>) {
        topics.clear()
        topics.addAll(titles)
        notifyDataSetChanged()
    }
}