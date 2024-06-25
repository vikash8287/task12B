package com.chamberly.chamberly.presentation.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.chamberly.chamberly.models.Chamber
import com.chamberly.chamberly.R

class KolodaAdapter(
    private val context: Context,
    private val dataList: MutableList<Chamber>
) : BaseAdapter() {

    override fun getCount(): Int = dataList.size

    override fun getItem(position: Int): Any = dataList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_koloda, parent, false)
        val chamber = dataList[position] as Chamber
        // Set up your Koloda card views here, using view.findViewById
        return view
    }
}
