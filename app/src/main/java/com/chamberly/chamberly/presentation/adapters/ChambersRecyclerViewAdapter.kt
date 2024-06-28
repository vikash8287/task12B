package com.chamberly.chamberly.presentation.adapters

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.ChamberPreview
import com.google.firebase.Timestamp

class ChambersRecyclerViewAdapter(private val UID: String, private val onItemClick: (ChamberPreview) -> Unit) : RecyclerView.Adapter<ChambersRecyclerViewAdapter.ViewHolder>() {

    private var dataList = mutableListOf<ChamberPreview>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage) // Placeholder for last message
        val notificationBadge: ImageView = view.findViewById(R.id.notificationBadge)
        // Add other UI components if needed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chamber, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= dataList.size) {
            return
        }
        val chamber = dataList[position]
        holder.textTitle.text = chamber.chamberTitle
        holder.tvLastMessage.text =
            if (
                chamber.lastMessage == null ||
                chamber.lastMessage.message_id.isBlank()
            ) {
                "No messages"
            } else if (chamber.lastMessage.UID == UID) {
                "You: ${chamber.lastMessage.message_content}"
            } else {
                chamber.lastMessage.message_content
            }
        if (chamber.messageRead) {
            holder.textTitle.setTypeface(holder.textTitle.typeface, Typeface.NORMAL)
            holder.tvLastMessage.setTypeface(holder.tvLastMessage.typeface, Typeface.NORMAL)
            holder.notificationBadge.visibility = View.GONE
        } else {
            holder.textTitle.setTypeface(holder.textTitle.typeface, Typeface.BOLD)
            holder.tvLastMessage.setTypeface(holder.tvLastMessage.typeface, Typeface.BOLD)
            holder.notificationBadge.visibility = View.VISIBLE
        }
        holder.itemView.setOnClickListener { onItemClick(chamber) }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateChambers(chambers: List<ChamberPreview>) {
        dataList.clear()
        dataList.addAll(chambers)
        dataList = dataList.sortedWith(compareBy(
            { !it.messageRead },
            { it.timestamp as? Timestamp ?: Timestamp.now() }
        )).reversed().toMutableList()
        for(chamber in chambers) {
            Log.d("Timestamp", chamber.chamberTitle + ":" +(chamber.timestamp as Timestamp).toString())
        }
        notifyDataSetChanged()
    }

    fun addChamber(chamber: ChamberPreview?) {
        if(chamber == null) {
            return
        }
        dataList.add(chamber)
        notifyItemInserted(dataList.size - 1)
    }
}

