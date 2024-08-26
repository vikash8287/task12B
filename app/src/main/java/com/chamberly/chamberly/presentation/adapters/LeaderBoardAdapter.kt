package com.chamberly.chamberly.presentation.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.LeaderBoard
import com.google.firebase.auth.FirebaseAuth

class LeaderBoardAdapter(
    private val leaderBoardList: List<LeaderBoard>
) : RecyclerView.Adapter<LeaderBoardAdapter.LeaderBoardViewHolder>() {
    private val displayedList: MutableList<LeaderBoard> = mutableListOf()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    init {
        var currentUserInTopTen = false
        var currentUserLeaderBoard: LeaderBoard?=null

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderBoardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_leaderboard, parent, false)
        return LeaderBoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderBoardViewHolder, position: Int) {
        val leaderBoard = leaderBoardList[position]
        holder.bind(leaderBoard,position)
    }

    override fun getItemCount(): Int {
        return leaderBoardList.size
    }

    inner class LeaderBoardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val userName: TextView = view.findViewById(R.id.leaderboard_user_name)
        private val earnedCoins: TextView = view.findViewById(R.id.leaderboard_user_coins)
        private val rank: TextView = view.findViewById(R.id.rank)
        private val leaderboardUserImageView: ImageView = view.findViewById(R.id.leaderboard_user_image)
        fun bind(leaderBoard: LeaderBoard, position: Int) {
               userName.text = leaderBoard.name
               earnedCoins.text = leaderBoard.auxiCoins.toString()
               rank.text =(position+1).toString()
               val imageName = leaderBoard.avatarName
               val imageFileName = "$imageName.png"
               if (imageName != "" ) {
                val assetManager = itemView.context.assets
                val inputStream = assetManager.open("avatars/$imageName.imageset/$imageFileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                leaderboardUserImageView.setImageBitmap(bitmap)
            }
        }
    }
}
