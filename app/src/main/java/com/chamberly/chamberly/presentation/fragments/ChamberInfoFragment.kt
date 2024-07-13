package com.chamberly.chamberly.presentation.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.ActiveChatInfoModel

class ChamberInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chamber_info, container,false)
        // Retrieve the map from the intent
        val chamberMetadata = arguments?.getSerializable("chamberInfo") as ActiveChatInfoModel
        val textView: TextView = view.findViewById(R.id.chamberName)
        textView.text = chamberMetadata.groupChatName

        val backButton1: ImageButton = view.findViewById(R.id.backButton)
        backButton1.setOnClickListener {
            findNavController().popBackStack()
        }

        val backButton2: ImageButton = view.findViewById(R.id.back_to_chamber)
        backButton2.setOnClickListener {
            findNavController().popBackStack()
        }

        val listView: ListView = view.findViewById(R.id.chamberMembers)

        // Sample data for the ListView
        val data = chamberMetadata.memberInfoList

        // Setting the custom adapter to the ListView
        val adapter = CustomAdapter(requireContext(), R.layout.item_chamber_member, data)
        listView.adapter = adapter
   return view
    }

    class CustomAdapter(context: Context, resource: Int, objects: List<List<String>>) :
        ArrayAdapter<List<String>>(context, resource, objects) {

        private val mContext = context
        private val mResource = resource
        private val mObjects = objects

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = LayoutInflater.from(mContext)
            val view = convertView ?: inflater.inflate(mResource, parent, false)

            val memberInfo = mObjects[position]
            val textView: TextView = view.findViewById(R.id.textView)
            textView.text = memberInfo[1]

            val textRating: TextView = view.findViewById(R.id.ratingCount)
            val formattedString = buildString {
                append("(")
                append(memberInfo[3])
                append(")")
            }
            textRating.text =  formattedString

            val ratings = memberInfo[2].toDouble()
            val imageView1 : ImageView = view.findViewById(R.id.star1)
            val imageView2 : ImageView = view.findViewById(R.id.star2)
            val imageView3 : ImageView = view.findViewById(R.id.star3)
            val imageView4 : ImageView = view.findViewById(R.id.star4)
            val imageView5 : ImageView = view.findViewById(R.id.star5)

            if(ratings >= 0.5)
                imageView1.setImageResource(R.drawable.ic_star)
            else
                imageView1.setImageResource(R.drawable.ic_empty_star)

            if(ratings >= 1.5)
                imageView2.setImageResource(R.drawable.ic_star)
            else
                imageView2.setImageResource(R.drawable.ic_empty_star)

            if(ratings >= 2.5)
                imageView3.setImageResource(R.drawable.ic_star)
            else
                imageView3.setImageResource(R.drawable.ic_empty_star)

            if(ratings >= 3.5)
                imageView4.setImageResource(R.drawable.ic_star)
            else
                imageView4.setImageResource(R.drawable.ic_empty_star)

            if(ratings >= 4.5)
                imageView5.setImageResource(R.drawable.ic_star)
            else
                imageView5.setImageResource(R.drawable.ic_empty_star)

            return view
        }
    }
}