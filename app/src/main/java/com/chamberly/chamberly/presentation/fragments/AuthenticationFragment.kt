package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chamberly.chamberly.R

class AuthenticationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_authentication, container, false)
//        val navHostFragment = childFragmentManager.findFragmentById(R.id.authNavHostFragment) as NavHostFragment
//        val navController = navHostFragment.navController
        return view
    }
}