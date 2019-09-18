package pub.nya.saber.mediakey

import android.annotation.SuppressLint
import android.content.*
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Vibrator
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity(), View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}

    override fun onStartTrackingTouch(p0: SeekBar?) {}

    override fun onStopTrackingTouch(p0: SeekBar?) {
        if (p0 == null) {
            Log.e(tag, "Seekbar is null?!")
            return
        }
        val progress = p0.progress
        Log.v(tag, "New progress: $progress")

        val intent = Intent(this, ConnectivityService::class.java)
        intent.putExtra(
            ConnectivityService.intentExtraMethod,
            ConnectivityService.Companion.Method.PerformAction.name
        )
        intent.putExtra(ConnectivityService.intentExtraAction, "setVol")
        intent.putExtra(ConnectivityService.intentExtraNewVol, progress)
        startService(intent)
    }

    private val tag = this::class.java.simpleName

    private lateinit var preferences: SharedPreferences

    private lateinit var vibrator: Vibrator

    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            Log.v(tag, "Received broadcast: $intent")
            if (intent == null) {
                Log.e(tag, "Intent is null, aborting")
                return
            }

            val exception = intent.getBooleanExtra("exception", true)
            val response = intent.getStringExtra("response")
            if (exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.MainActivity_requestFailure_exception) + response,
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            processResponse(response)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        Button_openSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

//        Log.v(tag, "Prefs contains ${preferences.all}")
        Button_hibernatePc.setOnClickListener(this)
        Button_volDown.setOnClickListener(this)
        Button_prev.setOnClickListener(this)
        Button_playPause.setOnClickListener(this)
        Button_next.setOnClickListener(this)
        Button_volUp.setOnClickListener(this)

        ProgressBar_currentVolume.setOnSeekBarChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                serviceBroadcastReceiver,
                IntentFilter(ConnectivityService.intentFilterTag)
            )
    }

    @SuppressLint("SetTextI18n")
    fun processResponse(response: String?) {
        if (response == null) {
            Log.e(tag, "Response is null")
            return
        }

        val json: JSONObject
        try {
            json = JSONObject(response)
        } catch (e: JSONException) {
            Log.e(tag, "Got invalid JSON", e)
            Toast.makeText(
                this,
                R.string.MainActivity_requestFailure_json,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!json.optBoolean("success", false)) {
            Log.e(tag, "Server failed: ${json.toString(2)}")
            val error = json.optString("error")
            val errorDesc = json.optString("errorDescription")
            Toast.makeText(
                this,
                "$error:\n$errorDesc",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val volume = json.optDouble("volume", -1.0)
        @Suppress("UNUSED_VARIABLE") val muted = json.optBoolean("muted", false) // TODO
        val preview: String? = json.optString("preview")

        val title = json.optString(
            "title",
            getString(R.string.MainActivity_layout_Text_currentSong_nothing)
        )
        if (title == "null") {
            TextView_currentSong.text =
                getString(R.string.MainActivity_layout_Text_currentSong_nothing)
            Button_playPause.setImageResource(R.drawable.ic_play_black_24dp)
        } else {
            TextView_currentSong.text = title
            Button_playPause.setImageResource(R.drawable.ic_pause_black_24dp)
        }
        TextView_currentVolume.text = "${volume.toInt()}%"
        ProgressBar_currentVolume.progress = volume.toInt()

        if (preview != null) {
            val decodedImage = Base64.decode(preview, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
            ImageView_albumCover.setImageBitmap(bitmap)
        }

        @Suppress("DEPRECATION")
        vibrator.vibrate(500)
    }

    override fun onClick(view: View?) {
        if (view == null) {
            Log.e(tag, "View is null")
            return
        }
        val id = view.id
        Log.v(tag, "Clicked view w/ id $id")
        val intent = Intent(this, ConnectivityService::class.java)
        intent.putExtra(
            ConnectivityService.intentExtraMethod,
            ConnectivityService.Companion.Method.PerformAction.name
        )
        val action: String?
        action = when (id) {
            R.id.Button_hibernatePc -> {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.MainActivity_hibernate_confirmation))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        intent.putExtra(ConnectivityService.intentExtraAction, "hibernate")
                        startService(intent)
                    }
                    .setNegativeButton(R.string.no) { _, _ -> }
                    .setCancelable(true)
                    .show()
                null
            }
            R.id.Button_volDown -> "volDown"
            R.id.Button_prev -> "prev"
            R.id.Button_playPause -> "pause"
            R.id.Button_next -> "next"
            R.id.Button_volUp -> "volUp"
            else -> {
                Log.e(tag, "Unknown id $id")
                return
            }
        }
        intent.putExtra(ConnectivityService.intentExtraAction, action)

        startService(intent)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceBroadcastReceiver)
    }
}
