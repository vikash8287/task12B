package com.company.chamberly.presentation.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.company.chamberly.R
import com.company.chamberly.presentation.viewmodels.UserViewModel
import com.company.chamberly.utils.Role


class WelcomeFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private val selectedRole = MutableLiveData(Role.VENTOR)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userViewModel.logEventToAnalytics("landed_on_confirm_login_page_vc")
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        val createAccountButton = view.findViewById<Button>(R.id.btnCreateAccount)
        val displayNameField = view.findViewById<EditText>(R.id.etEmail)
        val termsConditionsTextView = view.findViewById<TextView>(R.id.tvTermsConditions)
        val roleSelectorLayout = view.findViewById<RadioGroup>(R.id.role_selector_group)
        val roleVentorButton = view.findViewById<RadioButton>(R.id.role_ventor)
        val roleListenerButton = view.findViewById<RadioButton>(R.id.role_listener)
        val roleMessage = view.findViewById<TextView>(R.id.role_details)
        showTermsAndConditions(termsConditionsTextView)
        roleVentorButton.setOnClickListener {
            selectedRole.value = Role.VENTOR
        }

        roleListenerButton.setOnClickListener {
            selectedRole.value = Role.LISTENER
        }

        selectedRole.observe(viewLifecycleOwner) {
            roleSelectorLayout.check(
                if(it == Role.VENTOR) { R.id.role_ventor }
                else {R.id.role_listener }
            )
            roleMessage.text =
                if (it == Role.VENTOR) { getString(R.string.ventor_details) }
                else { getString(R.string.listener_details) }

            roleVentorButton.setTextColor(
                if(it == Role.VENTOR) { resources.getColor(R.color.black) }
                else { resources.getColor(R.color.white)}
            )

            roleListenerButton.setTextColor(
                if(it == Role.LISTENER) { resources.getColor(R.color.black) }
                else { resources.getColor(R.color.white)}
            )
        }

        createAccountButton.setOnClickListener {
            if (displayNameField.text.toString().isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Please enter a username",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            createAccountButton.isEnabled = false
            displayNameField.isEnabled = false
            userViewModel.registerUser(
                displayName = displayNameField.text.toString(),
                role = selectedRole.value!!,
                onComplete = {
                    createAccountButton.isEnabled = true
                    displayNameField.isEnabled = true
                }
            )
        }
        return view
    }

    private fun showTermsAndConditions(termsConditionsTextView: TextView) {
        val fullText =
            getString(R.string.feel_free_we_do_not_ask_your_real_name_but_it_must_comply_with_the_terms_conditions)

        val spannableString = SpannableString(fullText)

        val termsStart = fullText.indexOf("Terms & Conditions")
        val termsEnd = termsStart + "Terms & Conditions".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.chamberly.net/terms-and-conditions")
                )
                startActivity(browserIntent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#7A7AFF")
                ds.isUnderlineText = false
            }
        }
        spannableString.setSpan(
            clickableSpan,
            termsStart,
            termsEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        termsConditionsTextView.text = spannableString
        termsConditionsTextView.movementMethod = LinkMovementMethod.getInstance()
        termsConditionsTextView.highlightColor = Color.TRANSPARENT
    }
}