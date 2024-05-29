package com.company.chamberly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.company.chamberly.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SubscriptionBottomSheet: BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?  {
        val view = inflater.inflate(R.layout.subscription_bottom_sheet, container, false)
        val bottomSheet = view.findViewById<FrameLayout>(R.id.bottom_sheet_frame)
        val sheetBehavior = BottomSheetBehavior.from(bottomSheet)
        sheetBehavior.isDraggable = false
        sheetBehavior.skipCollapsed = true
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        return view
    }

    companion object {
        const val TAG = "SubscriptionBottomSheet"
    }
}