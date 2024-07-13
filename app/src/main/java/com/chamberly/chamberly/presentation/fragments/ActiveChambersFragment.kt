package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.adapters.ChambersRecyclerViewAdapter
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActiveChambersFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var emptyStateView: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChambersRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_active_chambers, container, false)
        emptyStateView = view.findViewById(R.id.emptyStateView)
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView = view.findViewById(R.id.rvChambers)
        adapter =
            ChambersRecyclerViewAdapter(userViewModel.userState.value!!.UID) { chamber ->
                // Handle click, navigate to ChatActivity
                userViewModel.openChamber(chamber.chamberID)
            }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(requireContext(), layoutManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)
        userViewModel.myChambers.observe(viewLifecycleOwner) { chambers ->
            if (chambers.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyStateView.visibility = View.VISIBLE
            } else {
                adapter.updateChambers(chambers)
                recyclerView.visibility = View.VISIBLE
                emptyStateView.visibility = View.GONE
            }
        }
        return view
    }
}