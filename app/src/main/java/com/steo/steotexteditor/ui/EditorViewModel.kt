package com.steo.steotexteditor.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.steo.steotexteditor.data.db.FileEntity
import com.steo.steotexteditor.data.repository.FileRepository
import com.steo.steotexteditor.util.FileHelper
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val fileRepository = FileRepository(application)
    private val context = application
    
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
            val fileId = fileRepository.saveFileWithVersion(file, content, "Manual save")
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
