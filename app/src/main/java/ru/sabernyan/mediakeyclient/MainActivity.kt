package ru.sabernyan.mediakeyclient

import android.animation.LayoutTransition
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val tag = this::class.java.simpleName

    private var prefs: SharedPreferences? = null

    companion object {
        private const val prefKeyServerAddress = "serverAddress"
        private const val prefKeyToken = "token"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getPreferences(Context.MODE_PRIVATE)

        val transition = constraintLayout_root.layoutTransition
        transition.enableTransitionType(LayoutTransition.CHANGING)
        transition.setDuration(LayoutTransition.CHANGING, 150)

        textInputEditText_serverAddress.setText(prefs?.getString(prefKeyServerAddress, ""))
        textInputEditText_token.setText(prefs?.getString(prefKeyToken, ""))

        textInputEditText_serverAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                prefs?.edit()
                        ?.putString(prefKeyServerAddress, p0.toString())
                        ?.apply()
            }
        })
        textInputEditText_token.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                prefs?.edit()
                        ?.putString(prefKeyToken, p0.toString())
                        ?.apply()
            }
        })

        button_prev.setOnClickListener(this)
        button_playPause.setOnClickListener(this)
        button_next.setOnClickListener(this)
        button_volDown.setOnClickListener(this)
        button_mute.setOnClickListener(this)
        button_volUp.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        try {
            val keyByte = when (p0?.id) {
                R.id.button_prev -> 177
                R.id.button_playPause -> 179
                R.id.button_next -> 176
                R.id.button_volDown -> 174
                R.id.button_mute -> 173
                R.id.button_volUp -> 175
                else -> return
            }
            val keyBase64 = Base64.encodeToString(byteArrayOf(keyByte.toByte()), Base64.NO_WRAP)
            val jsonOut = JSONObject(mapOf(
                    "token" to textInputEditText_token.text.toString(),
                    "key" to keyBase64
            )).toString()
            Log.d(tag, "Sending key ${keyByte.toByte()}")

            val request = Request.Builder()
                    .url("${textInputEditText_serverAddress.text}/pressKey/")
                    .post(jsonOut.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .build()
            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(tag, "Failed with exception!", e)
                    this@MainActivity.runOnUiThread {
                        Toast.makeText(
                                this@MainActivity,
                                "Failed with exception!\n${e.localizedMessage}",
                                Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseJson = JSONObject(response.body!!.string())
                    if (!responseJson.optBoolean("success")) {
                        Log.e(tag, "Server failed: ${responseJson.optString("errorDescription")}")
                        this@MainActivity.runOnUiThread {
                            Toast.makeText(
                                    this@MainActivity,
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
                    this@MainActivity,
                    "Unexpected exception!\n${e.localizedMessage}",
                    Toast.LENGTH_LONG
            ).show()
        }
    }
}
