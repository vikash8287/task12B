package com.chamberly.chamberly.presentation.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.viewmodels.ProfileViewModel


class SettingFragment : Fragment() {
val profileViewModel: ProfileViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
val view = inflater.inflate(R.layout.fragment_setting,container,false)
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
          requireActivity().supportFragmentManager.popBackStack()
        }
        val signOut = view.findViewById<Button>(R.id.sign_out)
        signOut.setOnClickListener {
            showSignOutDialogBox()
        }

settingUpList(view)
        return view

    }

    private fun showSignOutDialogBox() {
        val dialog = Dialog(requireContext(),R.style.Dialog)
        dialog.setContentView(R.layout.dialog_box_delete_account)
        val heading = dialog.findViewById<TextView>(R.id.heading)
        val description = dialog.findViewById<TextView>(R.id.description)
        val deleteButton = dialog.findViewById<Button>(R.id.delete_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)

        heading.text = "Delete account and logout"
        description.text = "OBS: You are currently logged in as a guest. This action will also delete your guest account and any data associated with it! Continue?"
        deleteButton.text = "Delete account and logout"
        deleteButton.setOnClickListener {
            profileViewModel.deleteAccount()
            dialog.dismiss()

            requireParentFragment().findNavController().navigate(
                R.id.authentication_fragment,
                null,
                navOptions {
                    anim {
                        enter = R.anim.slide_in
                        exit = R.anim.slide_out
                    }
                }
            )

        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setCancelable(true)
        dialog.show()
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
        showFeedbackOrReportDialogBox()

    }
    SettingAction.TermsAndCondition->{
 showTermsConditionAndPrivacyPolicyDialogBox()

    }




}
        }
    }

    private fun showFeedbackOrReportDialogBox() {
        val dialog = Dialog(requireContext(),R.style.Dialog)
        dialog.setContentView(R.layout.dialog_box_feedback_and_report)
        val buttons = listOf(dialog.findViewById<Button>(R.id.option1),dialog.findViewById<Button>(R.id.option2),dialog.findViewById<Button>(R.id.option3),
            dialog.findViewById<Button>(R.id.option4),
            dialog.findViewById<Button>(R.id.option5),
            dialog.findViewById<Button>(R.id.option6),

        )
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        for(button:Button in buttons){
            button.setOnClickListener {
                profileViewModel.setFeedbackToFirestore(button.text.toString()){
                    dialog.dismiss()
                }
            }
        }
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun showTermsConditionAndPrivacyPolicyDialogBox() {
        val dialog = Dialog(requireContext(),R.style.Dialog)
        dialog.setContentView(R.layout.dialog_box_terms_and_condition_and_privacy_policy)
dialog.setCancelable(true)
        val termsAndConditionButton = dialog.findViewById<Button>(R.id.terms_and_condition)
        val privacyPolicyButton = dialog.findViewById<Button>(R.id.show_privacy_policy)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)

        termsAndConditionButton.setOnClickListener {
            val httpIntent = Intent(Intent.ACTION_VIEW)
            httpIntent.setData(Uri.parse("https://www.chamberly.net/terms-and-conditions"))

            startActivity(httpIntent)
        }
        privacyPolicyButton.setOnClickListener {
            val httpIntent = Intent(Intent.ACTION_VIEW)
            httpIntent.setData(Uri.parse("https://www.chamberly.net/privacy-policy"))

            startActivity(httpIntent)
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
