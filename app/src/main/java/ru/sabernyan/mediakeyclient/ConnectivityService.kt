/******************************************************************************
 * Copyright 2019 saber-nyan                                                  *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package ru.sabernyan.mediakeyclient

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.util.Base64
import android.util.Log
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ru.sabernyan.mediakeyclient.MainActivity.Companion.ACTION_MUTE
import ru.sabernyan.mediakeyclient.MainActivity.Companion.ACTION_NEXT
import ru.sabernyan.mediakeyclient.MainActivity.Companion.ACTION_PAUSE
import ru.sabernyan.mediakeyclient.MainActivity.Companion.ACTION_PREV
import ru.sabernyan.mediakeyclient.MainActivity.Companion.ACTION_VOLDOWN
import ru.sabernyan.mediakeyclient.MainActivity.Companion.ACTION_VOLUP
import java.io.IOException

class ConnectivityService : Service() {
    private val tag = this::class.java.simpleName

    private var prefs: SharedPreferences? = null

    private var handler: Handler? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) return START_NOT_STICKY
        prefs = getSharedPreferences(MainActivity.prefsName, Context.MODE_PRIVATE)
        handler = Handler()
        sendRequest(intent.action)
        return START_NOT_STICKY
    }

    private fun runOnUiThread(runnable: () -> Unit) {
        handler?.post(runnable)
    }

    private fun sendRequest(action: String?) {
        try {
            val keyByte = when (action) {
                ACTION_PREV -> 177
                ACTION_PAUSE -> 179
                ACTION_NEXT -> 176
                ACTION_VOLDOWN -> 174
                ACTION_MUTE -> 173
                ACTION_VOLUP -> 175
                else -> return
            }
            val keyBase64 = Base64.encodeToString(byteArrayOf(keyByte.toByte()), Base64.NO_WRAP)
            val jsonOut = JSONObject(
                mapOf(
                    "token" to prefs!!.getString(MainActivity.prefKeyToken, ""),
                    "key" to keyBase64
                )
            ).toString()
            Log.d(tag, "Sending key ${keyByte.toByte()}")

            val request = Request.Builder()
                .url("${prefs!!.getString(MainActivity.prefKeyServerAddress, "")}/pressKey/")
                .post(jsonOut.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(tag, "Failed with exception!", e)
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Failed with exception!\n${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseJson = JSONObject(response.body!!.string())
                    if (!responseJson.optBoolean("success")) {
                        Log.e(tag, "Server failed: ${responseJson.optString("errorDescription")}")
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Server failed: ${responseJson.optString("errorName")}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Unexpected exception:", e)
            Toast.makeText(
                this,
                "Unexpected exception!\n${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
