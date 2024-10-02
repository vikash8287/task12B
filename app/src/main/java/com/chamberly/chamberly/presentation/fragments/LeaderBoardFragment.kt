package com.chamberly.chamberly.presentation.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.LeaderBoard
import com.chamberly.chamberly.presentation.adapters.LeaderBoardAdapter
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlin.math.abs


class LeaderBoardFragment : Fragment() {
    private lateinit var leaderBoardAdapter: LeaderBoardAdapter
    private lateinit var leaderBoardList: MutableList<LeaderBoard>
    private lateinit var countdownTextView: TextView
    private var countdownTimer: CountDownTimer? = null
    private lateinit var rankingChangeTextView: TextView
    private lateinit var upDownImageView: ImageView
    private lateinit var changePlacesTextView:TextView
    private lateinit var periodButton: String
    private lateinit var firstPlaceProfilePic: ImageView
    private lateinit var secondPlaceProfilePic:ImageView
    private lateinit var thirdPlaceProfilePic:ImageView
    private lateinit var todayCacheList: MutableList<LeaderBoard>
    private lateinit var weekCacheList: MutableList<LeaderBoard>
    private lateinit var monthCacheList: MutableList<LeaderBoard>
    private lateinit var sharedPreferences: SharedPreferences
    private val userViewModel: UserViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_leader_board, container, false)
        sharedPreferences = requireContext().getSharedPreferences("cache", Context.MODE_PRIVATE)
        leaderBoardList = mutableListOf()
        todayCacheList= mutableListOf()
        weekCacheList= mutableListOf()
        monthCacheList= mutableListOf()

        periodButton="today"
        loadLeaderBoard("today")
        refreshTimer("today")

        countdownTextView = view.findViewById(R.id.countdownTextView)
        upDownImageView=view.findViewById(R.id.upDownImageView)
        rankingChangeTextView=view.findViewById(R.id.rankingChangeTextView)
        changePlacesTextView=view.findViewById(R.id.changePlacesTextView)
        firstPlaceProfilePic=view.findViewById(R.id.firstPlacePic)
        secondPlaceProfilePic=view.findViewById(R.id.secondPlacePic)
        thirdPlaceProfilePic=view.findViewById(R.id.thirdPlacePic)

        val btnToday = view.findViewById<Button>(R.id.btntoday)
        val btnThisWeek = view.findViewById<Button>(R.id.btnweek)
        val btnThisMonth = view.findViewById<Button>(R.id.btnmonth)
        val leaderBoardRecyclerView= view.findViewById<RecyclerView>(R.id.leaderboard_recyclerView)

        leaderBoardRecyclerView.layoutManager = LinearLayoutManager(context)
        leaderBoardAdapter = LeaderBoardAdapter(leaderBoardList,this.requireContext())
        leaderBoardRecyclerView.adapter = leaderBoardAdapter

        setButtonBackground(btnToday,btnThisWeek,btnThisMonth)
        countdownTextView.paint.textSkewX = -0.30f

        btnToday.setOnClickListener {
            periodButton="today"
            refreshTimer("today")
            if(todayCacheList.isEmpty())loadLeaderBoard("today")
            else updateTop3withRecyclerView("today")
            setButtonBackground(btnToday,btnThisWeek,btnThisMonth)
        }
        btnThisWeek.setOnClickListener {
            periodButton="thisWeek"
            refreshTimer("thisWeek")
            if(weekCacheList.isEmpty())loadLeaderBoard("thisWeek")
            else updateTop3withRecyclerView("thisWeek")
            setButtonBackground(btnThisWeek,btnToday,btnThisMonth)
        }
        btnThisMonth.setOnClickListener {
            periodButton="thisMonth"
            refreshTimer("thisMonth")
            if(monthCacheList.isEmpty())loadLeaderBoard("thisMonth")
            else updateTop3withRecyclerView("thisMonth")
            setButtonBackground(btnThisMonth,btnThisWeek,btnToday)
        }
        view.findViewById<ImageView>(R.id.leaderboard_back_btn).setOnClickListener {
            findNavController().navigateUp()
        }

        return view
    }

    private fun setButtonBackground(btn1: Button,btn2:Button,btn3:Button) {
        btn1.setBackgroundColor(Color.WHITE)
        btn2.setBackgroundColor(Color.TRANSPARENT)
        btn3.setBackgroundColor(Color.TRANSPARENT)
        btn1.setTextColor(resources.getColor(R.color.text_primary))
        btn2.setTextColor(resources.getColor(R.color.white))
        btn3.setTextColor(resources.getColor(R.color.white))
        btn1.setBackgroundResource(R.drawable.leaderboard_button_bg2)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateTop3withRecyclerView(period: String){
        leaderBoardList.clear()
        when(period){
            "today" -> {
                leaderBoardList.addAll(todayCacheList)
                leaderBoardAdapter.periodButton="today"
            }
            "thisWeek" -> {
                leaderBoardList.addAll(weekCacheList)
                leaderBoardAdapter.periodButton="thisWeek"
            }
            "thisMonth" -> {
                leaderBoardList.addAll(monthCacheList)
                leaderBoardAdapter.periodButton="thisMonth"
            }
        }
        for (i in 0 until minOf(3, leaderBoardList.size)) {
            when (i) {
                0 -> {
                    view?.findViewById<TextView>(R.id.top3name1)?.text = leaderBoardList[i].name
                    view?.findViewById<TextView>(R.id.top3coins1)?.text = when(period){
                        "today"->leaderBoardList[i].earnedToday.toString()
                        "thisWeek"->leaderBoardList[i].earnedThisWeek.toString()
                        "thisMonth"->leaderBoardList[i].earnedThisMonth.toString()
                        else -> ({}).toString()
                    }
                    val imageName = leaderBoardList[i].avatarName
                    setProfilePic(imageName,1)
                }
                1 -> {
                    view?.findViewById<TextView>(R.id.top3name2)?.text = leaderBoardList[i].name
                    view?.findViewById<TextView>(R.id.top3coins2)?.text = when(period){
                        "today"->leaderBoardList[i].earnedToday.toString()
                        "thisWeek"->leaderBoardList[i].earnedThisWeek.toString()
                        "thisMonth"->leaderBoardList[i].earnedThisMonth.toString()
                        else -> ({}).toString()
                    }
                    val imageName = leaderBoardList[i].avatarName
                    setProfilePic(imageName,2)
                }
                2 -> {
                    view?.findViewById<TextView>(R.id.top3name3)?.text = leaderBoardList[i].name
                    view?.findViewById<TextView>(R.id.top3coins3)?.text = when(period){
                        "today"->leaderBoardList[i].earnedToday.toString()
                        "thisWeek"->leaderBoardList[i].earnedThisWeek.toString()
                        "thisMonth"->leaderBoardList[i].earnedThisMonth.toString()
                        else -> ({}).toString()
                    }
                    val imageName = leaderBoardList[i].avatarName
                    setProfilePic(imageName,3)
                }
            }
        }
        updateRanking(leaderBoardList, period)
        leaderBoardAdapter.updateDisplayedList()
        leaderBoardAdapter.notifyDataSetChanged()
    }

    private fun setProfilePic(imageName:String,pos:Int){
        val profilePics = arrayListOf(firstPlaceProfilePic, secondPlaceProfilePic, thirdPlaceProfilePic)
        val imageFileName = "$imageName.png"

        if (imageName.isNotEmpty() && pos in 1..3) {
            val assetManager = requireContext().assets
            val inputStream = assetManager.open("avatars/$imageName.imageset/$imageFileName")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            profilePics[pos - 1].setImageBitmap(bitmap)
        }

    }

    private fun refreshTimer(periodButton: String) {
        val currentTime = System.currentTimeMillis()
        val refreshTimeInMillis: Long = when (periodButton) {
            "today" -> userViewModel.getStartOfNextDayInMillis()
            "thisWeek" -> userViewModel.getStartOfNextWeekInMillis()
            "thisMonth" -> userViewModel.getStartOfNextMonthInMillis()
            else -> return
        }

        val remainingTime = refreshTimeInMillis - currentTime
         countdownTimer?.cancel()

       countdownTimer= object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                val seconds = (millisUntilFinished / 1000) % 60
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val days = (millisUntilFinished / (1000 * 60 * 60 * 24)) % 7
                val weeks = millisUntilFinished / (1000 * 60 * 60 * 24 * 7)

                val timeLeftString = when {
                    weeks > 0 -> "${weeks}w:${days}d"
                    days > 0 -> "${days}d:${hours}hr"
                    hours > 0 -> "${hours}hr:${minutes}m"
                    minutes > 0 -> "${minutes}m:${seconds}s"
                    else -> "${seconds}s"
                }
              countdownTextView.text= timeLeftString
            }

            override fun onFinish() {
                refreshTimer(periodButton)
            }
        }.start()
    }
    
    private fun loadLeaderBoard(period: String) {
        val db = FirebaseFirestore.getInstance()
        val currentTimestamp = System.currentTimeMillis()//+86400000 //->1 day in millisecond,604800000 ->1week,2592000000->1 month
        val startNextDayStamp = userViewModel.getStartOfNextDayInMillis()
        val startNextWeekStamp = userViewModel.getStartOfNextWeekInMillis()
        val startNextMonthStamp = userViewModel.getStartOfNextMonthInMillis()
        val upcomingDayReset = sharedPreferences.getLong("upcomingDayReset", 0L)
        val upcomingWeekReset = sharedPreferences.getLong("upcomingWeekReset", 0L)
        val upcomingMonthReset = sharedPreferences.getLong("upcomingMonthReset", 0L)
        db.collection("LeaderBoard")
            .get()
            .addOnSuccessListener { documents ->
                // Clear the relevant cache list before populating
                todayCacheList.clear()
                weekCacheList.clear()
                monthCacheList.clear()

                for (document in documents) {
                    val leaderBoard = document.toObject(LeaderBoard::class.java)
                    val docRef = db.collection("LeaderBoard").document(leaderBoard.uid)

                    // Reset fields if current timestamp exceeds the start of the next period
                    val updates = mutableMapOf<String, Any>()

                    if (currentTimestamp >= upcomingDayReset) {
                        leaderBoard.earnedToday = 0
                        updates["earnedToday"] = 0
                        sharedPreferences.edit().putLong("upcomingDayReset", startNextDayStamp).apply()
                    }
                    if (currentTimestamp >= upcomingWeekReset) {
                        leaderBoard.earnedThisWeek = 0
                        updates["earnedThisWeek"] = 0
                        sharedPreferences.edit().putLong("upcomingWeekReset", startNextWeekStamp).apply()
                    }
                    if (currentTimestamp >= upcomingMonthReset) {
                        leaderBoard.earnedThisMonth = 0
                        updates["earnedThisMonth"] = 0
                        sharedPreferences.edit().putLong("upcomingMonthReset", startNextMonthStamp).apply()
                    }

                    // Update the rank if it is -1
                    if (leaderBoard.todayRank == -1) {
                        leaderBoard.todayRank = documents.size()
                        updates["todayRank"] = leaderBoard.todayRank
                    }
                    if (leaderBoard.weekRank == -1) {
                        leaderBoard.weekRank = documents.size()
                        updates["weekRank"] = leaderBoard.weekRank
                    }
                    if (leaderBoard.monthRank == -1) {
                        leaderBoard.monthRank = documents.size()
                        updates["monthRank"] = leaderBoard.monthRank
                    }

                    // Update Firestore with the new values if any
                    if (updates.isNotEmpty()) {
                        docRef.update(updates)
                            .addOnSuccessListener {
                                println("LeaderBoard document updated successfully")
                            }
                            .addOnFailureListener { e ->
                                println("Error updating LeaderBoard document: $e")
                            }
                    }

                    todayCacheList.add(leaderBoard)
                    weekCacheList.add(leaderBoard)
                    monthCacheList.add(leaderBoard)
                }

                todayCacheList.sortByDescending { it.earnedToday }
                weekCacheList.sortByDescending { it.earnedThisWeek }
                monthCacheList.sortByDescending { it.earnedThisMonth }

                updateTop3withRecyclerView(period)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRanking(leaderBoardList: MutableList<LeaderBoard>, period: String) {
        val db = FirebaseFirestore.getInstance()
        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid

        for (i in leaderBoardList.indices) {
            val leaderBoard = leaderBoardList[i]
            val docRef = db.collection("LeaderBoard").document(leaderBoard.uid)

            val oldRank = when (period) {
                "today" -> leaderBoard.todayRank
                "thisWeek" -> leaderBoard.weekRank
                "thisMonth" -> leaderBoard.monthRank
                else -> continue
            }

            if (oldRank != i + 1 && leaderBoard.uid == currentUserUid) {
                when (period) {
                    "today" -> {
                        leaderBoard.prevTodayRank=leaderBoard.todayRank
                        leaderBoard.todayRank = i + 1
                        docRef.update("todayRank", i + 1)
                        docRef.update("prevTodayRank", leaderBoard.prevTodayRank)
                        docRef.update("lastTodayUpdated",System.currentTimeMillis())
                        showRankingChange(leaderBoard.prevTodayRank-leaderBoard.todayRank,false)
                    }

                    "thisWeek" -> {
                        leaderBoard.prevWeekRank=leaderBoard.weekRank
                        leaderBoard.weekRank = i + 1
                        docRef.update("weekRank", i + 1)
                        docRef.update("prevWeekRank", leaderBoard.prevWeekRank)
                        docRef.update("lastWeekUpdated",System.currentTimeMillis())
                        showRankingChange(leaderBoard.prevWeekRank-leaderBoard.weekRank,false)
                    }

                    "thisMonth" -> {
                        leaderBoard.prevMonthRank=leaderBoard.monthRank
                        leaderBoard.monthRank = i + 1
                        docRef.update("monthRank", i + 1)
                        docRef.update("prevMonthRank", leaderBoard.prevMonthRank)
                        docRef.update("lastMonthUpdated",System.currentTimeMillis())
                        showRankingChange(leaderBoard.prevMonthRank-leaderBoard.monthRank,false)
                    }
                }
            }
            else if(oldRank==i+1 && currentUserUid==leaderBoard.uid){
                when (period) {
                    "today" -> {
                        val currentTimestamp = System.currentTimeMillis()
                        val lastTodayUpdated = (leaderBoard.lastTodayUpdated as? Long) ?: 0L
                        if ((currentTimestamp - lastTodayUpdated) >= 43200000L) showRankingChange(leaderBoard.prevTodayRank - leaderBoard.todayRank, true)
                        else showRankingChange(leaderBoard.prevTodayRank - leaderBoard.todayRank, false)
                    }
                    "thisWeek" -> {
                        val currentTimestamp = System.currentTimeMillis()
                        val lastWeekUpdated = (leaderBoard.lastWeekUpdated as? Long) ?: 0L
                        if ((currentTimestamp - lastWeekUpdated) >= 302400000L) showRankingChange(leaderBoard.prevWeekRank - leaderBoard.weekRank, true)
                        else showRankingChange(leaderBoard.prevWeekRank - leaderBoard.weekRank, false)
                    }
                    "thisMonth" -> {
                        val currentTimestamp = System.currentTimeMillis()
                        val lastMonthUpdated = (leaderBoard.lastMonthUpdated as? Long) ?: 0L
                        if ((currentTimestamp - lastMonthUpdated) >= 1296000000L) showRankingChange(leaderBoard.prevMonthRank - leaderBoard.monthRank, true)
                        else showRankingChange(leaderBoard.prevMonthRank - leaderBoard.monthRank, false)
                    }
                }

            }
        }
        }

    private fun showRankingChange(change:Int,flag:Boolean){
        if(flag){
            if(change<0){
                upDownImageView.setImageResource(R.drawable.nochangedown)
                rankingChangeTextView.text=abs(change).toString()
                rankingChangeTextView.setTextColor(resources.getColor(R.color.gray))
                changePlacesTextView.setTextColor(resources.getColor(R.color.gray))
            }
            else{
                upDownImageView.setImageResource(R.drawable.nochangeup)
                rankingChangeTextView.text=abs(change).toString()
                rankingChangeTextView.setTextColor(resources.getColor(R.color.gray))
                changePlacesTextView.setTextColor(resources.getColor(R.color.gray))
            }
        }
        else{
            if(change<0){
                upDownImageView.setImageResource(R.drawable.down)
                rankingChangeTextView.text=abs(change).toString()
                rankingChangeTextView.setTextColor(resources.getColor(R.color.light_red))
                changePlacesTextView.setTextColor(resources.getColor(R.color.light_red))
            }
            else{
                upDownImageView.setImageResource(R.drawable.up)
                rankingChangeTextView.text=abs(change).toString()
                rankingChangeTextView.setTextColor(resources.getColor(R.color.light_grass_green))
                changePlacesTextView.setTextColor(resources.getColor(R.color.light_grass_green))
            }
        }
    }

}
