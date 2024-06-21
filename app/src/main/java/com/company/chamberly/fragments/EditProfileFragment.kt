package com.company.chamberly.fragments

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.company.chamberly.R
import com.company.chamberly.constant.Gender
import com.company.chamberly.customview.SettingSectionWithListAndSwitchButton
import com.company.chamberly.customview.SettingSectionWithListAndSwitchButton.ListWithSwitchItem
import com.company.chamberly.viewmodels.ProfileViewModel

class EditProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private  var chosenGender:Int = Gender.MALE_GENDER_INT
    private var age:Int = 24
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
      backButtonView(view)
     list(view)
whatOtherPersonCanSeeList(view)


    }
private  fun backButtonView(view:View){
    val backButton = view.findViewById<ImageButton>(R.id.back_button)
    backButton.setOnClickListener {
        requireActivity().supportFragmentManager.popBackStack()
    }
}
    private fun list(view: View){
        val editInfoList =
            view.findViewById<ListView>(R.id.normal_list_without_toggle)
        val name = profileViewModel.getNameFromSharePreference()
        val gender:String = when(profileViewModel.getGenderFromSharePreference()){
            Gender.MALE_GENDER_INT->"Male"
            Gender.FEMALE_GENDER_INT->"Female"
            Gender.OTHER_GENDER_INT->"Other"
else ->"Male"
        }
        val age = profileViewModel.getAgeFromSharePreference().toString() +" y/o"
        val editInfoListData = arrayListOf<EditInfoListDataItem>(
            EditInfoListDataItem(name,false){
                                                //TODO: when name is clicked
            },


            EditInfoListDataItem(gender){
showGenderPicker(profileViewModel.getGenderFromSharePreference(),it)
            },
            EditInfoListDataItem(age){
                showAgePicker(it)
            }
        )
        editInfoList.adapter = EditInfoListAdapter(
            list = editInfoListData,
            activity = requireActivity()
        )
    }
    private fun whatOtherPersonCanSeeList(view:View){
        val whatOtherPersonSeeListView =
            view.findViewById<SettingSectionWithListAndSwitchButton>(R.id.list_with_switch)
        val toggleList = arrayListOf<ListWithSwitchItem>(
            ListWithSwitchItem("Allow others to see your age", state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("seeAge")){
                                                               _,check->
                                                              profileViewModel.updateSectionInAccount("privacy","seeAge",check)
            },
            ListWithSwitchItem("Allow others to see my gender",state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("seeGender")){
                    _,check->
                profileViewModel.updateSectionInAccount("privacy","seeGender",check)
            },
            ListWithSwitchItem("Allow others to see your achievements",state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("seeAchievements")){
                    _,check->
                profileViewModel.updateSectionInAccount("privacy","seeAchievements",check)
            },

            )

        whatOtherPersonSeeListView.setData(toggleList)
    }
    private fun showGenderPicker(
        genderInt: Int,
        chosenGenderText: TextView
    ) {
        val dialog = Dialog(requireContext(), R.style.Dialog)
        var selectedGenderInt = genderInt
        dialog.setContentView(R.layout.dialog_box_gender_picker)
        val window = dialog.window
        val wlp = window?.attributes
        wlp?.gravity = Gravity.BOTTOM
        wlp?.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        window?.attributes = wlp
        dialog.setCancelable(true)
        val maleGenderContainer = dialog.findViewById<LinearLayout>(R.id.male_gender_container)
        val femaleGenderContainer = dialog.findViewById<LinearLayout>(R.id.female_gender_container)
        val otherGenderContainer = dialog.findViewById<LinearLayout>(R.id.other_gender_container)
        val saveButton = dialog.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            var text = ""
            when (chosenGender) {
                Gender.MALE_GENDER_INT -> {
                    text = "Male"

                    profileViewModel.setGenderToDatabase("male",chosenGender)
                }

                Gender.FEMALE_GENDER_INT -> {
                    text = "Female"
                    profileViewModel.setGenderToDatabase("female",chosenGender)

                }

                Gender.OTHER_GENDER_INT -> {
                    text = "Other"
                    profileViewModel.setGenderToDatabase("other",chosenGender)

                }
            }
            chosenGenderText.setText(text)
            dialog.dismiss()
        }
        updateGenderCard(
            genderInt,
            dialog,
            maleGenderContainer,
            femaleGenderContainer,
            otherGenderContainer
        )

        femaleGenderContainer.setOnClickListener {
            updateGenderCard(
                Gender.FEMALE_GENDER_INT,
                dialog,
                maleGenderContainer,
                femaleGenderContainer,
                otherGenderContainer
            )
        }
        maleGenderContainer.setOnClickListener {
            updateGenderCard(
                Gender.MALE_GENDER_INT,
                dialog,
                maleGenderContainer,
                femaleGenderContainer,
                otherGenderContainer
            )


        }
        otherGenderContainer.setOnClickListener {
            updateGenderCard(
                Gender.OTHER_GENDER_INT,
                dialog,
                maleGenderContainer,
                femaleGenderContainer,
                otherGenderContainer
            )

        }
        dialog.show()
    }
    private fun updateGenderCard(
        selectedGenderInt: Int,
        dialog: Dialog,
        maleGenderContainer: LinearLayout,
        femaleGenderContainer: LinearLayout,
        otherGenderContainer: LinearLayout,
    ) {
        chosenGender = selectedGenderInt
        val maleGenderIcon = dialog.findViewById<ImageView>(R.id.male_gender_icon)
        val maleGenderText = dialog.findViewById<TextView>(R.id.male_gender_text)
        val femaleGenderIcon = dialog.findViewById<ImageView>(R.id.female_gender_icon)
        val femaleGenderText = dialog.findViewById<TextView>(R.id.female_gender_text)
        val otherGenderIcon = dialog.findViewById<ImageView>(R.id.other_gender_icon)
        val otherGenderText = dialog.findViewById<TextView>(R.id.other_gender_text)
        maleGenderText.setTextColor(
            if (selectedGenderInt == Gender.MALE_GENDER_INT) requireActivity().getColor(R.color.white) else requireActivity().getColor(
                R.color.black
            )
        )
        maleGenderIcon.setColorFilter(
            if (selectedGenderInt == Gender.MALE_GENDER_INT) requireActivity().getColor(R.color.white) else requireActivity().getColor(
                R.color.primary
            )
        )
        maleGenderContainer.setBackgroundResource(if (selectedGenderInt == Gender.MALE_GENDER_INT) R.drawable.bg_rounded_primary else R.drawable.white_corner_background)

        femaleGenderText.setTextColor(
            if (selectedGenderInt == Gender.FEMALE_GENDER_INT) requireActivity().getColor(R.color.white) else requireActivity().getColor(
                R.color.black
            )
        )
        femaleGenderIcon.setColorFilter(
            if (selectedGenderInt == Gender.FEMALE_GENDER_INT) requireActivity().getColor(R.color.white) else requireActivity().getColor(
                R.color.primary
            )
        )
        femaleGenderContainer.setBackgroundResource(if (selectedGenderInt == Gender.FEMALE_GENDER_INT) R.drawable.bg_rounded_primary else R.drawable.white_corner_background)

        otherGenderText.setTextColor(
            if (selectedGenderInt == Gender.OTHER_GENDER_INT) requireActivity().getColor(R.color.white) else requireActivity().getColor(
                R.color.black
            )
        )
        otherGenderIcon.setColorFilter(
            if (selectedGenderInt == Gender.OTHER_GENDER_INT) requireActivity().getColor(R.color.white) else requireActivity().getColor(
                R.color.primary
            )
        )
        otherGenderContainer.setBackgroundResource(if (selectedGenderInt == Gender.OTHER_GENDER_INT) R.drawable.bg_rounded_primary else R.drawable.white_corner_background)

    }

    data class EditInfoListDataItem(
        val label: String,
        val state: Boolean = true,
        val action: (textView:TextView)->Unit,
    )
    private fun showAgePicker(textView: TextView) {
        val dialog = Dialog(requireContext(), R.style.Dialog)
        val window = dialog.window
        val wlp = window?.attributes
        wlp?.gravity = Gravity.BOTTOM
        wlp?.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        window?.attributes = wlp
        dialog.setContentView(R.layout.dialog_box_age_picker)
        dialog.setCancelable(true)
        val agePicker = dialog.findViewById<NumberPicker>(R.id.age_picker)
        val save_button = dialog.findViewById<Button>(R.id.save_button)
        agePicker.value = 18
        agePicker.maxValue = 90
        agePicker.minValue = 23
        agePicker.setOnValueChangedListener { picker, oldVal, newVal ->
            if (newVal != age) {
                age = newVal
            }

        }
        save_button.setOnClickListener {
            updateAge(textView)
            dialog.dismiss()
        }
        dialog.show()
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

    }
    private fun updateAge(textView: TextView) {

        textView.text = "$age y/o"
        profileViewModel.setAgeToDatabase(age)
    }
    class EditInfoListAdapter(
        val list: ArrayList<EditInfoListDataItem>,
        val activity: Activity
    ) : BaseAdapter()
    {
        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Any {
            return list[position]
        }

        override fun getItemId(position: Int): Long {
            return list.size.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            val view: View?
            val vh: ViewHolder?
            if (convertView == null) {
                view = activity.layoutInflater.inflate(
                    R.layout.edit_profile_normal_list_layout,
                    parent,
                    false
                )
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder

            }
            vh.label.text = list[position].label

            if (position == 0) {
                vh.rootLayout.background = AppCompatResources.getDrawable(
                    activity,
                    R.drawable.setting_top_list_item_white_background
                )
            } else if (position == (list.size - 1)) {
                vh.rootLayout.background = AppCompatResources.getDrawable(
                    activity,
                    R.drawable.setting_bottom_list_item_white_background
                )

            }
            if (!list[position].state) {
                vh.rootLayout.setBackgroundColor(activity.getColor(R.color.gray))
            }
            vh.rootLayout.setOnClickListener{
                list[position].action(vh.label)
            }
            return view
        }

        class ViewHolder(view: View) {
            val label: TextView
            val rootLayout: ConstraintLayout

            init {
                label = view.findViewById<TextView>(R.id.label)
                rootLayout = view.findViewById<ConstraintLayout>(R.id.root_layout)
            }
        }

    }
}