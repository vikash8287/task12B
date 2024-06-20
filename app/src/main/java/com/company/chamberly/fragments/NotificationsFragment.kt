package com.company.chamberly.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.company.chamberly.R
import com.company.chamberly.customview.SettingSectionWithListViewToggleButton


class NotificationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ChambersSection = view.findViewById<SettingSectionWithListViewToggleButton>(R.id.chamber_section)
        val ChambersSectionList   = arrayListOf<SettingSectionWithListViewToggleButton.NormalListWithToggleItem>(
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Remainders","To check up on your chambers"),


            )

        ChambersSection.setData(ChambersSectionList, AdapterView.OnItemClickListener{
                _,_,position,_->
            Log.d("Position",position.toString())
        })

        val JournalSection = view.findViewById<SettingSectionWithListViewToggleButton>(R.id.journal_section)
        val JournalSectionList   = arrayListOf<SettingSectionWithListViewToggleButton.NormalListWithToggleItem>(
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Journal","Daily reminders to write your journal"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Plupi","Reminders to chat with Plupi"),


            )

        JournalSection.setData(JournalSectionList, AdapterView.OnItemClickListener{
                _,_,position,_->
            Log.d("Position",position.toString())
        })
        val PromotionalAndOtherSection = view.findViewById<SettingSectionWithListViewToggleButton>(R.id.promotional_and_other_section)
        val PromotionalAndOtherSectionList   = arrayListOf<SettingSectionWithListViewToggleButton.NormalListWithToggleItem>(
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Coins","Daily reminders to collect your coins"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Discounts","When we offer discounts on purchases"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Exciting app updates","When Chamberly gets major updates"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Check up","Reminder to use Chamberly"),


            )

        PromotionalAndOtherSection.setData(PromotionalAndOtherSectionList, AdapterView.OnItemClickListener{
                _,_,position,_->
            Log.d("Position",position.toString())
        })

    }
}