package com.company.chamberly.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.company.chamberly.R
import com.company.chamberly.constant.Gender
import com.company.chamberly.models.ProfileInfo
import com.company.chamberly.viewmodels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileFragment: Fragment() {
    private val profileViewModel:ProfileViewModel by activityViewModels()
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val uid = firebaseAuth.currentUser?.uid
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var  nameTextView:TextView
    lateinit var  roleSelectorButton:RadioGroup
    lateinit var  genderContainer:LinearLayout
    lateinit var chosenGenderTextView:TextView
    lateinit var  chosenGenderImageView:ImageView
    lateinit var agePickerTextView:TextView
    lateinit var bioEditText:EditText
    lateinit var bioSaveButtonContainer:ConstraintLayout
    lateinit var  rootView:ConstraintLayout
    lateinit var saveBioButton:Button
lateinit var settingButton:ImageButton
    lateinit var backButton:ImageButton
    lateinit var coinText:TextView
    lateinit var numberOfPeopleView :TextView
    lateinit var ratingBar:RatingBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("cache", Context.MODE_PRIVATE)
        profileViewModel.setUpProfileInfo()

        nameTextView = view.findViewById<TextView>(R.id.name_text)
        roleSelectorButton = view.findViewById<RadioGroup>(R.id.role_selector_group)
        genderContainer = view.findViewById<LinearLayout>(R.id.gender_container)
        chosenGenderTextView = view.findViewById<TextView>(R.id.gender_text)
        chosenGenderImageView = view.findViewById<ImageView>(R.id.gender_icon)
        agePickerTextView = view.findViewById<TextView>(R.id.age_picker_button)
        bioEditText= view.findViewById<EditText>(R.id.bio_edit_text)
        bioSaveButtonContainer =  view.findViewById<ConstraintLayout>(R.id.save_button_container)
        rootView =    view.findViewById<ConstraintLayout>(R.id.main)
        saveBioButton= view.findViewById<Button>(R.id.save_button)
        settingButton = view.findViewById<ImageButton>(R.id.setting_button)
        backButton= view.findViewById<ImageButton>(R.id.back_button)
        coinText = view.findViewById<TextView>(R.id.coins_number)
         numberOfPeopleView = view.findViewById<TextView>(R.id.number_of_people)
         ratingBar = view.findViewById<RatingBar>(R.id.rating_bar)

        val roleSelectorButton = view.findViewById<RadioGroup>(R.id.role_selector_group)
        val ventorButton = view.findViewById<RadioButton>(R.id.role_ventor)
        val listenerButton = view.findViewById<RadioButton>(R.id.role_listener)
        roleSetView(roleSelectorButton, ventorButton, listenerButton)
        settingUpGenderContainerlistener(
            genderContainer,
            chosenGenderTextView,
            chosenGenderImageView,
            profileViewModel._profileInfo.value!!
        )
        agePickerClickListener()
        bioEditTextListener(view)
        settingButtonListener()
        backButtonListener(view)



        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setValueOfView(profileViewModel._profileInfo.value, view.context)
        val observer = Observer<ProfileInfo> { newProfileInfo ->
            setValueOfView(
                context = view.context,
                value = profileViewModel._profileInfo.value,
            )
        }
        profileViewModel._profileInfo.observe(requireActivity(), observer)
    }
    fun settingButtonListener(){
        settingButton.setOnClickListener {
            requireParentFragment().findNavController().navigate(
                R.id.setting_fragment,
                null,
                navOptions {
                    anim {
                        enter = R.anim.slide_in
                        exit = R.anim.slide_out
                    }
                }
            )
        }
    }
