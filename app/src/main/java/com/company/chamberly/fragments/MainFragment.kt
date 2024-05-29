package com.company.chamberly.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.company.chamberly.R
import com.company.chamberly.utils.Role
import com.company.chamberly.viewmodels.UserViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val firestore = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val homeNavHostFragment = childFragmentManager.findFragmentById(R.id.homeNavHostFragment) as NavHostFragment
        val homeNavController = homeNavHostFragment.navController
        val myChambersButton = view.findViewById<ImageButton>(R.id.myChambersButton)
        val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val profilePictureButton = view.findViewById<ImageButton>(R.id.profilePic)
        val addChamberButton = view.findViewById<ImageButton>(R.id.btnAddChamber)

        usernameTextView.text = userViewModel.userState.value?.displayName ?: "Anonymous"

        homeNavController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.home_fragment -> {
                    homeButton.isEnabled = false
                    myChambersButton.isEnabled = true
                    myChambersButton.setImageResource(R.drawable.mychambers)
                    homeButton.setImageResource(R.drawable.home)
                    addChamberButton.visibility = View.GONE
                }
                R.id.active_chambers_fragment -> {
                    homeButton.isEnabled = true
                    myChambersButton.isEnabled = false
                    myChambersButton.setImageResource(R.drawable.mychambersactive)
                    homeButton.setImageResource(R.drawable.homeinactive)
                    addChamberButton.visibility = View.VISIBLE
                }
            }

            homeButton.setOnClickListener {
                homeNavController.popBackStack(R.id.home_fragment, inclusive = false)
            }

            myChambersButton.setOnClickListener {
                homeNavController.navigate(
                    R.id.active_chambers_fragment,
                    null,
                    navOptions = navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
            }

            profilePictureButton.setOnClickListener {
                showProfileOptionsPopup(it)
            }
            addChamberButton.setOnClickListener {
                requireParentFragment()
                    .findNavController()
                    .navigate(
                        R.id.topic_create_fragment,
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
        return view
    }

    private fun showProfileOptionsPopup(buttonView: View) {
        val profileOptionsPopUp = Dialog(requireContext(), R.style.Dialog)
        profileOptionsPopUp.setContentView(R.layout.popup_profile_options)

        val deleteAccountButton = profileOptionsPopUp.findViewById<TextView>(R.id.delete_account)
        val showPrivacyPolicyButton = profileOptionsPopUp.findViewById<TextView>(R.id.show_privacy_policy)
        val submitFeedbackButton = profileOptionsPopUp.findViewById<TextView>(R.id.submit_feedback)

        val roleSelectorButton = profileOptionsPopUp.findViewById<RadioGroup>(R.id.role_selector_group)
        val confirmRoleChangeView = profileOptionsPopUp.findViewById<LinearLayout>(R.id.confirm_role_change_view)
        val confirmRoleChangeButton = profileOptionsPopUp.findViewById<TextView>(R.id.confirm_role_change_button)
        val ventorButton = profileOptionsPopUp.findViewById<RadioButton>(R.id.role_ventor)
        val listenerButton = profileOptionsPopUp.findViewById<RadioButton>(R.id.role_listener)
        val subscribeButton = profileOptionsPopUp.findViewById<TextView>(R.id.subscribe_button)

        var isListener = userViewModel.userState.value?.role == Role.LISTENER

        roleSelectorButton.setOnCheckedChangeListener { _, selectedButton ->
            listenerButton.setTextColor(if(selectedButton == R.id.role_listener) Color.BLACK else Color.WHITE)
            ventorButton.setTextColor(if(selectedButton == R.id.role_ventor) Color.BLACK else Color.WHITE)

            if(isListener && selectedButton == R.id.role_ventor) {
                confirmRoleChangeView.visibility = View.VISIBLE
                confirmRoleChangeButton.setOnClickListener {
                    isListener = false
                    userViewModel.setRole(Role.VENTOR)
                    confirmRoleChangeView.visibility = View.GONE
                }
            } else if(!isListener && selectedButton == R.id.role_listener) {
                confirmRoleChangeView.visibility = View.VISIBLE
                confirmRoleChangeButton.setOnClickListener {
                    isListener = true
                    userViewModel.setRole(Role.LISTENER)
                    confirmRoleChangeView.visibility = View.GONE
                }
            } else {
                confirmRoleChangeView.visibility = View.GONE
            }
        }
        roleSelectorButton.check(if(isListener) R.id.role_listener else R.id.role_ventor)


        deleteAccountButton.setOnClickListener {
            profileOptionsPopUp.dismiss()
            userViewModel.deleteAccount()
        }
        showPrivacyPolicyButton.setOnClickListener {
            profileOptionsPopUp.dismiss()
            showPrivacyPolicy()
        }
        submitFeedbackButton.setOnClickListener {
            submitFeedback(profileOptionsPopUp)
        }
        subscribeButton.setOnClickListener {
            showBottomSheet()
        }

        val params = WindowManager.LayoutParams()
        params.copyFrom(profileOptionsPopUp.window?.attributes)
        params.gravity = Gravity.TOP
        params.y = buttonView.bottom
        profileOptionsPopUp.window?.attributes = params
        profileOptionsPopUp.show()
    }

    private fun showPrivacyPolicy() {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chamberly.net/privacy-policy"))
        startActivity(browserIntent)
    }

    private fun submitFeedback(dialog: Dialog) {
        dialog.setContentView(R.layout.dialog_feedback)
        dialog.show()
        val submitButton = dialog.findViewById<Button>(R.id.submitFeedbackButton)
        val dismissButton = dialog.findViewById<Button>(R.id.dismissFeedbackDialogButton)
        val editText = dialog.findViewById<EditText>(R.id.feedback_text)
        val feedbackSuccessText = dialog.findViewById<TextView>(R.id.feedback_success_text)
        dismissButton?.setOnClickListener {
            dialog.dismiss()
        }

        submitButton?.setOnClickListener {
            val feedbackText = "Android: ${editText.text}"
            val feedbackRef = firestore.collection("Feedback").document()
            val uid = userViewModel.userState.value!!.UID
            val displayName = userViewModel.userState.value!!.displayName
            feedbackRef.set(mapOf(
                "byName" to displayName,
                "byUID" to uid,
                "feedbackData" to feedbackText,
                "timestamp" to FieldValue.serverTimestamp()
            )).addOnSuccessListener {
                editText.visibility = View.GONE
                feedbackSuccessText.visibility = View.VISIBLE
                submitButton.visibility = View.GONE
            }
        }
    }

    private fun showBottomSheet() {
        val bottomSheet = SubscriptionBottomSheet()
        bottomSheet.show(childFragmentManager, bottomSheet.tag)
    }
}