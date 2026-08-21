package com.steo.steotexteditor.data.repository

import com.steo.steotexteditor.data.db.AppDatabase
import com.steo.steotexteditor.data.db.FileEntity
import com.steo.steotexteditor.data.db.VersionEntity
import com.steo.steotexteditor.util.FileHelper
import android.content.Context
import androidx.lifecycle.LiveData

class FileRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val fileDao = db.fileDao()
    private val versionDao = db.versionDao()



    suspend fun getAllFiles(): List<FileEntity> = fileDao.getAllFiles()

    fun getAllFilesLive(): LiveData<List<FileEntity>> = fileDao.getAllFilesLive()

    suspend fun getFileById(id: Long): FileEntity? = fileDao.getFileById(id)

    suspend fun getFileByPath(path: String): FileEntity? = fileDao.getFileByPath(path)

    suspend fun setReadOnly(fileId: Long, readOnly: Boolean) {
        fileDao.setReadOnly(fileId, readOnly)
    }

    suspend fun deleteFile(file: FileEntity) {
        fileDao.deleteFile(file)
    }

    // ── Save & version creation ───────────────────────────────────

    /**
     * Saves file content and appends a new immutable version snapshot.
     */
    suspend fun saveFileWithVersion(
        fileEntity: FileEntity,
        content: String,
        label: String
    ): Long {
        val savedAt = System.currentTimeMillis()
        val savedFile = fileEntity.copy(lastModified = savedAt)
        val fileId = if (savedFile.id == 0L) {
            fileDao.insertFile(savedFile)
        } else {
            fileDao.updateFile(savedFile)
            savedFile.id
        }

        val latestVersion = versionDao.getLatestVersion(fileId)
        if (latestVersion?.patchText == content) {
            FileHelper.writeFile(fileEntity.path, content)
            return fileId
        }

        val versionNumber = (latestVersion?.versionNumber ?: 0) + 1
        versionDao.insertVersion(
            VersionEntity(
                fileId = fileId,
                versionNumber = versionNumber,
                label = label,
                patchText = content,
                createdAt = savedAt
            )
        )

        FileHelper.writeFile(fileEntity.path, content)

        return fileId
    }

    // ── Version retrieval ─────────────────────────────────────────

    suspend fun getVersionsForFile(fileId: Long): List<VersionEntity> {
        return versionDao.getVersionsForFile(fileId)
    }

    /**
     * Returns the immutable file content saved for a version.
     */
    suspend fun reconstructVersion(fileId: Long, targetVersionNumber: Int): String? {
        return versionDao.getVersionsForFile(fileId)
            .firstOrNull { it.versionNumber == targetVersionNumber }
            ?.patchText
    }

    /**
     * Restores file content to a specific version and writes it to disk.
     */
    suspend fun restoreVersion(fileId: Long, versionNumber: Int): Boolean {
        val content = reconstructVersion(fileId, versionNumber) ?: return false
        val file = fileDao.getFileById(fileId) ?: return false
        return FileHelper.writeFile(file.path, content)
    }

    /**
     * Returns the unified diff string between two versions for display.
     */
    suspend fun getDiffBetweenVersions(
        fileId: Long,
        fromVersion: Int,
        toVersion: Int
    ): List<String> {
        val from = reconstructVersion(fileId, fromVersion) ?: return emptyList()
        val to = reconstructVersion(fileId, toVersion) ?: return emptyList()

        return buildUnifiedDiffLines(fromVersion, toVersion, from.lines(), to.lines())
    }

    private fun buildUnifiedDiffLines(
        fromVersion: Int,
        toVersion: Int,
        fromLines: List<String>,
        toLines: List<String>
    ): List<String> {
        val rows = mutableListOf("v$fromVersion -> v$toVersion")
        val max = maxOf(fromLines.size, toLines.size)
        for (index in 0 until max) {
            val oldLine = fromLines.getOrNull(index)
            val newLine = toLines.getOrNull(index)
            when {
                oldLine == newLine -> rows.add(" ${index + 1}: ${oldLine.orEmpty()}")
                oldLine == null -> rows.add("+${index + 1}: ${newLine.orEmpty()}")
                newLine == null -> rows.add("-${index + 1}: ${oldLine}")
                else -> {
                    rows.add("-${index + 1}: ${oldLine}")
                    rows.add("+${index + 1}: ${newLine}")
                }
            }
        }
        return rows
    }
}
