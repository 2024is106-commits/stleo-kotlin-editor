package com.steo.steotexteditor.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "versions",
    foreignKeys = [ForeignKey(
        entity = FileEntity::class,
        parentColumns = ["id"],
        childColumns = ["fileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["fileId"])]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val versionNumber: Int,
    val label: String,
    val patchText: String?,   // v1 stores base content; later versions store unified diffs
    val createdAt: Long = System.currentTimeMillis()
)
