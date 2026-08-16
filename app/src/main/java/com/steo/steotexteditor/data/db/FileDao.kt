package com.steo.steotexteditor.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    suspend fun getAllFiles(): List<FileEntity>

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    fun getAllFilesLive(): LiveData<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE path = :path")
    suspend fun getFileByPath(path: String): FileEntity?

    @Query("UPDATE files SET isReadOnly = :readOnly WHERE id = :id")
    suspend fun setReadOnly(id: Long, readOnly: Boolean)
}