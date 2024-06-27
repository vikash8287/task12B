package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.chamberly.chamberly.utils.EMAIL_REGEX
import com.chamberly.chamberly.utils.Role

class SignupFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedRole: Role? = null

    private lateinit var displayNameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var roleGroup: RadioGroup
    private lateinit var nextButton: Button
    private lateinit var loginButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_signup, container, false)
        displayNameField = view.findViewById(R.id.displayNameField)
        emailField = view.findViewById(R.id.emailField)
        passwordField = view.findViewById(R.id.passwordField)
        confirmPasswordField = view.findViewById(R.id.confirmPasswordField)
        roleGroup = view.findViewById(R.id.roleSelectorRadioGroup)
        nextButton = view.findViewById(R.id.proceed)
        loginButton = view.findViewById(R.id.goToLogin)

        roleGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedRole =
                if(checkedId == R.id.roleVentor)    Role.VENTOR
                else                                Role.LISTENER
        }


        nextButton.setOnClickListener {
            var errorMessage = ""
            val password = passwordField.text.toString()
            val confirmedPassword = confirmPasswordField.text.toString()
            val email = emailField.text.toString()
            if(selectedRole == null) {
                errorMessage = "Please select a role first"
            }
            if(password != confirmedPassword) {
                errorMessage = "Password and confirmed password do not match."
            }
            if (password.length < 8) {
                errorMessage = "Password must contain at least 8 characters"
            }
            if (!email.matches(EMAIL_REGEX)) {
                errorMessage = "Email entered is invalid"
            }
            if(errorMessage.isNotBlank()) {
                showToast(errorMessage)
                return@setOnClickListener
            }
            disableButtons()
            userViewModel.registerUser(
                displayName = displayNameField.text.toString(),
                email = emailField.text.toString(),
                password = password,
                role = selectedRole!!,
                onComplete = {
                    enableButtons()
                }
            )
        }

        loginButton.setOnClickListener {
            val navController = findNavController()
            navController
                .popBackStack(
                    R.id.welcome_fragment,
                    inclusive = false
                )
            navController
                .navigate(
                    R.id.login_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in_horizontal
                            exit = R.anim.slide_out_horizontal
                            popEnter = R.anim.slide_in_horizontal
                            popExit = R.anim.slide_out_horizontal
                        }
                    }
                )
        }
        return view
    }

    private fun disableButtons() {
        displayNameField.isEnabled = false
        emailField.isEnabled = false
        passwordField.isEnabled = false
        confirmPasswordField.isEnabled = false
        roleGroup.isEnabled = false
        nextButton.isEnabled = false
        loginButton.isEnabled = false
    }

    private fun enableButtons() {
        displayNameField.isEnabled = true
        emailField.isEnabled = true
        passwordField.isEnabled = true
        confirmPasswordField.isEnabled = true
        roleGroup.isEnabled = true
        nextButton.isEnabled = true
        loginButton.isEnabled = true
    }

    private fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}