package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [Index(value = ["channelId"])]
)
data class FileEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val name: String, // title
    val description: String,
    val fileType: String, // PDF, ZIP, APK, MP4, DOCX, PPTX, XLSX, TXT, etc.
    val fileSizeBytes: Long,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val downloadCount: Int = 0,
    val downloadUrl: String = "",
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val thumbnailUrl: String? = null,
    val category: String = "General",
    val isPublished: Boolean = true
)
