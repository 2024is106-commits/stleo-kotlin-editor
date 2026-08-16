package com.steo.steotexteditor.util

import android.content.Context
import java.io.File
import java.io.IOException

object FileHelper {

    fun writeFile(path: String, content: String): Boolean {
        return try {
            File(path).writeText(content, Charsets.UTF_8)
            true
        } catch (e: IOException) {
            false
        }
    }

    fun readFile(path: String): String? {
        return try {
            File(path).readText(Charsets.UTF_8)
        } catch (e: IOException) {
            null
        }
    }

    fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, "steocode_files")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCrashRecoveryFile(context: Context): File {
        return File(context.cacheDir, "crash_recovery.tmp")
    }

    fun saveCrashRecovery(context: Context, content: String) {
        try {
            getCrashRecoveryFile(context).writeText(content)
        } catch (e: Exception) {}
    }

    fun readCrashRecovery(context: Context): String? {
        val file = getCrashRecoveryFile(context)
        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun clearCrashRecovery(context: Context) {
        try {
            getCrashRecoveryFile(context).delete()
        } catch (e: Exception) {}
    }

    fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    fun deleteFile(path: String): Boolean {
        return File(path).delete()
    }
}