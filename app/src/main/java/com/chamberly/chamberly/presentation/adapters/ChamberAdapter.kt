package com.chamberly.chamberly.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.chamberly.chamberly.models.Chamber
import com.chamberly.chamberly.R

class ChamberAdapter : BaseAdapter() {

    val dataList = mutableListOf<Chamber>()

    override fun getCount(): Int = dataList.size

    override fun getItem(position: Int): Chamber = dataList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_koloda, parent, false)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val rightSwipeOverlay: LinearLayout = view.findViewById(R.id.rightSwipeOverlay)
        val leftSwipeOverlay: LinearLayout = view.findViewById(R.id.leftSwipeOverlay)

        val chamber = getItem(position)
        textTitle.text = chamber.groupTitle

        rightSwipeOverlay.visibility = View.INVISIBLE
        leftSwipeOverlay.visibility = View.INVISIBLE

        return view
    }

    fun setData(chamber: Chamber) {
        dataList.add(chamber)
        notifyDataSetChanged()
    }

    fun updateChambers(newChambers: List<Chamber>) {
        dataList.clear()
        dataList.addAll(newChambers)
        notifyDataSetChanged()
    }
}

