package com.chamberly.chamberly.presentation.fragments

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.LeaderBoard
import com.chamberly.chamberly.presentation.adapters.LeaderBoardAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlin.math.abs


class LeaderBoardFragment : Fragment() {
    private lateinit var leaderBoardAdapter: LeaderBoardAdapter
    private lateinit var leaderBoardList: MutableList<LeaderBoard>
    private lateinit var countdownTextView: TextView
    private lateinit var rankingChangeTextView: TextView
    private lateinit var upDownImageView: ImageView
    private lateinit var changePlacesTextView:TextView
    private lateinit var periodButton: String
    private lateinit var name1:TextView
    private lateinit var name2:TextView
    private lateinit var name3:TextView
    private lateinit var coin1:TextView
    private lateinit var coin2:TextView
    private lateinit var coin3:TextView
    private lateinit var firstPlaceProfilePic: ImageView
    private lateinit var secondPlaceProfilePic:ImageView
    private lateinit var thirdPlaceProfilePic:ImageView
    private lateinit var todayCacheList: MutableList<LeaderBoard>
    private lateinit var weekCacheList: MutableList<LeaderBoard>
    private lateinit var monthCacheList: MutableList<LeaderBoard>

    private val refreshInterval: Long = 60 * 1000

    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if(periodButton=="today")loadLeaderBoard("today")
            else if(periodButton=="thisWeek")loadLeaderBoard("thisWeek")
            else if(periodButton=="thisMonth")loadLeaderBoard("thisMonth")
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_leader_board, container, false)
        leaderBoardList = mutableListOf()
        todayCacheList= mutableListOf()
        weekCacheList= mutableListOf()
        monthCacheList= mutableListOf()

        periodButton="today"
        loadLeaderBoard("today")

        countdownTextView = view.findViewById(R.id.countdownTextView)
        upDownImageView=view.findViewById(R.id.upDownImageView)
        rankingChangeTextView=view.findViewById(R.id.rankingChangeTextView)
        changePlacesTextView=view.findViewById(R.id.changePlacesTextView)
        name1=view.findViewById(R.id.top3name1)
        name2=view.findViewById(R.id.top3name2)
        name3=view.findViewById(R.id.top3name3)
        coin1=view.findViewById(R.id.top3coins1)
        coin2=view.findViewById(R.id.top3coins2)
        coin3=view.findViewById(R.id.top3coins3)
        firstPlaceProfilePic=view.findViewById(R.id.firstPlacePic)
        secondPlaceProfilePic=view.findViewById(R.id.secondPlacePic)
        thirdPlaceProfilePic=view.findViewById(R.id.thirdPlacePic)

        val btnToday = view.findViewById<Button>(R.id.btntoday)
        val btnThisWeek = view.findViewById<Button>(R.id.btnweek)
        val btnThisMonth = view.findViewById<Button>(R.id.btnmonth)
        val leaderBoardRecyclerView= view.findViewById<RecyclerView>(R.id.leaderboard_recyclerView)

        leaderBoardRecyclerView.layoutManager = LinearLayoutManager(context)
        leaderBoardAdapter = LeaderBoardAdapter(leaderBoardList)
        leaderBoardRecyclerView.adapter = leaderBoardAdapter

        setButtonBackground(btnToday,btnThisWeek,btnThisMonth)
        countdownTextView.paint.textSkewX = -0.30f

        btnToday.setOnClickListener {
            periodButton="today"
            if(todayCacheList.isEmpty())loadLeaderBoard("today")
            else updateTop3withRecyclerView("today")
            setButtonBackground(btnToday,btnThisWeek,btnThisMonth)
        }
        btnThisWeek.setOnClickListener {
            periodButton="thisWeek"
            if(weekCacheList.isEmpty())loadLeaderBoard("thisWeek")
            else updateTop3withRecyclerView("thisWeek")
            setButtonBackground(btnThisWeek,btnToday,btnThisMonth)
        }
        btnThisMonth.setOnClickListener {
            periodButton="thisMonth"
            if(monthCacheList.isEmpty())loadLeaderBoard("thisMonth")
            else updateTop3withRecyclerView("thisMonth")
            setButtonBackground(btnThisMonth,btnThisWeek,btnToday)
        }
        view.findViewById<ImageView>(R.id.leaderboard_back_btn).setOnClickListener {
            findNavController().navigateUp()
        }

        startCountdownTimer()
        handler.post(countdownRunnable)

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
            }
            "thisWeek" -> {
                leaderBoardList.addAll(weekCacheList)
            }
            "thisMonth" -> {
                leaderBoardList.addAll(monthCacheList)
            }
        }
        for (i in 0 until minOf(3, leaderBoardList.size)) {
            when (i) {
                0 -> {
                    name1.text = leaderBoardList[i].name
                    coin1.text = leaderBoardList[i].auxiCoins.toString()
                    val imageName = leaderBoardList[i].avatarName
                    setProfilePic(imageName,1)
                }
                1 -> {
                    name2.text = leaderBoardList[i].name
                    coin2.text = leaderBoardList[i].auxiCoins.toString()
                    val imageName = leaderBoardList[i].avatarName
                    setProfilePic(imageName,2)
                }
                2 -> {
                    name3.text = leaderBoardList[i].name
                    coin3.text = leaderBoardList[i].auxiCoins.toString()
                    val imageName = leaderBoardList[i].avatarName
                    setProfilePic(imageName,3)
                }
            }
        }
        updateRanking(leaderBoardList, period)
        leaderBoardAdapter.updateDisplayedList(periodButton)
        leaderBoardAdapter.notifyDataSetChanged()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(countdownRunnable)
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
    private fun startCountdownTimer() {
        val timer = object : CountDownTimer(refreshInterval, 1000) {
            @SuppressLint("DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) % 60
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                countdownTextView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                // Restart the countdown timer
                startCountdownTimer()
            }
        }
        timer.start()
    }

    private fun loadLeaderBoard(period: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("LeaderBoard")
            .get()
            .addOnSuccessListener { documents ->
                // Clear the relevant cache list before populating
                todayCacheList.clear()
                weekCacheList.clear()
                monthCacheList.clear()

                val currentTimestamp = System.currentTimeMillis()
                val startNextDayStamp = getStartOfNextDayInMillis()
                val startNextWeekStamp = getStartOfNextWeekInMillis()
                val startNextMonthStamp = getStartOfNextMonthInMillis()

                for (document in documents) {
                    val leaderBoard = document.toObject(LeaderBoard::class.java)
                    val docRef = db.collection("LeaderBoard").document(leaderBoard.uid)

                    // Reset fields if current timestamp exceeds the start of the next period
                    val updates = mutableMapOf<String, Any>()

                    if (currentTimestamp >= startNextDayStamp) {
                        leaderBoard.earnedToday = 0
                        updates["earnedToday"] = 0
                    }
                    if (currentTimestamp >= startNextWeekStamp) {
                        leaderBoard.earnedThisWeek = 0
                        updates["earnedThisWeek"] = 0
                    }
                    if (currentTimestamp >= startNextMonthStamp) {
                        leaderBoard.earnedThisMonth = 0
                        updates["earnedThisMonth"] = 0
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

                    // Create separate copies for each period
                    val todayLeaderBoard = leaderBoard.copy(earnedToday = leaderBoard.earnedToday, auxiCoins = leaderBoard.earnedToday)
                    val weekLeaderBoard = leaderBoard.copy(earnedThisWeek = leaderBoard.earnedThisWeek, auxiCoins = leaderBoard.earnedThisWeek)
                    val monthLeaderBoard = leaderBoard.copy(earnedThisMonth = leaderBoard.earnedThisMonth, auxiCoins = leaderBoard.earnedThisMonth)

                    todayCacheList.add(todayLeaderBoard)
                    weekCacheList.add(weekLeaderBoard)
                    monthCacheList.add(monthLeaderBoard)
                }

                todayCacheList.sortByDescending { it.auxiCoins }
                weekCacheList.sortByDescending { it.auxiCoins }
                monthCacheList.sortByDescending { it.auxiCoins }

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

            if (oldRank != i + 1) {
                val change = oldRank - (i + 1)
                if (leaderBoard.uid == currentUserUid) {
                    showRankingChange(change)
                }

                // Update the rank and save it to Firestore
                when (period) {
                    "today" -> {
                        leaderBoard.todayRank = i + 1
                        docRef.update("todayRank", i + 1)
                        docRef.update("todayChangeRank", change)
                    }

                    "thisWeek" -> {
                        leaderBoard.weekRank = i + 1
                        docRef.update("weekRank", i + 1)
                        docRef.update("weekChangeRank", change)
                    }

                    "thisMonth" -> {
                        leaderBoard.monthRank = i + 1
                        docRef.update("monthRank", i + 1)
                        docRef.update("monthChangeRank", change)
                    }
                }
            }
            else if(leaderBoard.uid==currentUserUid){
                docRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val todayRankChange = document.getLong("todayChangeRank")?.toInt()
                            val weekRankChange = document.getLong("weekChangeRank")?.toInt()
                            val monthRankChange = document.getLong("monthChangeRank")?.toInt()

                            if (todayRankChange != null && weekRankChange != null && monthRankChange != null) {
                          when(period){
                              "today"->showRankingChange(todayRankChange)
                              "thisWeek"->showRankingChange(weekRankChange)
                              "thisMonth"->showRankingChange(monthRankChange)
                          }
                          }
                        }
                    }
            }
        }
        }

    private fun showRankingChange(change:Int){
         if(change<0){
            upDownImageView.setImageResource(R.drawable.down)
            rankingChangeTextView.text=abs(change).toString()
            rankingChangeTextView.setTextColor(resources.getColor(R.color.new_red))
            changePlacesTextView.setTextColor(resources.getColor(R.color.new_red))
         }
        else{
             upDownImageView.setImageResource(R.drawable.up)
             rankingChangeTextView.text=abs(change).toString()
             rankingChangeTextView.setTextColor(resources.getColor(R.color.grass_green))
             changePlacesTextView.setTextColor(resources.getColor(R.color.grass_green))
        }
    }

    private fun getStartOfNextDayInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfNextWeekInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfNextMonthInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
