package com.steo.steotexteditor.data.db

import androidx.room.*

@Dao
interface VersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: VersionEntity): Long

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber ASC")
    suspend fun getVersionsForFile(fileId: Long): List<VersionEntity>

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestVersion(fileId: Long): VersionEntity?

    @Query("SELECT COUNT(*) FROM versions WHERE fileId = :fileId")
    suspend fun getVersionCount(fileId: Long): Int

    @Query("DELETE FROM versions WHERE fileId = :fileId")
    suspend fun deleteAllVersionsForFile(fileId: Long)

    @Query("SELECT * FROM versions WHERE id = :versionId")
    suspend fun getVersionById(versionId: Long): VersionEntity?
}