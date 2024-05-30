package com.company.chamberly.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.company.chamberly.R
import com.company.chamberly.utils.Role
import com.company.chamberly.viewmodels.UserViewModel


class WelcomeFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private val selectedRole = MutableLiveData(Role.VENTOR)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        val createAccountButton = view.findViewById<Button>(R.id.btnCreateAccount)
        val displayNameField = view.findViewById<EditText>(R.id.etEmail)
        val roleSelectorLayout = view.findViewById<RadioGroup>(R.id.role_selector_group)
        val roleVentorButton = view.findViewById<RadioButton>(R.id.role_ventor)
        val roleListenerButton = view.findViewById<RadioButton>(R.id.role_listener)
        val roleMessage = view.findViewById<TextView>(R.id.role_details)

        roleVentorButton.setOnClickListener {
            selectedRole.value = Role.VENTOR
        }

        roleListenerButton.setOnClickListener {
            selectedRole.value = Role.LISTENER
            Log.d("ISCHECKED", roleListenerButton.isChecked.toString())
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
}