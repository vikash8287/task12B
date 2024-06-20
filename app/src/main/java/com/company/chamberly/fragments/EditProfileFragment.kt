package com.company.chamberly.fragments

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.company.chamberly.R
import com.company.chamberly.customview.SettingSectionWithListViewToggleButton
import com.company.chamberly.customview.SettingSectionWithListViewToggleButton.NormalListWithToggleItem

class EditProfileFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val normalListWithoutToggleView = view.findViewById<ListView>(R.id.normal_list_without_toggle)
        val normalListWithoutToggle = arrayListOf<NormalListWithoutToggleItem>(
            NormalListWithoutToggleItem("Name",NormalListWithoutToggleAction.NAME,false),
            NormalListWithoutToggleItem("Male",NormalListWithoutToggleAction.GENDER),
            NormalListWithoutToggleItem("24",NormalListWithoutToggleAction.AGE)
        )
        normalListWithoutToggleView.adapter = NormalListWithoutToggleAdapter(list=normalListWithoutToggle, activity = requireActivity())

   val normalListWithToggleView = view.findViewById<SettingSectionWithListViewToggleButton>(R.id.list_with_toggle)
        val toggleList  = arrayListOf<NormalListWithToggleItem>(
            NormalListWithToggleItem("Allow others to see your age"),
            NormalListWithToggleItem("Allow others to see my gender"),
            NormalListWithToggleItem("Allow others to see your achievements"),

        )

normalListWithToggleView.setData(toggleList)
    }
    enum class NormalListWithoutToggleAction{
        NAME,
        GENDER,
        AGE,

    }
    data class  NormalListWithoutToggleItem(val label:String, val action:NormalListWithoutToggleAction,val state:Boolean = true)
class NormalListWithoutToggleAdapter(val list: ArrayList<NormalListWithoutToggleItem>,val activity:Activity):BaseAdapter(){
    override fun getCount(): Int {
return list.size
    }

    override fun getItem(position: Int): Any {
return  list[position]
    }

    override fun getItemId(position: Int): Long {
return  list.size.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
   val view:View?
   val vh:ViewHolder?
   if (convertView==null){
       view=activity.layoutInflater.inflate(R.layout.edit_profile_normal_list_layout,parent,false)
       vh = ViewHolder(view)
       view.tag = vh
   }else{
       view = convertView
       vh = view.tag as ViewHolder

   }
        vh.label.text = list[position].label

        if (position==0 ){
            vh.rootLayout.background = AppCompatResources.getDrawable(activity,R.drawable.setting_top_list_item_white_background)
        }else if(position == (list.size-1)){
            vh.rootLayout.background = AppCompatResources.getDrawable(activity,R.drawable.setting_bottom_list_item_white_background)

        }
        if(!list[position].state){
            vh.rootLayout.setBackgroundColor(activity.getColor(R.color.gray))
        }
        return  view
    }
class ViewHolder(view:View){
    val label:TextView
    val rootLayout:ConstraintLayout
    init{
        label = view.findViewById<TextView>(R.id.label)
        rootLayout = view.findViewById<ConstraintLayout>(R.id.root_layout)
    }
}
}
}