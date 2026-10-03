package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_records")
data class DownloadRecordEntity(
    @PrimaryKey val id: String,
    val fileId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)
