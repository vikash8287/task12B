package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel


class WelcomeFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userViewModel.logEventToAnalytics("landed_on_confirm_login_page_vc")
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        val signupButton = view.findViewById<Button>(R.id.signUpButton)
        val loginButton = view.findViewById<Button>(R.id.loginButton)

        signupButton.setOnClickListener {
            findNavController()
                .navigate(
                    R.id.action_welcome_to_signup_fragment,
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
        loginButton.setOnClickListener {
            findNavController()
                .navigate(
                    R.id.action_welcome_to_login_fragment,
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