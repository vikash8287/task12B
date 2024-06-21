package com.company.chamberly.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.activityViewModels
import com.company.chamberly.R
import com.company.chamberly.customview.SettingSectionWithListAndSwitchButton
import com.company.chamberly.viewmodels.ProfileViewModel


class NotificationsFragment : Fragment() {
val profileViewModel:ProfileViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        val chambersSection = view.findViewById<SettingSectionWithListAndSwitchButton>(R.id.chamber_section)
        val chambersSectionList   = arrayListOf<SettingSectionWithListAndSwitchButton.ListWithSwitchItem>(
            SettingSectionWithListAndSwitchButton.ListWithSwitchItem("Remainders","To check up on your chambers",state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("ChamberReminders") ,switchStateListener = {
                _,  check->
                profileViewModel.updateSectionInAccount("notifications","ChamberReminders",check)


            }),


            )

        chambersSection.setData(chambersSectionList,)

        val journalSection = view.findViewById<SettingSectionWithListAndSwitchButton>(R.id.journal_section)
        val journalSectionList   = arrayListOf<SettingSectionWithListAndSwitchButton.ListWithSwitchItem>(
            SettingSectionWithListAndSwitchButton.ListWithSwitchItem("Journal","Daily reminders to write your journal"),
            SettingSectionWithListAndSwitchButton.ListWithSwitchItem("Plupi","Reminders to chat with Plupi"),


            )

        journalSection.setData(journalSectionList, )
        val promotionalAndOtherSection = view.findViewById<SettingSectionWithListAndSwitchButton>(R.id.promotional_and_other_section)
        val promotionalAndOtherSectionList   = arrayListOf<SettingSectionWithListAndSwitchButton.ListWithSwitchItem>(
            SettingSectionWithListAndSwitchButton.ListWithSwitchItem("Coins","Daily reminders to collect your coins",state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("DailyCoins")){
                    _,check->
                profileViewModel.updateSectionInAccount("notifications","DailyCoins",check)
            },
            SettingSectionWithListAndSwitchButton.ListWithSwitchItem("Discounts","When we offer discounts on purchases",state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("Discounts")){
                    _,check->
                profileViewModel.updateSectionInAccount("notifications","Discounts",check)
            },
            SettingSectionWithListAndSwitchButton.ListWithSwitchItem("Exciting app updates","When Chamberly gets major updates",state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("AppUpdates")){
                    _,check->
                profileViewModel.updateSectionInAccount("notifications","AppUpdates",check)
            },
            SettingSectionWithListAndSwitchButton.ListWithSwitchItem("Check up","Reminder to use Chamberly",state = profileViewModel.getSettingInfoWithBooleanFromSharePreference("Checkup")){
                    _,check->
                profileViewModel.updateSectionInAccount("notifications","Checkup",check)
            },


            )

        promotionalAndOtherSection.setData(promotionalAndOtherSectionList, )

    }
}