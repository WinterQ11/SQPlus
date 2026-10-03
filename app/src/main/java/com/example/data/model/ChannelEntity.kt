package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val iconName: String,
    val fileCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFeatured: Boolean = false
)
