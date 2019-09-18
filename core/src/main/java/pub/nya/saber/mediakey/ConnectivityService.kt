package pub.nya.saber.mediakey

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ConnectivityService : Service() {

    private val tag = this::class.java.simpleName

    private lateinit var preferences: SharedPreferences
    private lateinit var client: OkHttpClient

    override fun onBind(intent: Intent): IBinder? {
        Log.e(tag, "Binding on this service is not implemented!")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.d(tag, "Intent is null, skipping...")
            return super.onStartCommand(intent, flags, startId)
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        client = OkHttpClient()
        val token = preferences.getString(
            getString(R.string.prefKey_accessToken),
            getString(R.string.prefDef_accessToken)
        )

        if (token.isNullOrBlank()) {
            Log.w(tag, "Token is empty, aborting")
            return super.onStartCommand(intent, flags, startId)
        }

        val requestBuilder = Request.Builder()
            .url(
                preferences.getString(
                    getString(R.string.prefKey_serverUrl),
                    getString(R.string.prefDef_serverUrl)
                )!! + "/mediaApi/"
            )

        val jsonObject: JSONObject
        when (intent.getStringExtra(intentExtraMethod)) {
            Method.PerformAction.name -> {
                jsonObject = JSONObject()
                jsonObject.put("token", token)
                jsonObject.put("method", "performAction")
                val action = intent.getStringExtra(intentExtraAction)
                if (action == null) {
                    Log.d(tag, "Action is null")
                    return super.onStartCommand(intent, flags, startId)
                }
                jsonObject.put("action", action)

                if (action == "setVol") {
                    jsonObject.put("newVol", intent.getIntExtra(intentExtraNewVol, 10))
                }
            }
            Method.GetInfo.name -> {
                jsonObject = JSONObject()
                jsonObject.put("token", token)
                jsonObject.put("method", "getInfo")
            }
            else -> {
                Log.e(tag, "Unknown action ${intent.getStringExtra(intentExtraMethod)}!")
                return super.onStartCommand(intent, flags, startId)
            }
        }

        val request = requestBuilder
            .post(jsonObject.toString().toRequestBody(jsonContentType))
            .build()

        client.newCall(request).enqueue(NetworkCallback())

        return super.onStartCommand(intent, flags, startId)
    }

    inner class NetworkCallback : Callback {
        private val tag = this::class.java.simpleName

        override fun onFailure(call: Call, e: IOException) {
            Log.w(tag, "Call failed w/ exception:", e)
            sendToActivity(e.localizedMessage, true)
        }

        override fun onResponse(call: Call, response: Response) {
            sendToActivity(response.body!!.string(), false)
        }

        private fun sendToActivity(response: String?, exception: Boolean) {
            val callbackIntent = Intent(intentFilterTag)
            callbackIntent.putExtra("response", response)
            callbackIntent.putExtra("exception", exception)
            LocalBroadcastManager.getInstance(this@ConnectivityService)
                .sendBroadcast(callbackIntent)
        }

    }

    companion object {
        val jsonContentType = "application/json; charset=utf-8".toMediaType()
        const val intentFilterTag = "MediaKeyConnectivityService"

        const val intentExtraMethod = "MediaKeyExtraMethod"
        const val intentExtraAction = "MediaKeyExtraAction"
        const val intentExtraNewVol = "MediaKeyExtraNewVol"

        enum class Method {
            PerformAction,
            GetInfo
        }
    }
}
