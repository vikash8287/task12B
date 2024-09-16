package com.chamberly.chamberly.presentation.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.LeaderBoard
import com.google.firebase.auth.FirebaseAuth


class LeaderBoardAdapter(
    private val leaderBoardList: List<LeaderBoard>,private val context: Context
) : RecyclerView.Adapter<LeaderBoardAdapter.LeaderBoardViewHolder>() {
    private val displayedList: MutableList<LeaderBoard> = mutableListOf()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var periodButton="today"
    @SuppressLint("NotifyDataSetChanged")
    fun updateDisplayedList() {
        displayedList.clear()
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
        private val itemUpDownImageView: ImageView = view.findViewById(R.id.item_upDown_image)
        private val frameLayout: FrameLayout = view.findViewById(R.id.rankframelayout)
        private val unrankedTextView: TextView = view.findViewById(R.id.unranked_textview)
        private val leaderboardUserImageView: ImageView = view.findViewById(R.id.leaderboard_user_image)
        @SuppressLint("SetTextI18n")
        fun bind(leaderBoard: LeaderBoard, position: Int) {
            userName.text = leaderBoard.name
            earnedCoins.text = when(periodButton){
                "today"->leaderBoard.earnedToday.toString()
                "thisWeek"->leaderBoard.earnedThisWeek.toString()
                "thisMonth"->leaderBoard.earnedThisMonth.toString()
                else -> ({}).toString()
            }
            rank.text = (position + 1).toString()
            val imageName = leaderBoard.avatarName
            val imageFileName = "$imageName.png"

            val earnedValue = when(periodButton) {
                "today" -> leaderBoard.earnedToday
                "thisWeek" -> leaderBoard.earnedThisWeek
                "thisMonth" -> leaderBoard.earnedThisMonth
                else -> 0
            }

            if(currentUserId==leaderBoard.uid){
                itemUpDownImageView.visibility = View.VISIBLE
                val currentTimestamp = System.currentTimeMillis()
                when (periodButton) {
                    "today" -> {
                        val lastTodayUpdated = (leaderBoard.lastTodayUpdated as? Long) ?: 0L
                        val change = leaderBoard.prevTodayRank - leaderBoard.todayRank
                        val flag = (currentTimestamp - lastTodayUpdated) >= 43200000L

                        if (flag) {
                            if (change < 0) {
                                itemUpDownImageView.setImageResource(R.drawable.nochangedown)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.gray))
                            } else {
                                itemUpDownImageView.setImageResource(R.drawable.nochangeup)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.gray))
                            }
                        } else {
                            if (change < 0) {
                                itemUpDownImageView.setImageResource(R.drawable.down)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.light_red))
                            } else {
                                itemUpDownImageView.setImageResource(R.drawable.up)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.light_grass_green))
                            }
                        }
                    }

                    "thisWeek" -> {
                        val lastWeekUpdated = (leaderBoard.lastWeekUpdated as? Long) ?: 0L
                        val change = leaderBoard.prevWeekRank - leaderBoard.weekRank
                        val flag = (currentTimestamp - lastWeekUpdated) >= 302400000L

                        if (flag) {
                            if (change < 0) {
                                itemUpDownImageView.setImageResource(R.drawable.nochangedown)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.gray))
                            } else {
                                itemUpDownImageView.setImageResource(R.drawable.nochangeup)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.gray))
                            }
                        } else {
                            if (change < 0) {
                                itemUpDownImageView.setImageResource(R.drawable.down)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.light_red))
                            } else {
                                itemUpDownImageView.setImageResource(R.drawable.up)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.light_grass_green))
                            }
                        }
                    }

                    "thisMonth" -> {
                        val lastMonthUpdated = (leaderBoard.lastMonthUpdated as? Long) ?: 0L
                        val change = leaderBoard.prevMonthRank - leaderBoard.monthRank
                        val flag = (currentTimestamp - lastMonthUpdated) >= 1296000000L

                        if (flag) {
                            if (change < 0) {
                                itemUpDownImageView.setImageResource(R.drawable.nochangedown)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.gray))
                            } else {
                                itemUpDownImageView.setImageResource(R.drawable.nochangeup)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.gray))
                            }
                        } else {
                            if (change < 0) {
                                itemUpDownImageView.setImageResource(R.drawable.down)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.light_red))
                            } else {
                                itemUpDownImageView.setImageResource(R.drawable.up)
                                earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.light_grass_green))
                            }
                        }
                    }
                }

            }
            else {itemUpDownImageView.visibility=View.GONE;earnedCoins.setTextColor(ContextCompat.getColor(context, R.color.royal_purple))}

            if (earnedValue == 0) {
                unrankedTextView.visibility = View.VISIBLE
                unrankedTextView.text = "Unranked"
                unrankedTextView.paint.textSkewX = -0.30f
                frameLayout.visibility = View.GONE
            } else {
                unrankedTextView.visibility = View.GONE
                frameLayout.visibility = View.VISIBLE
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