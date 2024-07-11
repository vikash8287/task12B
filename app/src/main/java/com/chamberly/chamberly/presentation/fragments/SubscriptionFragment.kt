package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.chamberly.chamberly.utils.Entitlement

class SubscriptionFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private val chamberlyPlusBenefits = mutableListOf(
        Pair(R.drawable.ic_chamberly_memeber, "Unlimited messages, for everyone, all chambers"),
        Pair(R.drawable.ic_journal, "Higher Quality Matches"),
        Pair(R.drawable.chat_add_on, "Faster Matches"),
        Pair(R.drawable.ic_heart, "and so much more")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_subscription, container, false)
        val heading = view.findViewById<TextView>(R.id.chamberlyPlusText)
        val chamberlyBenefitsLayout = view.findViewById<LinearLayout>(R.id.chamberlyBenefits)
        val subscribeButton = view.findViewById<LinearLayout>(R.id.subscriptionBtn)
        addHeading(heading)
        for (benefit in chamberlyPlusBenefits) {
            val benefitLayout = getBenefitLayout(benefit.first, benefit.second)
            chamberlyBenefitsLayout.addView(benefitLayout)
        }

        subscribeButton.isEnabled =
            userViewModel.userState.value!!.entitlement == Entitlement.REGULAR

        subscribeButton.setOnClickListener {
            userViewModel.subscribe(requireActivity())
        }
        return view
    }

    private fun addHeading(heading: TextView) {
        val spannableString = SpannableString("Chamberlyplus")
        val color1 = ForegroundColorSpan(resources.getColor(R.color.primary))
        val color2 = ForegroundColorSpan(resources.getColor(R.color.golden))
        spannableString.setSpan(color1, 0, 9, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        spannableString.setSpan(color2, 9, 13, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        heading.text = spannableString
        
    }

    private fun getBenefitLayout(icon: Int, content: String): LinearLayout {
        val layout = LinearLayout(activity)
        val iconButton = ImageView(activity)
        val contentLayout = TextView(activity)

        layout.orientation = LinearLayout.HORIZONTAL
        layout.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        layout.setPadding(20, 8, 20, 8)
        iconButton.setImageResource(icon)
        iconButton.adjustViewBounds = true
        iconButton.maxWidth = 100
        iconButton.maxHeight = 100
        iconButton.foregroundGravity = Gravity.CENTER_VERTICAL
        iconButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
        iconButton.setPadding(5)

        contentLayout.text = content
        contentLayout.textSize = 20F
        contentLayout.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        contentLayout.setPadding(6)
        contentLayout.setTextColor(resources.getColor(R.color.lightTextColor))

        layout.addView(iconButton)
        layout.addView(contentLayout)
        return layout
    }
}