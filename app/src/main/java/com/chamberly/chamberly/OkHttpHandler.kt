package com.chamberly.chamberly

import android.content.Context
import android.util.Log
import com.chamberly.chamberly.utils.FCM_SERVER_URL
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


internal class OkHttpHandler(
    private val context: Context,
    private val token: String,
    private val notification: JSONObject,
    private val data: JSONObject
) {
    private val sharedPreferences =
        context.getSharedPreferences("cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient()
    private val requestJson = JSONObject()
    private val messageJson = JSONObject()
    private val SCOPES = arrayOf("https://www.googleapis.com/auth/firebase.messaging")

    private var accessToken: String = ""

    fun executeAsync() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                accessToken = sharedPreferences.getString("accessToken", "") ?: ""
                messageJson.put("token", token)
                messageJson.put("notification", notification)
                messageJson.put("data", data)
                messageJson.put("android", JSONObject(mapOf(
                    "priority" to "high"
                )))
                messageJson.put("apns", JSONObject(mapOf(
                    "payload" to mapOf(
                        "aps" to mapOf(
                            "category" to "NEW_MESSAGE_CATEGORY"
                        )
                    )
                )))
                requestJson.put("message", messageJson)
                val requestBody = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    requestJson.toString()
                )
                val request: Request = Request.Builder()
                    .url(FCM_SERVER_URL)
                    .addHeader("Authorization", "BEARER $accessToken")
                    .post(requestBody)
                    .build()
                val response: Response = client.newCall(request).execute()
                val status = response.code
                Log.d("STATUS", "$status tried sending notification")
                if (status % 100 != 2) {
                    //Request wasn't successful, try again with new access token
                    accessToken = getAccessToken()
                    val request: Request = Request.Builder()
                        .url(FCM_SERVER_URL)
                        .addHeader("Authorization", "Bearer $accessToken")
                        .post(requestBody)
                        .build()
                    val response: Response = client.newCall(request).execute()
                    val status = response.code
                    val responseBody = response.body?.string()
                    Log.d("STATUS", "$status $responseBody $accessToken tried sending notification")
                } else {
                    //Request was successful
                    return@launch
                }
                val responseBody = response.body?.string()

            } catch(e: Exception) {
                Log.e("SENDING NOTIFICATIONS", "Error: ${e.message}")
            }
        }
    }

    @Throws(IOException::class)
    private fun getAccessToken(): String {
        val googleCredentials: GoogleCredentials =
            GoogleCredentials
                .fromStream(context.resources.openRawResource(R.raw.service_account))
                .createScoped(SCOPES.toMutableList())
        googleCredentials.refresh()
        with(sharedPreferences.edit()) {
            putString("accessToken", googleCredentials.accessToken.tokenValue)
            apply()
        }
        return googleCredentials.getAccessToken().tokenValue
    }
}