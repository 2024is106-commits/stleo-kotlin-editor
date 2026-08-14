package com.steo.steotexteditor.data.db

@Entity(
    tableName = "versions",
    foreignKeys = [ForeignKey(
        entity = FileEntity::class,
        parentColumns = ["id"],
        childColumns = ["fileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val versionNumber: Int,
    val label: String,           // e.g. "v1", "after refactor"
    val patchText: String?,      // null only for version 1 (base)
    val createdAt: Long = System.currentTimeMillis()
)
