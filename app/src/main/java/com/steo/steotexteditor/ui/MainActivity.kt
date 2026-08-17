package com.steo.steotexteditor.ui

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.steo.steotexteditor.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setupWithNavController(navController)

        updateUserInfo()

        // Intercept Run button to trigger preview/save flow
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_run) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
                val currentFrag = navHostFragment.childFragmentManager.fragments.firstOrNull()
                if (currentFrag is EditorFragment) {
                    currentFrag.handleRunAction()
                    true
                } else {
                    // not in editor - navigate normally
                    navController.navigate(item.itemId)
                    true
                }
            } else {
                // default navigation for other items
                navController.navigate(item.itemId)
                true
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateUserInfo() {
        val prefs = getSharedPreferences("steo_prefs", Context.MODE_PRIVATE)
        val coderName = prefs.getString("coder_name", "LEON")
        findViewById<TextView>(R.id.tvProfileName)?.text = coderName?.uppercase()
    }
}
