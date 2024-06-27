package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel

class CreateTopicFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var tooManyTopicsView: LinearLayout
    private lateinit var clearButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_create_topic, container, false)
        val topicTitleField = view.findViewById<EditText>(R.id.topic_title)
        val createButton = view.findViewById<Button>(R.id.create_button)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        tooManyTopicsView = view.findViewById(R.id.too_many_topics_layout)
        clearButton = view.findViewById(R.id.clear_all_topics_button)
        val maxLength = 50
        val filterArray = arrayOf(InputFilter.LengthFilter(maxLength))
        topicTitleField.filters = filterArray

        userViewModel.getUserChambers()

        userViewModel.pendingTopics.observe(viewLifecycleOwner) {
            if (it.size >= userViewModel.maxAllowedTopics.value!!) {
                showTooManyTopicsMessage(
                    topicTitleField = topicTitleField,
                    createButton = createButton
                )
            }
        }

        createButton.setOnClickListener {
            it.isEnabled = false
            val topicTitle = topicTitleField.text.toString()
            if(topicTitle.isBlank()) {
                topicTitleField.error = "Please enter a title"
            } else {
                userViewModel.createTopic(
                    topicTitle,
                    callback = { findNavController().popBackStack() }
                )
            }
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        return view
    }

    private fun showTooManyTopicsMessage(
        topicTitleField: EditText,
        createButton: Button
    ) {
        createButton.isEnabled = false
        topicTitleField.isEnabled = false
        tooManyTopicsView.visibility = View.VISIBLE

        clearButton.setOnClickListener {
            userViewModel.stopProcrastination(
                callback = {
                    tooManyTopicsView.visibility = View.GONE
                    createButton.isEnabled = true
                    topicTitleField.isEnabled = true
                }
            )
        }
    }
}