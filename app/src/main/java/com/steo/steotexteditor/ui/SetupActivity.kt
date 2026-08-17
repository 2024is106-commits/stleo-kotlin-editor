package com.steo.steotexteditor.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.steo.steotexteditor.R

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup)

        val etCoderName = findViewById<EditText>(R.id.etCoderName)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        btnContinue.setOnClickListener {
            val name = etCoderName.text.toString().trim()
            if (name.isNotEmpty()) {
                saveCoderName(name)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveCoderName(name: String) {
        val prefs = getSharedPreferences("steo_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("coder_name", name).apply()
    }
}
