package com.chamberly.chamberly.presentation.adapters

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.LeaderBoard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LeaderBoardAdapter(
    private val leaderBoardList: List<LeaderBoard>
) : RecyclerView.Adapter<LeaderBoardAdapter.LeaderBoardViewHolder>() {
    private val displayedList: MutableList<LeaderBoard> = mutableListOf()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    //private var periodButton="today"
    //val docRef = FirebaseFirestore.getInstance().collection("LeaderBoard").document(currentUserId!!)

    @SuppressLint("NotifyDataSetChanged")
    fun updateDisplayedList(period:String) {
        displayedList.clear()
        //periodButton=period
        var currentUserInTopTen = false
        val currentUserLeaderBoard: LeaderBoard?

        for (i in 0 until minOf(10, leaderBoardList.size)) {
            if (leaderBoardList[i].uid == currentUserId) {
                currentUserInTopTen = true
            }
            displayedList.add(leaderBoardList[i])
        }
        if (!currentUserInTopTen) {
            currentUserLeaderBoard = leaderBoardList.find { it.uid == currentUserId }
            if (currentUserLeaderBoard != null && displayedList.size == 10) {
                displayedList[9] = currentUserLeaderBoard
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderBoardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_leaderboard, parent, false)
        return LeaderBoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderBoardViewHolder, position: Int) {
        val leaderBoard = displayedList[position]
        holder.bind(leaderBoard, position)
    }

    override fun getItemCount(): Int {
        return displayedList.size
    }

    inner class LeaderBoardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val userName: TextView = view.findViewById(R.id.leaderboard_user_name)
        private val earnedCoins: TextView = view.findViewById(R.id.leaderboard_user_coins)
        private val rank: TextView = view.findViewById(R.id.rank)
        //private val upDownImageView: ImageView = view.findViewById(R.id.upDownImageView)
        private val frameLayout: FrameLayout = view.findViewById(R.id.rankframelayout)
        private val unrankedTextView: TextView = view.findViewById(R.id.unranked_textview)
        private val leaderboardUserImageView: ImageView = view.findViewById(R.id.leaderboard_user_image)
        @SuppressLint("SetTextI18n")
        fun bind(leaderBoard: LeaderBoard, position: Int) {
            userName.text = leaderBoard.name
            earnedCoins.text = leaderBoard.auxiCoins.toString()
            rank.text = (position + 1).toString()
            val imageName = leaderBoard.avatarName
            val imageFileName = "$imageName.png"

//            if (leaderBoard.uid == currentUserId) {
//                docRef.get()
//                    .addOnSuccessListener { document ->
//                        if (document != null && document.exists()) {
//                            val change = when (periodButton) {
//                                "today" -> document.getLong("todayChangeRank")?.toInt()
//                                "thisWeek" -> document.getLong("weekChangeRank")?.toInt()
//                                "thisMonth" -> document.getLong("monthChangeRank")?.toInt()
//                                else -> 0
//                            }
//                            change?.let {
//                                if (it < 0) {
//                                    upDownImageView.setImageResource(R.drawable.down)
//                                } else {
//                                    upDownImageView.setImageResource(R.drawable.up)
//                                }
//                            }
//                        }
//                    }
//            } else {
//                upDownImageView.visibility = View.GONE
//            }

            if(leaderBoard.auxiCoins==0){
                unrankedTextView.visibility=View.VISIBLE
                unrankedTextView.text="Unranked"
                unrankedTextView.paint.textSkewX=-0.30f
                frameLayout.visibility=View.GONE

            }
            else{
                unrankedTextView.visibility=View.GONE
                frameLayout.visibility=View.VISIBLE
            }
            try{
                if (imageName != "") {
                    val assetManager = itemView.context.assets
                    val inputStream = assetManager.open("avatars/$imageName.imageset/$imageFileName")
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    leaderboardUserImageView.setImageBitmap(bitmap)
                }
            }catch(e:Exception){
                leaderboardUserImageView.setImageResource(R.drawable.top3star)
            }
        }
    }
}