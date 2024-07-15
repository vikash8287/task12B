package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.adapters.ChamberAdapter
import com.chamberly.chamberly.models.Chamber
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yalantis.library.Koloda
import com.yalantis.library.KolodaListener

class ChamberSearchFragment : Fragment(), KolodaListener {

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var kolodaView: Koloda
    private lateinit var kolodaAdapter: ChamberAdapter

    private var isFirstTimeEmpty = true
    private var areChambersAvailable = true
    private var lastTimestamp: Any? = null
    private val firestore = Firebase.firestore
    private val fetchedChambers: MutableLiveData<MutableList<Chamber>> =
        MutableLiveData(mutableListOf())
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chamber_search, container, false)

        val dismissButton = view.findViewById<ImageButton>(R.id.ic_skip)
        val joinButton = view.findViewById<ImageButton>(R.id.ic_chat)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        val buttonsView = view.findViewById<LinearLayout>(R.id.buttonsLayout)
        val emptyStateView = view.findViewById<RelativeLayout>(R.id.emptyStateView)
        kolodaView = view.findViewById(R.id.koloda)

        kolodaView.kolodaListener = this

        kolodaAdapter = ChamberAdapter()
        kolodaView.adapter = kolodaAdapter

        dismissButton.setOnClickListener { kolodaView.onClickLeft() }
        joinButton.setOnClickListener { kolodaView.onClickRight() }
        backButton.setOnClickListener { findNavController().popBackStack() }

        fetchedChambers.observe(viewLifecycleOwner) {
            kolodaAdapter.updateChambers(it)

            kolodaView.visibility = if(kolodaAdapter.count == 0) View.GONE else View.VISIBLE
            buttonsView.visibility = if(kolodaAdapter.count == 0) View.GONE else View.VISIBLE
            emptyStateView.visibility = if(kolodaAdapter.count == 0) View.VISIBLE else View.GONE
        }
        return view
    }

    override fun onCardDrag(position: Int, cardView: View, progress: Float) {
        val rightSwipeOverlay = cardView.findViewById<LinearLayout>(R.id.rightSwipeOverlay)
        val leftSwipeOverlay = cardView.findViewById<LinearLayout>(R.id.leftSwipeOverlay)

        rightSwipeOverlay.visibility =
            if (progress > 0.05f) { View.VISIBLE }
            else { View.GONE }
        leftSwipeOverlay.visibility =
            if (progress < -0.05f) { View.VISIBLE }
            else { View.GONE }

        super.onCardDrag(position, cardView, progress)
    }

    override fun onCardSwipedRight(position: Int) {
        val chamber = kolodaAdapter.getItem(position + 1)
        joinChamber(chamber)
        super.onCardSwipedRight(position)
    }

    override fun onEmptyDeck() {
        if (isFirstTimeEmpty) { isFirstTimeEmpty = false }
        else { getChambers() }
        super.onEmptyDeck()
    }
    private fun joinChamber(chamber: Chamber) {
        kolodaView.isEnabled = false
        userViewModel.joinChamber(chamber.groupChatId)
    }

    override fun onPause() {
        kolodaView.isEnabled = true
        super.onPause()
    }

    private fun isChamberVacant(chamber: Chamber): Boolean {
        return userViewModel.isChamberVacant(chamber.members, chamber.membersLimit)
    }

    private fun getChambers() {
        val updatedChambers = mutableListOf<Chamber>()
        val query =
            firestore
                .collection("GroupChatIds")
                .whereEqualTo("isLocked", false)
                .whereEqualTo("publishedPool", true)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .apply {
                    if(lastTimestamp != null) {
                        startAfter(lastTimestamp)
                    }
                }
                .limit(4)

        query
            .get()
            .addOnSuccessListener { querySnapshot ->
                if(querySnapshot.isEmpty) {
                    fetchedChambers.postValue(mutableListOf())
                    areChambersAvailable = false
                    return@addOnSuccessListener
                }
                for (documentSnapshot in querySnapshot) {
                    val chamber = documentSnapshot.toObject(Chamber::class.java)
                        .copy(groupChatId = documentSnapshot.id)
                    if (isChamberVacant(chamber = chamber)) {
                        updatedChambers.add(chamber)
                    }
                }
                fetchedChambers.postValue(updatedChambers)
                lastTimestamp = querySnapshot.documents.lastOrNull()?.get("timestamp")
            }
    }
}