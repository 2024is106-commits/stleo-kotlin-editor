package com.steo.steotexteditor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val fileType: String = "kt",       // "kt" or "md"
    val projectName: String? = null,   // null = standalone file
    val isReadOnly: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)