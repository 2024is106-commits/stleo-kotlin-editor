// util/UserPreferences.kt
package com.steo.steotexteditor.util

import android.content.Context

object UserPreferences {

    private const val PREFS_NAME = "steocode_prefs"
    private const val KEY_CODER_NAME = "coder_name"

    fun getCoderName(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CODER_NAME, null)
    }

    fun saveCoderName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CODER_NAME, name)
            .apply()
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getCoderName(context) == null
    }
}