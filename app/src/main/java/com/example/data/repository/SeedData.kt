package com.example.data.repository

import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity

object SeedData {
    // Demo content has been completely purged per user requirement
    val defaultChannels: List<ChannelEntity> = emptyList()
    val defaultFiles: List<FileEntity> = emptyList()

    // Known legacy demo IDs for database cleanup
    val legacyDemoChannelIds = setOf(
        "chan_exec_01",
        "chan_mobile_02",
        "chan_design_03",
        "chan_fin_04",
        "chan_eng_05",
        "chan_media_06"
    )

    fun isDemoChannel(channelId: String): Boolean {
        return legacyDemoChannelIds.contains(channelId)
    }

    fun isDemoFile(fileId: String): Boolean {
        return fileId.startsWith("file_exec_") ||
               fileId.startsWith("file_mobile_") ||
               fileId.startsWith("file_design_") ||
               fileId.startsWith("file_fin_") ||
               fileId.startsWith("file_eng_") ||
               fileId.startsWith("file_media_")
    }
}
