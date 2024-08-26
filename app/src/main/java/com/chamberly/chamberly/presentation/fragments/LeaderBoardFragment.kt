package com.chamberly.chamberly.presentation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.models.LeaderBoard
import com.chamberly.chamberly.presentation.adapters.LeaderBoardAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlin.math.abs


class LeaderBoardFragment : Fragment() {
    private lateinit var leaderBoardAdapter: LeaderBoardAdapter
    private lateinit var leaderBoardList: MutableList<LeaderBoard>
    private lateinit var countdownTextView: TextView
    private lateinit var rankingChangeTextView: TextView
    private lateinit var upDownTextView: TextView
    private lateinit var periodButton: String
    private lateinit var name1:TextView
    private lateinit var name2:TextView
    private lateinit var name3:TextView
    private lateinit var coin1:TextView
    private lateinit var coin2:TextView
    private lateinit var coin3:TextView

    private val refreshInterval: Long = 40 * 1000

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

        periodButton="today"
        rankingChangeTextView=view.findViewById(R.id.rankingChange)
        upDownTextView=view.findViewById(R.id.upDownTextview)
        countdownTextView = view.findViewById(R.id.countdownTextView)
        name1=view.findViewById(R.id.top3name1)
        name2=view.findViewById(R.id.top3name2)
        name3=view.findViewById(R.id.top3name3)
        coin1=view.findViewById(R.id.top3coins1)
        coin2=view.findViewById(R.id.top3coins2)
        coin3=view.findViewById(R.id.top3coins3)

        val btnToday = view.findViewById<Button>(R.id.btntoday)
        val btnThisWeek = view.findViewById<Button>(R.id.btnweek)
        val btnThisMonth = view.findViewById<Button>(R.id.btnmonth)
        val leaderBoardRecyclerView= view.findViewById<RecyclerView>(R.id.leaderboard_recyclerView)

        leaderBoardRecyclerView.layoutManager = LinearLayoutManager(context)
        leaderBoardAdapter = LeaderBoardAdapter(leaderBoardList)
        leaderBoardRecyclerView.adapter = leaderBoardAdapter

        btnToday.setOnClickListener {
            periodButton="today"
        }
        btnThisWeek.setOnClickListener {
            periodButton="thisWeek"
        }
        btnThisMonth.setOnClickListener {
            periodButton="thisMonth"
        }

        startCountdownTimer()
        handler.post(countdownRunnable)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(countdownRunnable)
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
                leaderBoardList.clear()
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

                    // Apply filter based on the selected period
                    when (period) {
                        "today" -> leaderBoard.auxiCoins = leaderBoard.earnedToday
                        "thisWeek" -> leaderBoard.auxiCoins = leaderBoard.earnedThisWeek
                        "thisMonth" -> leaderBoard.auxiCoins = leaderBoard.earnedThisMonth
                    }

                    leaderBoardList.add(leaderBoard)
                }

                leaderBoardList.sortByDescending { it.auxiCoins }
                for (i in 0 until 3) {
                    if(leaderBoardList.size>=i+1 && i==0){
                        name1.text=leaderBoardList[i].name
                        coin1.text=leaderBoardList[i].auxiCoins.toString()
                    }
                    else if(leaderBoardList.size>=i+1 && i==1){
                        name2.text=leaderBoardList[i].name
                        coin2.text=leaderBoardList[i].auxiCoins.toString()
                    }
                    else{
                        name3.text=leaderBoardList[i].name
                        coin3.text=leaderBoardList[i].auxiCoins.toString()
                    }
                }
                updateRanking(leaderBoardList, period)
                leaderBoardAdapter.notifyDataSetChanged()
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
                val change = abs(oldRank - (i + 1))
                if (leaderBoard.uid == currentUserUid) {
                    rankingChangeTextView.text = change.toString()
                    if (oldRank - (i + 1) > 0) {
                        upDownTextView.text = "UP"
                    } else {
                        upDownTextView.text = "Down"
                    }
                }

                // Update the rank and save it to Firestore
                when (period) {
                    "today" -> {
                        leaderBoard.todayRank = i + 1
                        docRef.update("todayRank", i + 1)
                    }
                    "thisWeek" -> {
                        leaderBoard.weekRank = i + 1
                        docRef.update("weekRank", i + 1)
                    }
                    "thisMonth" -> {
                        leaderBoard.monthRank = i + 1
                        docRef.update("monthRank", i + 1)
                    }
                }
            }
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

