package com.chamberly.chamberly.presentation.states

data class AppState(
    val isAppEnabled: Boolean,
    val isAppUpdated: Boolean,
    val areExperimentalFeaturesEnabled: Boolean,
)
