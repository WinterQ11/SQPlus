package com.example.ui.viewmodel

import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity

enum class Screen {
    HOME,
    CHANNELS,
    SEARCH,
    PROFILE,
    CHANNEL_DETAIL,
    ADMIN_DASHBOARD
}

enum class SortOption(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_AZ("Name A-Z"),
    LARGEST("Largest Size"),
    SMALLEST("Smallest Size")
}

enum class FileTypeFilter(val label: String, val extensions: List<String>) {
    ALL("All", emptyList()),
    PDF("PDF", listOf("PDF")),
    APK("APK", listOf("APK")),
    ZIP("Archives", listOf("ZIP", "RAR", "TAR", "GZ")),
    DOC("Documents", listOf("DOCX", "PPTX", "TXT")),
    SHEETS("Sheets", listOf("XLSX", "CSV")),
    MEDIA("Media", listOf("MP4", "MOV", "PNG", "JPG"))
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

data class DownloadProgressState(
    val fileId: String,
    val progress: Float,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

data class DashboardStats(
    val totalChannels: Int = 0,
    val totalFiles: Int = 0,
    val totalDownloads: Int = 0,
    val totalStorageBytes: Long = 0L
)
