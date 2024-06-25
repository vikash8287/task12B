package com.chamberly.chamberly.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

fun logEvent(firebaseAnalytics: FirebaseAnalytics, eventName: String, params: HashMap<String, Any>) {
    val bundle = Bundle()
    params.forEach { (key, value) ->
        when(value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is Double -> bundle.putDouble(key, value)
            else -> {}
        }
    }
    bundle.putBoolean("android", true)
    bundle.putBoolean("selfIsSubbed", false)
    firebaseAnalytics.logEvent(eventName, bundle)
}