private  fun settingUpGenderContainerlistener(genderContainer:LinearLayout, chosenGenderTextView: TextView, chosenGenderImageView: ImageView, value:ProfileInfo){
    genderContainer.setOnClickListener {
        showGenderPicker(
            chosenGenderIcon = chosenGenderImageView,
            chosenGenderText = chosenGenderTextView,
            value = profileViewModel._profileInfo.value!!,
        )
    }
}
    private fun agePickerClickListener() {
        agePickerTextView.setOnClickListener {
            showAgePicker()
        }
    }
    private fun setValueOfView(
        value: ProfileInfo?,
        context: Context
    ) {

        nameTextView.text =value?.name
        roleSelectorButton.check(if (value?.isListener == true) R.id.role_listener else R.id.role_ventor)
chosenGenderTextView.text = getGenderText(value?.gender!!)
        chosenGenderImageView.setImageDrawable(AppCompatResources.getDrawable(context,getGenderIcon(value.gender)))
        agePickerTextView.text = "${value.age} y/o"
        bioEditText.setText(value.bio)
        coinText.text = value.coins.toString()
        numberOfPeopleView.text = "(${value.noOfPeople})"
        ratingBar.rating = value.rating

    }
    private fun getGenderIcon(gender:Int):Int{
        return when(gender){
            Gender.MALE_GENDER_INT->R.drawable.male_gender_icon
                Gender.FEMALE_GENDER_INT->R.drawable.female_gender_icon
                    Gender.OTHER_GENDER_INT-> R.drawable.other_gender_icon
        else -> R.drawable.male_gender_icon
        }
    }
    private fun getGenderText(gender:Int):String{
        return when(gender){
            Gender.MALE_GENDER_INT->"Male"
            Gender.FEMALE_GENDER_INT->"Female"
            Gender.OTHER_GENDER_INT-> "Other"
            else -> "Male"
        }
    }
    private fun roleSetView(
        roleSelectorButton: RadioGroup,
        ventorButton: RadioButton,
        listenerButton: RadioButton
    ) {
        roleSelectorButton.setOnCheckedChangeListener { _, selectedButton ->
         //   if (selectedButton == R.id.role_listener) isListener = true else isListener = false
            listenerButton.setTextColor(if (selectedButton == R.id.role_listener) Color.BLACK else Color.WHITE)
            ventorButton.setTextColor(if (selectedButton == R.id.role_ventor) Color.BLACK else Color.WHITE)
            val listenerTypeface =
                Typeface.defaultFromStyle(if (selectedButton == R.id.role_listener) Typeface.BOLD else Typeface.NORMAL)
            val ventorTypeface =
                Typeface.defaultFromStyle(if (selectedButton == R.id.role_ventor) Typeface.BOLD else Typeface.NORMAL)
            listenerButton.setTypeface(listenerTypeface)
            ventorButton.setTypeface(ventorTypeface)

            if (selectedButton == R.id.role_listener) profileViewModel.updateIsListener(true) else profileViewModel.updateIsListener(false)

        }
    }










    private fun backButtonListener(view: View) {
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()

        }
    }




    private fun bioEditTextListener(view: View) {

rootView.setOnClickListener{
   bioEditText.clearFocus()
}
        bioEditText.onFocusChangeListener = onFocusChangeBioEditText(view)
        bioSaveButtonListener(view)

    }
private fun onFocusChangeBioEditText(view: View):View.OnFocusChangeListener{
    return View.OnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            bioSaveButtonContainer.visibility = View.VISIBLE

        } else {
            bioSaveButtonContainer.visibility = View.INVISIBLE
            // hiding keyboard
            val imm =
                requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
    private fun bioSaveButtonListener(
        view: View,
    ) {

        saveBioButton.setOnClickListener {
            bioEditText.clearFocus()
            profileViewModel.updateBio(bioEditText.text.toString())
            val imm =requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            bioSaveButtonContainer.visibility = View.INVISIBLE
            bioEditText.clearFocus()

        }

    }



    private fun showGenderPicker(

        chosenGenderIcon: ImageView,
        chosenGenderText: TextView,
        value: ProfileInfo,
    ) {
        val dialog = Dialog(requireContext(), R.style.Dialog)
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
            chosenGenderText.text =getGenderText(value.gender)
            chosenGenderIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(),getGenderIcon(value.gender)))
            profileViewModel.updateGender(profileViewModel._tempGender.value!!)

            dialog.dismiss()
        }

            updateGenderCard(
                value.gender,
                dialog,
                maleGenderContainer,
                femaleGenderContainer,
                otherGenderContainer
            )

        femaleGenderContainer.setOnClickListener{
            profileViewModel.updateTempGender(Gender.FEMALE_GENDER_INT)
        }
        maleGenderContainer.setOnClickListener{
            profileViewModel.updateTempGender(Gender.MALE_GENDER_INT)
        }
        otherGenderContainer.setOnClickListener{
            profileViewModel.updateTempGender(Gender.OTHER_GENDER_INT)
        }
        val tempGenderObserver = Observer<Int> { newtempGender ->
            updateGenderCard(
                newtempGender,
                dialog,
                maleGenderContainer,
                femaleGenderContainer,
                otherGenderContainer
            )

        }
        profileViewModel._tempGender.observe(requireActivity(),tempGenderObserver)
        dialog.show()
    }





    private fun updateGenderCard(
        selectedGenderInt: Int,
        dialog: Dialog,
        maleGenderContainer: LinearLayout,
        femaleGenderContainer: LinearLayout,
        otherGenderContainer: LinearLayout
    ) {
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


    private fun showAgePicker() {
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

        agePicker.maxValue = 90
        agePicker.minValue = 0
        agePicker.setOnValueChangedListener { _, _, newVal ->
            if (newVal != profileViewModel._tempAge.value) {
              profileViewModel.updateTempAge(newVal)
            }

        }
        agePicker.clearFocus()
        agePicker.value  =profileViewModel._profileInfo.value?.age!!
        save_button.setOnClickListener {
            profileViewModel.updateAge(profileViewModel._tempAge.value!!)
            dialog.dismiss()
        }
        dialog.show()
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

    }










}