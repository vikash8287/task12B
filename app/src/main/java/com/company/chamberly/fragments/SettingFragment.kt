package com.company.chamberly.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.company.chamberly.R
import org.w3c.dom.Text

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
val view = inflater.inflate(R.layout.fragment_setting,container,false)
settingUpList(view)
        return view

    }

    private  fun settingUpList(view: View) {
     val listView:ListView =view.findViewById<ListView>(R.id.setting_list)
        val listItems= arrayListOf(
           SettingItem("Edit profile",SettingAction.EditProfile),SettingItem("Notifications",SettingAction.Notifications),SettingItem("Feedback or report a problem",SettingAction.FeedbackOrReport),SettingItem("Terms and Conditions",SettingAction.TermsAndCondition))
   listView.adapter = SettingListAdapter(listItems,requireActivity())

        listView.setOnItemClickListener{
                _, _, position,_ ->

when(listItems[position].action){

    SettingAction.EditProfile->{
        requireParentFragment().findNavController().navigate(
            R.id.edit_profile_setting_fragment,
            null,
            navOptions {
                anim {
                    enter = R.anim.slide_in
                    exit = R.anim.slide_out
                }
            }
        )
    }
    SettingAction.Notifications->{
        requireParentFragment().findNavController().navigate(
            R.id.notification_setting_fragment,
            null,
            navOptions {
                anim {
                    enter = R.anim.slide_in
                    exit = R.anim.slide_out
                }
            }
        )
    }
    SettingAction.FeedbackOrReport->{

    }
    SettingAction.TermsAndCondition->{

    }




}
        }
    }

    enum class SettingAction {
        EditProfile,
        Notifications,
        FeedbackOrReport,
        TermsAndCondition
    }
    data class SettingItem(val title: String, val action: SettingAction)

private  class SettingListAdapter(private  val listItems:ArrayList<SettingItem>,private val context: Activity) : BaseAdapter(){
    override fun getCount(): Int {
return  listItems.size
    }

    override fun getItem(position: Int): Any {
return listItems[position]
    }
    override fun getItemId(position: Int): Long {
return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
 val view:View?
        val vh:ListViewHolder?
        if(convertView==null){
 view = context.layoutInflater.inflate(R.layout.setting_list_item_layout,parent,false)
vh = ListViewHolder(view)
       view.tag = vh
   }else{
       view=convertView
        vh = view.tag as ListViewHolder

   }
        vh.label.text= listItems[position].title
        if (position==0){
            vh.rootLayout.background = AppCompatResources.getDrawable(context,R.drawable.setting_top_list_item_white_background)
        }else if(position == (listItems.size-1)){
            vh.rootLayout.background = AppCompatResources.getDrawable(context,R.drawable.setting_bottom_list_item_white_background)

        }
        return view
    }

    class ListViewHolder(view:View){
         val label:TextView
         val rootLayout:ConstraintLayout
        init {
label= view.findViewById<TextView>(R.id.label)
            rootLayout = view.findViewById(R.id.root_layout)

        }
    }
}


}
