package pub.nya.saber.mediakey

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val tag = this::class.java.simpleName

    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        Button_openSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        Log.v(tag, "Prefs contains ${preferences.all}")
    }
}
