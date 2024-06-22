package com.company.chamberly.presentation.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.company.chamberly.R.color
import com.company.chamberly.R.layout
import com.company.chamberly.R.string
import com.company.chamberly.R.style
import com.company.chamberly.models.Topic
import com.company.chamberly.presentation.adapters.PendingTopicsListAdapter
import com.company.chamberly.presentation.adapters.TopicAdapter
import com.company.chamberly.presentation.viewmodels.UserViewModel
import com.company.chamberly.utils.Role
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yalantis.library.Koloda
import com.yalantis.library.KolodaListener


class TopicSearchFragment : Fragment(), KolodaListener {

    private val userViewModel: UserViewModel by activityViewModels()

    private var isFirstTimeEmpty = true
    private var lastDocumentSnapshot: Any? = null
    private val fetchedTopics: MutableLiveData<MutableList<Topic>> =
        MutableLiveData(mutableListOf())
    private val firestore = Firebase.firestore
    private var roleField: String = ""
    private var areTopicsAvailable = true

    private lateinit var kolodaView: Koloda
    private lateinit var kolodaAdapter: TopicAdapter
    private var pendingTopicsRecyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(layout.fragment_topic_search, container, false)
        roleField =
            if(userViewModel.userState.value!!.role == Role.LISTENER) "lflWeight"
            else "lfvWeight"
        kolodaView = view.findViewById(R.id.koloda)
        val dismissButton = view.findViewById<ImageButton>(R.id.ic_skip)
        val joinButton = view.findViewById<ImageButton>(R.id.ic_chat)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        val buttonsView = view.findViewById<LinearLayout>(R.id.buttonsLayout)
        val emptyStateView = view.findViewById<RelativeLayout>(R.id.emptyStateView)
        pendingTopicsRecyclerView = view.findViewById(R.id.pendingTopicsRecyclerView)
        kolodaView.kolodaListener = this

        kolodaAdapter = TopicAdapter()
        kolodaView.adapter = kolodaAdapter

        fetchedTopics.observe(viewLifecycleOwner) {
            kolodaAdapter.updateTopics(it)

            kolodaView.visibility = if(kolodaAdapter.count == 0) View.GONE else View.VISIBLE
            buttonsView.visibility = if(kolodaAdapter.count == 0) View.GONE else View.VISIBLE
            emptyStateView.visibility = if(kolodaAdapter.count == 0) View.VISIBLE else View.GONE
        }
        val layoutManager = LinearLayoutManager(requireContext())

        if (pendingTopicsRecyclerView != null) {
            // Initialize recycler view to show pending topics
            val pendingTopicsListAdapter = PendingTopicsListAdapter()
            pendingTopicsRecyclerView!!.adapter = pendingTopicsListAdapter

            userViewModel.pendingTopics.observe(viewLifecycleOwner) {
                val titles = mutableListOf<String>()
                for(topic in it) {
                    if (topic.isNotBlank()) {
                        titles.add(userViewModel.pendingTopicTitles[topic] ?: "")
                    }
                }
                pendingTopicsListAdapter.addItems(titles)
            }
            pendingTopicsRecyclerView!!.layoutManager = layoutManager
            val dividerItemDecoration =
                DividerItemDecoration(
                    requireContext(),
                    layoutManager.orientation
                )
            pendingTopicsRecyclerView!!.addItemDecoration(dividerItemDecoration)
        }

        dismissButton.setOnClickListener { kolodaView.onClickLeft() }
        joinButton.setOnClickListener { kolodaView.onClickRight() }

        backButton.setOnClickListener {  findNavController().popBackStack() }

        userViewModel.logEventToAnalytics("chamber_search")
        userViewModel.logEventToAnalytics("landed_on_cards_view")
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
        val topic = kolodaAdapter.getItem(position + 1)

        if (
            userViewModel.pendingTopics.value!!.size >=
            (userViewModel.maxAllowedTopics.value ?: 25)
        ) {
            showTooManyTopicsDialog()
        } else {
            userViewModel.waitOnTopic(topic.TopicID, topic.TopicTitle)
        }

        super.onCardSwipedRight(position)
    }

    override fun onCardSwipedLeft(position: Int) {
        fetchedTopics.value!!.remove(kolodaAdapter.getItem(position+1))
        userViewModel.dismissTopic()
        super.onCardSwipedLeft(position)
    }

    override fun onEmptyDeck() {
        if(isFirstTimeEmpty) { isFirstTimeEmpty = false }
        else if(areTopicsAvailable) { getTopics() }
        super.onEmptyDeck()
    }

    //TODO: Move this function to the viewModel later
    private fun getTopics() {
        val updatedTopics = mutableListOf<Topic>()
        val query: Query =
            if(lastDocumentSnapshot != null)  {
                firestore
                    .collection("TopicIds")
                    .orderBy(
                        roleField,
                        Query.Direction.DESCENDING
                    )
                    .startAfter(lastDocumentSnapshot)
                    .limit(8)
            } else {
                firestore
                    .collection("TopicIds")
                    .orderBy(
                        roleField,
                        Query.Direction.DESCENDING
                    )
                    .limit(8)
            }
        query.get()
            .addOnSuccessListener { querySnapshot ->
                if(querySnapshot.isEmpty) {
                    fetchedTopics.postValue(mutableListOf())
                    areTopicsAvailable = false
                    return@addOnSuccessListener
                }
                for (documentSnapshot in querySnapshot) {
                    val topic = documentSnapshot.toObject(Topic::class.java)
                        .copy(TopicID = documentSnapshot.id)

                    if(topic.TopicID !in userViewModel.pendingTopics.value!!) {
                        updatedTopics.add(topic)
                    }
                }
                fetchedTopics.postValue(updatedTopics)
                lastDocumentSnapshot =  querySnapshot.documents.lastOrNull()?.get("timestamp")
            }
            .addOnFailureListener { exception ->
                Log.e("QUERY_ERROR", exception.toString())
            }
    }

    private fun showTooManyTopicsDialog() {
        val dialog = Dialog(requireContext(), style.Dialog)

        dialog.setContentView(layout.cancel_procrastination_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        val heading = dialog.findViewById<TextView>(R.id.too_many_topics_title)
        val message = dialog.findViewById<TextView>(R.id.too_many_topics_body)
        val confirmButton = dialog.findViewById<Button>(R.id.cancelProcrastinationButton)
        val dismissButton = dialog.findViewById<Button>(R.id.dismissDialogButton)
        val loadingIndicator =
            dialog.findViewById<LinearProgressIndicator>(R.id.loading_indicator)

        confirmButton.setOnClickListener {
            confirmButton.isEnabled = false
            loadingIndicator.visibility = View.VISIBLE
            dismissButton.isEnabled = false
            userViewModel.stopProcrastination(
                callback = {
                    confirmButton.visibility = View.GONE
                    dismissButton.isEnabled = true
                    dismissButton.setTextColor(resources.getColor(color.green))
                    loadingIndicator.visibility = View.GONE
                    heading.text = getString(string.procrastination_cancel_success_title)
                    message.text = getString(string.procrastination_cancel_success_message)
                    dismissButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }
            )
        }

        dismissButton.setOnClickListener {
            dialog.dismiss()
            findNavController().popBackStack()
        }
    }
}