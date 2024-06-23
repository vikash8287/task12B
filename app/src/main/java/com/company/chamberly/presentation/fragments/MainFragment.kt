package com.company.chamberly.presentation.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.company.chamberly.R
import com.company.chamberly.presentation.viewmodels.UserViewModel
import com.company.chamberly.utils.Role
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val firestore = Firebase.firestore

    private var homeButton: ImageButton? = null
    private var myChambersButton: ImageButton? = null
    private var rightNavHostFragment: NavHostFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val homeNavHostFragment =
            childFragmentManager.findFragmentById(R.id.homeNavHostFragment) as? NavHostFragment
        val leftNavHostFragment =
            childFragmentManager.findFragmentById(R.id.leftNavHostFragment) as? NavHostFragment
        rightNavHostFragment =
            childFragmentManager.findFragmentById(R.id.rightNavHostFragment) as? NavHostFragment
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val profilePictureButton = view.findViewById<ImageButton>(R.id.profilePic)

        if(homeNavHostFragment == null) {
            //Currently in large screen layout
            setupLargeScreenNavigation(leftNavHostFragment!!, rightNavHostFragment!!)
        } else {
            //Currently in small screen layout
            myChambersButton = view.findViewById(R.id.myChambersButton)
            homeButton = view.findViewById(R.id.homeButton)
            setupSmallScreenNavigation(homeNavHostFragment)
        }

        usernameTextView.text = userViewModel.userState.value?.displayName ?: "Anonymous"

//        userViewModel.userState.observe(viewLifecycleOwner) {
//            if(it.entitlement == Entitlement.CHAMBERLY_PLUS) {
//                // User is subscribed
//            }
//        }

        profilePictureButton.setOnClickListener {
            showProfileOptionsPopup(it)
        }
        return view
    }

    private fun setupSmallScreenNavigation(homeNavHostFragment: NavHostFragment) {
        val homeNavController = homeNavHostFragment.navController
        homeNavController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.home_fragment -> {
                    homeButton?.isEnabled = false
                    myChambersButton?.isEnabled = true
                    myChambersButton?.setImageResource(R.drawable.mychambers)
                    homeButton?.setImageResource(R.drawable.home)
                }
                R.id.active_chambers_fragment -> {
                    homeButton?.isEnabled = true
                    myChambersButton?.isEnabled = false
                    myChambersButton?.setImageResource(R.drawable.mychambersactive)
                    homeButton?.setImageResource(R.drawable.homeinactive)
                }
            }
        }
        homeButton?.setOnClickListener {
            homeNavController.popBackStack(R.id.home_fragment, inclusive = false)
        }

        myChambersButton?.setOnClickListener {
            homeNavController.navigate(
                R.id.active_chambers_fragment,
                null,
                navOptions = navOptions {
                    anim {
                        enter = R.anim.slide_in
                        exit = R.anim.slide_out
                        popEnter = R.anim.slide_in
                        popExit = R.anim.slide_out
                    }
                }
            )
        }
    }

    private fun setupLargeScreenNavigation(
        leftNavHostFragment: NavHostFragment,
        rightNavHostFragment: NavHostFragment
    ) {
        val leftNavController = leftNavHostFragment.navController
        val rightNavController = rightNavHostFragment.navController

        rightNavController
            .navigate(
                R.id.activeChambersFragment,
                null,
                navOptions {
                    apply {
                        popUpTo(R.id.home_fragment) {
                            inclusive = true
                        }
                    }
                }
            )
    }

    override fun onResume() {
        super.onResume()
        val rightNavController = rightNavHostFragment?.navController

        rightNavController?.let {
            if (it.currentDestination?.id == R.id.homeFragment) {
                it.navigate(
                    R.id.activeChambersFragment,
                    null,
                    navOptions {
                        apply {
                            popUpTo(R.id.home_fragment) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
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

        userViewModel.appState.observe(viewLifecycleOwner) {
            subscribeButton.visibility =
                if (it.areExperimentalFeaturesEnabled) { View.VISIBLE }
                else { View.GONE }
        }

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
            showAccountDeleteDialog()
        }

        showPrivacyPolicyButton.setOnClickListener {
            profileOptionsPopUp.dismiss()
            showPrivacyPolicy()
        }

        submitFeedbackButton.setOnClickListener {
            submitFeedback(profileOptionsPopUp)
        }

        subscribeButton.setOnClickListener {
            profileOptionsPopUp.dismiss()
            findNavController()
                .navigate(
                    R.id.subscriptionFragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                            popEnter = R.anim.slide_in
                            popExit = R.anim.slide_out
                        }
                    }
                )
        }

        //TODO: Later, rectify positioning of the popup
        val params = WindowManager.LayoutParams()
        params.copyFrom(profileOptionsPopUp.window?.attributes)
        params.gravity = Gravity.TOP or Gravity.START
//        params.x = buttonView.width
        params.y = buttonView.bottom
        profileOptionsPopUp.window?.attributes = params
        profileOptionsPopUp.show()
    }

    private fun showAccountDeleteDialog() {
        val dialog = Dialog(requireActivity(), R.style.Dialog)

        dialog.setContentView(R.layout.dialog_delete_account_confirmation)
        val confirmButton = dialog.findViewById<TextView>(R.id.confirmButton)
        val dismissButton = dialog.findViewById<TextView>(R.id.dismissButton)

        confirmButton.setOnClickListener {
            userViewModel.deleteAccount()
            dialog.dismiss()
        }

        dismissButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showPrivacyPolicy() {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chamberly.net/privacy-policy"))
        startActivity(browserIntent)
    }

    private fun submitFeedback(dialog: Dialog) {
        dialog.setContentView(R.layout.dialog_feedback)
        val submitButton = dialog.findViewById<Button>(R.id.submitFeedbackButton)
        val dismissButton = dialog.findViewById<Button>(R.id.dismissFeedbackDialogButton)
        val editText = dialog.findViewById<EditText>(R.id.feedback_text)
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
                dialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Your feedback has been submitted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}