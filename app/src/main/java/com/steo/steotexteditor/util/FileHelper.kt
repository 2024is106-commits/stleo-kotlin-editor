package com.steo.steotexteditor.util

import android.content.Context
import java.io.File
import java.io.IOException

object FileHelper {
    data class RecoveryDraft(
        val content: String,
        val fileName: String,
        val fileType: String,
        val fileId: Long,
        val path: String
    )

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

    fun saveCrashRecovery(context: Context, draft: RecoveryDraft) {
        try {
            getCrashRecoveryFile(context).writeText(draft.content)
            context.getSharedPreferences("steo_recovery", Context.MODE_PRIVATE)
                .edit()
                .putString("file_name", draft.fileName)
                .putString("file_type", draft.fileType)
                .putLong("file_id", draft.fileId)
                .putString("path", draft.path)
                .commit()
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

    fun readCrashRecoveryDraft(context: Context): RecoveryDraft? {
        val content = readCrashRecovery(context) ?: return null
        val prefs = context.getSharedPreferences("steo_recovery", Context.MODE_PRIVATE)
        val fileName = prefs.getString("file_name", null)
        val path = prefs.getString("path", "").orEmpty()
        val fileType = prefs.getString("file_type", null)
            ?: fileName?.substringAfterLast('.', "txt")
            ?: "txt"
        return RecoveryDraft(
            content = content,
            fileName = fileName ?: "Untitled.$fileType",
            fileType = fileType.lowercase(),
            fileId = prefs.getLong("file_id", 0L),
            path = path
        )
    }

    fun clearCrashRecovery(context: Context) {
        try {
            getCrashRecoveryFile(context).delete()
            context.getSharedPreferences("steo_recovery", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        } catch (e: Exception) {}
    }

    fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    fun deleteFile(path: String): Boolean {
        return File(path).delete()
    }
}
