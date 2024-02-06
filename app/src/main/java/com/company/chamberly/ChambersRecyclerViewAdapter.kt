package com.company.chamberly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChambersRecyclerViewAdapter(private val onItemClick: (Chamber) -> Unit) : RecyclerView.Adapter<ChambersRecyclerViewAdapter.ViewHolder>() {

    private val dataList = mutableListOf<Chamber>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage) // Placeholder for last message
        // Add other UI components if needed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chamber, parent, false)
        return ViewHolder(view)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chamber = dataList[position]
        holder.textTitle.text = chamber.groupTitle
        holder.tvLastMessage.text = chamber.lastMessage // assuming 'lastMessage' is a String in Chamber
        holder.itemView.setOnClickListener { onItemClick(chamber) }
    }


    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateChambers(chambers: List<Chamber>) {
        dataList.clear()
        dataList.addAll(chambers)
        notifyDataSetChanged()
    }
}

