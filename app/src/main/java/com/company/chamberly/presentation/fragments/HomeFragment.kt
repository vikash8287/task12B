package com.company.chamberly.presentation.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.company.chamberly.R

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val createChamberButton = view.findViewById<Button>(R.id.createChamberButton)
        val createTopicButton = view.findViewById<Button>(R.id.createTopicButton)
        val searchButton = view.findViewById<Button>(R.id.findChamberButton)
        val searchTopicButton = view.findViewById<Button>(R.id.findTopicButton)
        val followUsButton = view.findViewById<Button>(R.id.followUs)

        createChamberButton.setOnClickListener {
            requireParentFragment()
                .requireParentFragment()
                .findNavController()
                .navigate(
                    R.id.action_main_fragment_to_create_chamber_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
        }

        createTopicButton.setOnClickListener {
            requireParentFragment()
                .requireParentFragment()
                .findNavController()
                .navigate(
                    R.id.action_main_fragment_to_create_topic_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
        }

        searchButton.setOnClickListener {
            requireParentFragment()
                .requireParentFragment()
                .findNavController()
                .navigate(
                    R.id.action_main_fragment_to_chamber_search_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
        }

        searchTopicButton.setOnClickListener {
            requireParentFragment()
                .requireParentFragment()
                .findNavController()
                .navigate(
                    R.id.action_main_fragment_to_topic_search_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.anim.slide_in
                            exit = R.anim.slide_out
                        }
                    }
                )
        }

        followUsButton.setOnClickListener {
            openInstagramPage("https://www.instagram.com/chamberly_app/")
        }
        // Inflate the layout for this fragment
        return view
    }

    private fun openInstagramPage(url: String) {
        try {
            // Try to open the Instagram page in the Instagram app
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.setPackage("com.instagram.android")
            startActivity(intent)
        } catch (e: Exception) {
            // If the Instagram app is not installed, open the page in a web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}