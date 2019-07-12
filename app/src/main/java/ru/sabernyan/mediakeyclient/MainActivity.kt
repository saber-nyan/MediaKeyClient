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

import android.animation.LayoutTransition
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {
    private val tag = this::class.java.simpleName

    private var prefs: SharedPreferences? = null

    companion object {
        const val prefKeyServerAddress = "serverAddress"
        const val prefKeyToken = "token"
        const val prefsName = "MediaKeyClient.nyan"

        const val ACTION_PREV = "mediakey_prev"
        const val ACTION_PAUSE = "mediakey_pause"
        const val ACTION_NEXT = "mediakey_next"
        const val ACTION_VOLDOWN = "mediakey_voldown"
        const val ACTION_MUTE = "mediakey_mute"
        const val ACTION_VOLUP = "mediakey_volup"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

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

        button_prev.setOnLongClickListener(this)
        button_playPause.setOnLongClickListener(this)
        button_next.setOnLongClickListener(this)
        button_volDown.setOnLongClickListener(this)
        button_mute.setOnLongClickListener(this)
        button_volUp.setOnLongClickListener(this)
    }

    override fun onClick(p0: View?) {
        startService(
            Intent(this, ConnectivityService::class.java).setAction(
                when (p0?.id) {
                    R.id.button_prev -> ACTION_PREV
                    R.id.button_playPause -> ACTION_PAUSE
                    R.id.button_next -> ACTION_NEXT
                    R.id.button_volDown -> ACTION_VOLDOWN
                    R.id.button_mute -> ACTION_MUTE
                    R.id.button_volUp -> ACTION_VOLUP
                    else -> return
                }
            )
        )
    }

    @TargetApi(Build.VERSION_CODES.O)
    override fun onLongClick(p0: View?): Boolean {
        val shortcutManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(ShortcutManager::class.java)
        } else {
            null
        }
        if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported || p0 == null) {
            Toast.makeText(
                this,
                "Shortcuts is not supported...",
                Toast.LENGTH_SHORT
            ).show()
            return true
        }

        val shortcut = ShortcutInfo.Builder(this, p0.id.toString())
            .setIntent(
                Intent(applicationContext, ConnectivityService::class.java).setAction(
                    when (p0.id) {
                        R.id.button_prev -> ACTION_PREV
                        R.id.button_playPause -> ACTION_PAUSE
                        R.id.button_next -> ACTION_NEXT
                        R.id.button_volDown -> ACTION_VOLDOWN
                        R.id.button_mute -> ACTION_MUTE
                        R.id.button_volUp -> ACTION_VOLUP
                        else -> return true
                    }
                )
            )
            .setShortLabel(
                when (p0.id) {
                    R.id.button_prev -> ACTION_PREV
                    R.id.button_playPause -> ACTION_PAUSE
                    R.id.button_next -> ACTION_NEXT
                    R.id.button_volDown -> ACTION_VOLDOWN
                    R.id.button_mute -> ACTION_MUTE
                    R.id.button_volUp -> ACTION_VOLUP
                    else -> return true
                }
            ) // TODO
            .build()

        shortcutManager.requestPinShortcut(shortcut, null)
        return true
    }
}
