package com.example.pomo

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.example.pomo.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var quoteSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        quoteSwitch = findViewById(R.id.quoteSwitch)

        quoteSwitch.isChecked = prefs.getBoolean("show_quote", true)

        quoteSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_quote", isChecked).apply()
        }
    }
}
