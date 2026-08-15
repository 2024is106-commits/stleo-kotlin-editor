package com.steo.steotexteditor.data.repository

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.UnifiedDiffUtils
import com.steo.steotexteditor.data.db.AppDatabase
import com.steo.steotexteditor.data.db.FileEntity
import com.steo.steotexteditor.data.db.VersionEntity
import com.steo.steotexteditor.util.FileHelper
import android.content.Context

class FileRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val fileDao = db.fileDao()
    private val versionDao = db.versionDao()

    // ── File operations ──────────────────────────────────────────

    suspend fun getAllFiles(): List<FileEntity> = fileDao.getAllFiles()

    suspend fun getFileById(id: Long): FileEntity? = fileDao.getFileById(id)

    suspend fun setReadOnly(fileId: Long, readOnly: Boolean) {
        fileDao.setReadOnly(fileId, readOnly)
    }

    suspend fun deleteFile(file: FileEntity) {
        fileDao.deleteFile(file)
    }

    // ── Save & version creation ───────────────────────────────────

    /**
     * Saves file content and creates a new version.
     * v1 stores the full text. Every subsequent version stores a diff patch.
     */
    suspend fun saveFileWithVersion(
        fileEntity: FileEntity,
        content: String,
        label: String
    ): Long {
        // Upsert the file record
        val fileId = fileDao.insertFile(fileEntity.copy(
            lastModified = System.currentTimeMillis()
        ))

        // Write content to disk
        FileHelper.writeFile(fileEntity.path, content)

        val versionCount = versionDao.getVersionCount(fileId)

        if (versionCount == 0) {
            // First save — store full content as patch (null = base)
            versionDao.insertVersion(
                VersionEntity(
                    fileId = fileId,
                    versionNumber = 1,
                    label = label,
                    patchText = null  // base version, read from disk
                )
            )
        } else {
            // Subsequent saves — compute delta from previous version
            val previousContent = reconstructVersion(fileId, versionCount)
                ?: return fileId

            val previousLines = previousContent.lines()
            val currentLines = content.lines()

            val patch = DiffUtils.diff(previousLines, currentLines)
            val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                "previous", "current", previousLines, patch, 3
            )
            val patchText = unifiedDiff.joinToString("\n")

            versionDao.insertVersion(
                VersionEntity(
                    fileId = fileId,
                    versionNumber = versionCount + 1,
                    label = label,
                    patchText = patchText
                )
            )
        }

        return fileId
    }

    // ── Version retrieval ─────────────────────────────────────────

    suspend fun getVersionsForFile(fileId: Long): List<VersionEntity> {
        return versionDao.getVersionsForFile(fileId)
    }

    /**
     * Reconstructs file content at a given version number
     * by applying patches from v1 up to the target version.
     */
    suspend fun reconstructVersion(fileId: Long, targetVersionNumber: Int): String? {
        val versions = versionDao.getVersionsForFile(fileId)
        if (versions.isEmpty()) return null

        val file = fileDao.getFileById(fileId) ?: return null

        // v1 is always the raw file on disk
        var currentContent = FileHelper.readFile(file.path) ?: return null

        for (version in versions) {
            if (version.versionNumber == 1) continue
            if (version.versionNumber > targetVersionNumber) break

            val patchText = version.patchText ?: continue
            val patchLines = patchText.lines()
            val originalLines = currentContent.lines()

            val patch: Patch<String> = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
            currentContent = DiffUtils.patch(originalLines, patch).joinToString("\n")
        }

        return currentContent
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

        val patch = DiffUtils.diff(from.lines(), to.lines())
        return UnifiedDiffUtils.generateUnifiedDiff(
            "v$fromVersion", "v$toVersion", from.lines(), patch, 3
        )
    }
}
