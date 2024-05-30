package com.company.chamberly.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import org.w3c.dom.Text

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

    fun addItem(title: String) {
        topics.add(title)
        notifyItemInserted(topics.size - 1)
    }
}