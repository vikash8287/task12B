package com.company.chamberly

import android.os.AsyncTask
import android.util.Log
import com.company.chamberly.utils.FCM_SERVER_URL
import com.company.chamberly.utils.SERVER_KEY
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject


internal class OkHttpHandler(private val payload: JSONObject, private val token: String) : AsyncTask<Void?, Void?, String?>() {

    private val TAG = "SENDING_NOTIFICATIONS"

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        if (result != null) {
            // Handle the response here
        } else {
            Log.e(TAG, "Error: No response received")
        }
    }

    override fun doInBackground(vararg p0: Void?): String? {
        val client = OkHttpClient()
        val requestJson = JSONObject()
        requestJson.put("to", token)
        requestJson.put("priority", "high")
        requestJson.put("data", payload)
        requestJson.put("notification", payload)
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), requestJson.toString())
//        val fm = Firebase
        val request: Request = Request.Builder()
            .url(FCM_SERVER_URL)
            .addHeader("Authorization", "BEARER $SERVER_KEY")
            .post(requestBody)
            .build()
        return try {
            val call: Call = client.newCall(request)
            val response: Response = call.execute()
            response.body?.string()
        } catch (e: Exception) {
            Log.e(TAG, "Error: " + e.message)
            null
        }
    }
}