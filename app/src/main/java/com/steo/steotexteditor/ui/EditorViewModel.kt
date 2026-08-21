package com.steo.steotexteditor.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.steo.steotexteditor.data.db.FileEntity
import com.steo.steotexteditor.data.db.VersionEntity
import com.steo.steotexteditor.data.repository.FileRepository
import com.steo.steotexteditor.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class ActivityLogEntry(
    val fileId: Long,
    val fileName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val fileRepository = FileRepository(application)
    private val context = application

    val editorContent = MutableLiveData<String>()

    init {
        startAutoSave()
    }

    private fun startAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(10000L)
                val content = editorContent.value
                if (content != null) {
                    withContext(Dispatchers.IO) {
                        FileHelper.saveCrashRecovery(context, content)
                    }
                }
            }
        }
    }

    // LiveData list of recent files observed from Room
    val recentFiles: LiveData<List<FileEntity>> = fileRepository.getAllFilesLive()

    private val _currentFile = MutableLiveData<FileEntity?>()
    val currentFile: LiveData<FileEntity?> = _currentFile

    private val _activityLogs = MutableLiveData<List<ActivityLogEntry>>(emptyList())
    val activityLogs: LiveData<List<ActivityLogEntry>> = _activityLogs

    fun setCurrentFile(file: FileEntity?) {
        _currentFile.value = file
    }

    fun recordActivity(file: FileEntity, message: String) {
        val entry = ActivityLogEntry(
            fileId = file.id,
            fileName = file.name,
            message = message
        )
        _activityLogs.value = (_activityLogs.value.orEmpty() + entry)
    }
    
    suspend fun getVersionsForFile(fileId: Long): List<VersionEntity> {
        return fileRepository.getVersionsForFile(fileId)
    }

    fun restoreVersion(fileId: Long, versionNumber: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = fileRepository.restoreVersion(fileId, versionNumber)
            onComplete(result)
        }
    }

    suspend fun getDiffBetweenVersions(fileId: Long, fromVersion: Int, toVersion: Int): List<String> {
        return fileRepository.getDiffBetweenVersions(fileId, fromVersion, toVersion)
    }

    suspend fun reconstructVersion(fileId: Long, versionNumber: Int): String? {
        return fileRepository.reconstructVersion(fileId, versionNumber)
    }

    fun loadFile(fileId: Long, onFileLoaded: (FileEntity?, String?) -> Unit) {
        viewModelScope.launch {
            val file = fileRepository.getFileById(fileId)
            if (file != null) {
                val content = FileHelper.readFile(file.path)
                onFileLoaded(file, content)
            } else {
                onFileLoaded(null, null)
            }
        }
    }
    
    fun saveFile(file: FileEntity, content: String, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val fileId = fileRepository.saveFileWithVersion(file, content, "Save")
            onSaved(fileId)
        }
    }
    
    fun createNewFile(name: String, content: String, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val path = FileHelper.getStorageDir(context).resolve(name).absolutePath
            val newFile = FileEntity(
                id = 0,
                name = name,
                path = path,
                fileType = name.substringAfterLast('.', "txt").lowercase(),
                lastModified = System.currentTimeMillis(),
                isReadOnly = false
            )
            // Write content to disk first
            FileHelper.writeFile(path, content)
            // Then save to database with version
            val fileId = fileRepository.saveFileWithVersion(newFile, content, "Initial save")
            onSaved(fileId)
        }
    }
    
    fun openFile(uri: Uri, onFileOpened: (FileEntity?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                val path = uri.path ?: ""
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Untitled"
                
                // Check if file already exists in DB
                var file = fileRepository.getFileByPath(path)
                if (file == null) {
                    file = FileEntity(
                        id = 0,
                        name = fileName,
                        path = path,
                        fileType = fileName.substringAfterLast('.', "txt").lowercase(),
                        lastModified = System.currentTimeMillis(),
                        isReadOnly = false
                    )
                    val fileId = fileRepository.saveFileWithVersion(file, content ?: "", "Initial import")
                    file = fileRepository.getFileById(fileId)
                }
                onFileOpened(file, content)
            } catch (e: Exception) {
                onFileOpened(null, null)
            }
        }
    }
    
    fun deleteFile(file: FileEntity, onDelete: () -> Unit) {
        viewModelScope.launch {
            fileRepository.deleteFile(file)
            FileHelper.deleteFile(file.path)
            onDelete()
        }
    }
}
