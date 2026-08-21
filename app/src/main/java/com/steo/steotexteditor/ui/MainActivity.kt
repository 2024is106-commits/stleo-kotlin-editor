package com.steo.steotexteditor.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.steo.steotexteditor.R

class MainActivity : AppCompatActivity() {
    private val viewModel: EditorViewModel by viewModels()
    private var syncingBottomNav = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        setupBottomNavigation(bottomNav, navController)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        findViewById<ImageButton>(R.id.btnTopDrawer)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.btnProfile)?.setOnClickListener {
            showCoderNameDialog()
        }

        updateUserInfo()
        setupObservers()

        val profileBar = findViewById<View>(R.id.profileBar)
        ViewCompat.setOnApplyWindowInsetsListener(profileBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingStart, systemBars.top, v.paddingEnd, v.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }
    }

    private fun setupBottomNavigation(bottomNav: BottomNavigationView, navController: NavController) {
        bottomNav.setOnItemSelectedListener { item ->
            if (syncingBottomNav) {
                return@setOnItemSelectedListener true
            }
            navigateToTab(navController, item.itemId)
        }

        bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_run) {
                navigateToTab(navController, item.itemId)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menuItemId = when (destination.id) {
                R.id.nav_home,
                R.id.nav_edit,
                R.id.nav_run,
                R.id.nav_logs,
                R.id.nav_diff -> destination.id
                else -> null
            }

            if (menuItemId != null && bottomNav.selectedItemId != menuItemId) {
                syncingBottomNav = true
                bottomNav.selectedItemId = menuItemId
                syncingBottomNav = false
            }
        }
    }

    private fun setupObservers() {
        viewModel.currentFile.observe(this) { file ->
            val tvReadOnly = findViewById<TextView>(R.id.tvReadOnly)
            tvReadOnly?.visibility = if (file?.isReadOnly == true) View.VISIBLE else View.GONE
        }
    }

    private fun updateUserInfo() {
        val prefs = getSharedPreferences("steo_prefs", Context.MODE_PRIVATE)
        val coderName = prefs.getString("coder_name", "LEON")
        findViewById<TextView>(R.id.tvProfileName)?.text = coderName?.uppercase()
    }

    private fun showCoderNameDialog() {
        val prefs = getSharedPreferences("steo_prefs", Context.MODE_PRIVATE)
        val currentName = prefs.getString("coder_name", "LEON").orEmpty().take(5)

        val density = resources.displayMetrics.density
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (18 * density).toInt(), (24 * density).toInt(), (10 * density).toInt())
        }

        val label = TextView(this).apply {
            text = "Your coder-name:"
            setTextColor(android.graphics.Color.BLACK)
            typeface = ResourcesCompat.getFont(this@MainActivity, R.font.silkscreen)
            textSize = 14f
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (12 * density).toInt(), 0, 0)
        }

        val input = EditText(this).apply {
            setText(currentName)
            filters = arrayOf(CoderNameFilter(), InputFilter.LengthFilter(5))
            isSingleLine = true
            selectAll()
            typeface = ResourcesCompat.getFont(this@MainActivity, R.font.silkscreen)
            setTextColor(android.graphics.Color.BLACK)
            setHintTextColor(android.graphics.Color.GRAY)
            hint = "LEON"
        }

        val save = TextView(this).apply {
            text = "save"
            gravity = android.view.Gravity.CENTER
            typeface = ResourcesCompat.getFont(this@MainActivity, R.font.silkscreen)
            setTextColor(android.graphics.Color.parseColor("#E8E8F0"))
            setBackgroundResource(R.drawable.pixel_button_background)
            setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
        }

        row.addView(input, LinearLayout.LayoutParams(0, (48 * density).toInt(), 1f))
        row.addView(save, LinearLayout.LayoutParams((86 * density).toInt(), (48 * density).toInt()).apply {
            marginStart = (12 * density).toInt()
        })
        panel.addView(label)
        panel.addView(row)

        val dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()

        save.setOnClickListener {
            val nextName = input.text.toString().trim().take(5).ifBlank { "LEON" }
            prefs.edit().putString("coder_name", nextName).apply()
            updateUserInfo()
            refreshHomeHeadline()
            Toast.makeText(this, "Coder-name saved", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            input.requestFocus()
            input.post {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        dialog.show()
    }

    private fun refreshHomeHeadline() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment
        val currentFragment = navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()
        if (currentFragment is HomeFragment) {
            currentFragment.refreshCoderName()
        }
    }

    private class CoderNameFilter : InputFilter {
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val filtered = source.subSequence(start, end).filter { it.isLetter() }
            return if (filtered.length == end - start) null else filtered
        }
    }

    private fun navigateToTab(navController: NavController, destinationId: Int): Boolean {
        return try {
            if (navController.currentDestination?.id != destinationId) {
                val args = if (destinationId == R.id.nav_edit) {
                    Bundle().apply {
                        val currentFileId = viewModel.sessionState.value?.currentFileId ?: 0L
                        if (currentFileId > 0L) {
                            putLong("file_id", currentFileId)
                        }
                    }
                } else {
                    null
                }
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(navController.graph.startDestinationId, false)
                    .build()
                navController.navigate(destinationId, args, navOptions)
            }
            true
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }
}
