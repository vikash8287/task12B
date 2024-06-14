package com.company.chamberly.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.company.chamberly.R
import com.company.chamberly.constant.Gender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileFragment: Fragment() {
    var age: Int = 24
    private var chosenGender = Gender.MALE_GENDER_INT //MALE_GENDER_INT
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val uid = firebaseAuth.currentUser?.uid
    private lateinit var sharedPreferences: SharedPreferences
    private var isListener: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("cache", Context.MODE_PRIVATE)

            isListener = getIsListenerFromSharedPreference()
        age = getAgeFromSharePreference()
        val bioText = getBioTextFromSharePreference()
        chosenGender = getGenderFromSharePreference()

        selectedRoleContainerLogic(view)
        agePickerContainerLogic(view)
        genderPickerContainerLogic(view)
        nameTextLogic(view)
        coinTextLogic(view)
        backButtonLogic(view)
        ratingLogic(view)
        bioEditTextLogic(bioText = bioText.toString(), view = view)



        return view
    }


    private fun coinTextLogic(view: View) {
        val coinText = view.findViewById<TextView>(R.id.coins_number)
        val coins = getCoinsFromSharePreference()
        coinText.setText(coins.toString())
        getUserInfoFromDatabase(coinText)
    }

    private fun nameTextLogic(view: View) {
        val nameText = view.findViewById<TextView>(R.id.name_text)
        nameText.setText(getNameFromSharePreference())
    }
    private fun getCoinsFromSharePreference(): Int {
        return sharedPreferences.getInt("Coins", 0)

    }


    private fun getNameFromSharePreference(): String {
        return sharedPreferences.getString("displayName", "Name") ?: ""

    }

    private fun getUserInfoFromDatabase(coinText: TextView) {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val doc = document.toObject(UserInfo::class.java)!!
                coinText.setText(doc.Coins.toString())
                editor.putInt("Coins",doc.Coins!!)
                editor.apply()
            }
        }.addOnFailureListener {
            Log.d("getCoins", "Failed")
            Log.d("getCoins", it.toString())

        }
    }

    private fun getGenderFromSharePreference(): Int {
        return sharedPreferences.getInt("gender", Gender.MALE_GENDER_INT)

    }

    private fun getBioTextFromSharePreference(): String {
        return sharedPreferences.getString("bio", "default")!!
    }

    private fun getAgeFromSharePreference(): Int {
        return sharedPreferences.getInt("age", 24)

    }

    private fun backButtonLogic(view: View) {
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            //finish()
            // TODO: go back
        }
    }

    private fun genderPickerContainerLogic(view: View) {
        val genderContainer = view.findViewById<LinearLayout>(R.id.gender_container)
        val chosenGenderText =view.findViewById<TextView>(R.id.gender_text)
        val chosenGenderIcon = view.findViewById<ImageView>(R.id.gender_icon)
        var text = ""
        var icon = R.drawable.male_gender_icon
        when (chosenGender) {
            Gender.MALE_GENDER_INT -> {
                text = "Male"
                icon = R.drawable.male_gender_icon
            }

            Gender.FEMALE_GENDER_INT -> {
                text = "Female"
                icon = R.drawable.female_gender_icon


            }

            Gender.OTHER_GENDER_INT -> {
                text = "Other"
                icon = R.drawable.other_gender_icon

            }
        }
        chosenGenderText.setText(text)
        chosenGenderIcon.setImageDrawable(requireActivity().getDrawable(icon))
        genderContainer.setOnClickListener {
            showGenderPicker(
                genderInt = chosenGender,
                chosenGenderText = chosenGenderText,
                chosenGenderIcon = chosenGenderIcon
            )
        }
    }

    private fun agePickerContainerLogic(view: View) {
        val agePickerButton = view.findViewById<TextView>(R.id.age_picker_button)
        agePickerButton.setText(age.toString() + " y/o")
        agePickerButton.setOnClickListener {
            showAgePicker(agePickerButton)
        }
    }

    private fun bioEditTextLogic(bioText: String, view: View) {
        val bioEditText = view.findViewById<EditText>(R.id.bio_edit_text)
        val bioSaveButtonContainer = view.findViewById<ConstraintLayout>(R.id.save_button_container)
        val rootView = view.findViewById<ConstraintLayout>(R.id.main)
rootView.setOnClickListener{
   bioEditText.clearFocus()
}
        bioEditText.setText(bioText)
        bioEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                bioSaveButtonContainer.visibility = View.VISIBLE

                showSaveBioContainer(bioEditText, view, bioSaveButtonContainer)
            } else {
                bioSaveButtonContainer.visibility = View.INVISIBLE
                // hiding keyboard
                val imm =
                    requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    private fun showSaveBioContainer(
        bioEditText: EditText,
        view: View,
        bioSaveButtonContainer: ConstraintLayout
    ) {

        val saveButton:Button = view.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            bioEditText.clearFocus()
            val text = bioEditText.text.toString()
            setBioToDatabase(text)
            val imm =requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            bioSaveButtonContainer.visibility = View.INVISIBLE
            bioEditText.clearFocus()

        }

    }

    private fun selectedRoleContainerLogic(view: View) {
        val roleSelectorButton = view.findViewById<RadioGroup>(R.id.role_selector_group)
        val ventorButton = view.findViewById<RadioButton>(R.id.role_ventor)
        val listenerButton = view.findViewById<RadioButton>(R.id.role_listener)

        roleSelectorButton.setOnCheckedChangeListener { _, selectedButton ->
            if (selectedButton == R.id.role_listener) isListener = true else isListener = false
            listenerButton.setTextColor(if (selectedButton == R.id.role_listener) Color.BLACK else Color.WHITE)
            ventorButton.setTextColor(if (selectedButton == R.id.role_ventor) Color.BLACK else Color.WHITE)
            val listenerTypeface =
                Typeface.defaultFromStyle(if (selectedButton == R.id.role_listener) Typeface.BOLD else Typeface.NORMAL)
            val ventorTypeface =
                Typeface.defaultFromStyle(if (selectedButton == R.id.role_ventor) Typeface.BOLD else Typeface.NORMAL)

            listenerButton.setTypeface(listenerTypeface)
            ventorButton.setTypeface(ventorTypeface)


            if (isListener) setRoleToDatabase("listener") else setRoleToDatabase("ventor")

        }
        roleSelectorButton.check(if (isListener) R.id.role_listener else R.id.role_ventor)
    }


    private fun showGenderPicker(
        genderInt: Int,
        chosenGenderIcon: ImageView,
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
            var icon = R.drawable.male_gender_icon
            when (chosenGender) {
                Gender.MALE_GENDER_INT -> {
                    text = "Male"
                    icon = R.drawable.male_gender_icon

                    setGenderToDatabase("male")
                }

                Gender.FEMALE_GENDER_INT -> {
                    text = "Female"
                    icon = R.drawable.female_gender_icon
                    setGenderToDatabase("female")


                }

                Gender.OTHER_GENDER_INT -> {
                    text = "Other"
                    icon = R.drawable.other_gender_icon
                    setGenderToDatabase("other")

                }
            }
            chosenGenderText.setText(text)
            chosenGenderIcon.setImageDrawable(requireActivity().getDrawable(icon))
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

    private fun ratingLogic(view: View) {
        val numberOfPeopleView = view.findViewById<TextView>(R.id.number_of_people)
        val ratingBar = view.findViewById<RatingBar>(R.id.rating_bar)
        val rating = getRatingFromSharePreference()
        val reviewCount = getReviewCountFromSharedPreference()
        numberOfPeopleView.setText("(${reviewCount.toString()})")
        ratingBar.rating = rating
        getUserRatingFromDataBase(numberOfPeopleView,ratingBar)

    }

    private fun getReviewCountFromSharedPreference(): Int {
        return sharedPreferences.getInt("reviewCount", 0)
    }

    private fun getRatingFromSharePreference(): Float {
        return sharedPreferences.getFloat("rating",0f)
    }

    private fun updateGenderCard(
        selectedGenderInt: Int,
        dialog: Dialog,
        maleGenderContainer: LinearLayout,
        femaleGenderContainer: LinearLayout,
        otherGenderContainer: LinearLayout
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
        setAgeToDatabase(age)
    }

    private fun getIsListenerFromSharedPreference(): Boolean {

        val isListener = sharedPreferences.getBoolean("isListener", false)
        return isListener


    }

    private fun setRoleToDatabase(role: String) {
        val editor = sharedPreferences.edit()
        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.update("selectedRole", role).addOnSuccessListener {
            editor.putBoolean("isListener", if (role.equals("ventor")) false else true)
            Log.d("selectRoleUpdate", "Success")
        }.addOnFailureListener {
            Log.d("selectRoleUpdate", "Failure")

        }
        editor.apply()
    }

    private fun setAgeToDatabase(age: Int) {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.update("age", age).addOnSuccessListener {
            editor.putInt("age", age)
            editor.apply()
            Log.d("ageUpdate", "Success")
        }.addOnFailureListener {
            Log.d("ageUpdate", "Failure")

        }

    }

    private fun setBioToDatabase(bio: String) {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.update("bio", bio).addOnSuccessListener {
            editor.putString("bio", bio)
            editor.apply()

            Log.d("bioUpdate", "Success")
        }.addOnFailureListener {
            Log.d("bioUpdate", "Failure")

        }

    }

    private fun setGenderToDatabase(genderData: String) {
        val editor = sharedPreferences.edit()

        val currUserRef = firestore.collection("Accounts").document(uid!!)
        currUserRef.update("gender", genderData).addOnSuccessListener {
            Log.d("genderUpdate", "Success")
            editor.putInt("gender", chosenGender)
            editor.apply()

        }.addOnFailureListener {
            Log.d("genderUpdate", "Failure")

        }

        currUserRef.update("firstGender", genderData).addOnSuccessListener {
            editor.putInt("firstGender", chosenGender)
            editor.apply()
            Log.d("firstGenderUpdate", "Success")
        }.addOnFailureListener {
            Log.d("firstGenderUpdate", "Failure")

        }
    }
    private fun getUserRatingFromDataBase(numberOfPeopleView: TextView, ratingBar: RatingBar) {
        val editor = sharedPreferences.edit()

        firestore.collection("StarReviews").whereEqualTo("To",uid).orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().addOnSuccessListener {
                document->
            if(document.size()==0){
                numberOfPeopleView.setText("(0)")
                ratingBar.rating = 0f
                return@addOnSuccessListener
            }
            val doc= document.documents[0].data
            val averageRating =  doc?.get("AverageStars")
            val numberOfPeople= doc?.get("ReviewsCount")

            numberOfPeopleView.setText("(${numberOfPeople.toString()})")
            ratingBar.rating = (averageRating as Double).toFloat()
            editor.putInt("reviewCount",numberOfPeople.toString().toInt())
            editor.putFloat("rating", (averageRating as Double).toFloat())
            editor.apply()

            Log.d("Data",doc.toString())
        }.addOnFailureListener {
            Log.d("FirestoreError",it.message.toString())
        }
    }


    data class UserInfo(
        var Coins: Int? = 0
    )

}