package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.chamberly.chamberly.R

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_login, container, false)
        val emailField = view.findViewById<EditText>(R.id.emailField)
        val passwordField = view.findViewById<EditText>(R.id.passwordField)
        val confirmLoginButton = view.findViewById<Button>(R.id.confirmLogIn)
        val signupButton = view.findViewById<Button>(R.id.goToSignUp)

        confirmLoginButton.setOnClickListener {
//            user
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
}