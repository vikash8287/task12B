package com.company.chamberly.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.company.chamberly.R
import com.company.chamberly.models.Topic

class TopicAdapter: BaseAdapter() {

    val dataList = mutableListOf<Topic>()
    override fun getCount(): Int = dataList.size

    override fun getItem(position: Int): Topic = dataList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_koloda, parent, false)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val rightSwipeOverlay: LinearLayout = view.findViewById(R.id.rightSwipeOverlay)
        val leftSwipeOverlay: LinearLayout = view.findViewById(R.id.leftSwipeOverlay)

        val topic = getItem(position)
        textTitle.text = topic.TopicTitle

        rightSwipeOverlay.visibility = View.INVISIBLE
        leftSwipeOverlay.visibility = View.INVISIBLE

        return view
    }

    fun setData(topic: Topic) {
        dataList.add(topic)
        notifyDataSetChanged()
    }

    fun updateTopics(newTopics: List<Topic>) {
        dataList.clear()
        dataList.addAll(newTopics)
        notifyDataSetChanged()
    }
}