package com.steo.steotexteditor.data.db

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,           // absolute path on device storage
    val isReadOnly: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
