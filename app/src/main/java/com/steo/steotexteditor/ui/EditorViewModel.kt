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

data class EditorSessionState(
    val currentFileName: String = "Untitled.txt",
    val currentFileType: String = "txt",
    val currentFileContent: String = "",
    val isSessionAlive: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
    val currentFileId: Long = 0L,
    val currentFilePath: String = ""
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val fileRepository = FileRepository(application)
    private val context = application

    val editorContent = MutableLiveData<String>()
    private val _sessionState = MutableLiveData(EditorSessionState())
    val sessionState: LiveData<EditorSessionState> = _sessionState
    private var shouldOfferRecovery = FileHelper.readCrashRecoveryDraft(application) != null

    init {
        startAutoSave()
    }

    private fun startAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(10000L)
                val content = editorContent.value
                val state = _sessionState.value
                if (content != null && state?.hasUnsavedChanges == true) {
                    withContext(Dispatchers.IO) {
                        FileHelper.saveCrashRecovery(
                            context,
                            FileHelper.RecoveryDraft(
                                content = state.currentFileContent,
                                fileName = state.currentFileName,
                                fileType = state.currentFileType,
                                fileId = state.currentFileId,
                                path = state.currentFilePath
                            )
                        )
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
        file?.let {
            updateSessionFile(
                fileName = it.name,
                fileType = it.fileType.ifBlank { it.name.substringAfterLast('.', "txt") },
                fileId = it.id,
                path = it.path
            )
        }
    }

    fun updateEditorSession(
        file: FileEntity?,
        content: String,
        hasUnsavedChanges: Boolean
    ) {
        val fileName = file?.name ?: _sessionState.value?.currentFileName ?: "Untitled.txt"
        val fileType = file?.fileType?.ifBlank { fileName.substringAfterLast('.', "txt") }
            ?: fileName.substringAfterLast('.', _sessionState.value?.currentFileType ?: "txt")
        _sessionState.value = EditorSessionState(
            currentFileName = fileName,
            currentFileType = fileType.lowercase(),
            currentFileContent = content,
            isSessionAlive = true,
            hasUnsavedChanges = hasUnsavedChanges,
            currentFileId = file?.id ?: _sessionState.value?.currentFileId ?: 0L,
            currentFilePath = file?.path ?: _sessionState.value?.currentFilePath.orEmpty()
        )
        editorContent.value = content
    }

    fun updateSessionFile(fileName: String, fileType: String, fileId: Long, path: String) {
        val current = _sessionState.value ?: EditorSessionState()
        _sessionState.value = current.copy(
            currentFileName = fileName,
            currentFileType = fileType.lowercase(),
            currentFileId = fileId,
            currentFilePath = path,
            isSessionAlive = true
        )
    }

    fun consumeRecoveryDraftIfNeeded(): FileHelper.RecoveryDraft? {
        if (!shouldOfferRecovery) return null
        shouldOfferRecovery = false
        return FileHelper.readCrashRecoveryDraft(context)
    }

    fun discardRecoveryDraft() {
        FileHelper.clearCrashRecovery(context)
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
            val savedFile = fileRepository.getFileById(fileId) ?: file.copy(id = fileId)
            _currentFile.value = savedFile
            updateEditorSession(savedFile, content, hasUnsavedChanges = false)
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
            val savedFile = fileRepository.getFileById(fileId) ?: newFile.copy(id = fileId)
            _currentFile.value = savedFile
            updateEditorSession(savedFile, content, hasUnsavedChanges = false)
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
                _currentFile.value = file
                updateEditorSession(file, content.orEmpty(), hasUnsavedChanges = false)
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
