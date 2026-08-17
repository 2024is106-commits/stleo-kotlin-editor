package com.steo.steotexteditor.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.steo.steotexteditor.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Simple timed splash then open Setup or Main
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("steo_prefs", MODE_PRIVATE)
            val coderName = prefs.getString("coder_name", null)
            
            if (coderName == null) {
                startActivity(Intent(this, SetupActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 800)
    }
}
