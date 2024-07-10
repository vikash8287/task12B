package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel

class LoginFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmLoginButton: Button
    private lateinit var signupButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_login, container, false)
        emailField = view.findViewById(R.id.emailField)
        passwordField = view.findViewById(R.id.passwordField)
        confirmLoginButton = view.findViewById(R.id.confirmLogIn)
        signupButton = view.findViewById(R.id.goToSignUp)

        confirmLoginButton.setOnClickListener {
            Log.d("HERE", "LOGGING IN")
            disableAllButtons()
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            var errorMessage = ""
            if(email.isBlank()) {
                errorMessage = "Please enter your email ID"
            }
            if(password.contains(" ")) {
                errorMessage = "Password cannot contain whitespaces"
            }
            if(password.length < 8) {
                errorMessage = "Password must be at least 8 characters"
            }
            if(errorMessage.isNotBlank()) {
                Toast.makeText(
                    requireContext(),
                    errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
                enableAllButtons()
                return@setOnClickListener
            }
            userViewModel.loginUser(email, password, onComplete = { enableAllButtons() })
        }

        signupButton.setOnClickListener {
            val navController = findNavController()
            navController.popBackStack(
                R.id.welcome_fragment, inclusive = false
            )
            navController.navigate(
                R.id.signup_fragment,
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

    private fun disableAllButtons() {
        emailField.isEnabled = false
        passwordField.isEnabled = false
        confirmLoginButton.isEnabled = false
        signupButton.isEnabled = false
    }

    private fun enableAllButtons() {
        emailField.isEnabled = true
        passwordField.isEnabled = true
        confirmLoginButton.isEnabled = true
        signupButton.isEnabled = true
    }
}