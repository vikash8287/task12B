package com.company.chamberly.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val chambersSection = view.findViewById<SettingSectionWithListViewToggleButton>(R.id.chamber_section)
        val chambersSectionList   = arrayListOf<SettingSectionWithListViewToggleButton.NormalListWithToggleItem>(
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Remainders","To check up on your chambers", toggleChangeListener = {
                buttonView,  isChecked->
                Log.i("Checked Remainder",isChecked.toString())

            }),


            )

        chambersSection.setData(chambersSectionList,)

        val journalSection = view.findViewById<SettingSectionWithListViewToggleButton>(R.id.journal_section)
        val journalSectionList   = arrayListOf<SettingSectionWithListViewToggleButton.NormalListWithToggleItem>(
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Journal","Daily reminders to write your journal"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Plupi","Reminders to chat with Plupi"),


            )

        journalSection.setData(journalSectionList, )
        val promotionalAndOtherSection = view.findViewById<SettingSectionWithListViewToggleButton>(R.id.promotional_and_other_section)
        val promotionalAndOtherSectionList   = arrayListOf<SettingSectionWithListViewToggleButton.NormalListWithToggleItem>(
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Coins","Daily reminders to collect your coins"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Discounts","When we offer discounts on purchases"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Exciting app updates","When Chamberly gets major updates"),
            SettingSectionWithListViewToggleButton.NormalListWithToggleItem("Check up","Reminder to use Chamberly"),


            )

        promotionalAndOtherSection.setData(promotionalAndOtherSectionList, )

    }
}