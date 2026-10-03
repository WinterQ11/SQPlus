package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChannelEntity
import com.example.data.model.DownloadRecordEntity
import com.example.data.model.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SqPlusDao {

    // --- Channels ---
    @Query("SELECT * FROM channels ORDER BY isFeatured DESC, lastUpdated DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFeatured = 1 ORDER BY lastUpdated DESC")
    fun getFeaturedChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun getChannelById(channelId: String): Flow<ChannelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannelById(channelId: String)

    @Query("DELETE FROM channels WHERE id IN (:channelIds)")
    suspend fun deleteChannelsByIds(channelIds: List<String>)

    @Query("DELETE FROM files WHERE channelId = :channelId")
    suspend fun deleteFilesByChannelId(channelId: String)

    @Query("DELETE FROM files WHERE channelId IN (:channelIds)")
    suspend fun deleteFilesByChannelIds(channelIds: List<String>)

    @Query("SELECT * FROM files WHERE channelId = :channelId")
    suspend fun getFileListByChannel(channelId: String): List<FileEntity>

    @Query("UPDATE channels SET fileCount = (SELECT COUNT(*) FROM files WHERE channelId = :channelId AND isPublished = 1), lastUpdated = :timestamp WHERE id = :channelId")
    suspend fun syncChannelStats(channelId: String, timestamp: Long = System.currentTimeMillis())

    // --- Files (Admin Queries - includes unpublished/drafts) ---
    @Query("SELECT * FROM files ORDER BY uploadTimestamp DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE channelId = :channelId ORDER BY uploadTimestamp DESC")
    fun getFilesByChannel(channelId: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY uploadTimestamp DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 10): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY downloadCount DESC LIMIT :limit")
    fun getPopularFiles(limit: Int = 10): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :fileId LIMIT 1")
    fun getFileById(fileId: String): Flow<FileEntity?>

    // --- Published Files (for Normal Users - published only) ---
    @Query("SELECT * FROM files WHERE isPublished = 1 ORDER BY uploadTimestamp DESC")
    fun getPublishedFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE channelId = :channelId AND isPublished = 1 ORDER BY uploadTimestamp DESC")
    fun getPublishedFilesByChannel(channelId: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isPublished = 1 ORDER BY uploadTimestamp DESC LIMIT :limit")
    fun getRecentPublishedFiles(limit: Int = 10): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isPublished = 1 ORDER BY downloadCount DESC LIMIT :limit")
    fun getPopularPublishedFiles(limit: Int = 10): Flow<List<FileEntity>>

    @Query("SELECT COUNT(*) FROM files WHERE isPublished = 1")
    fun getPublishedFileCount(): Flow<Int>

    @Query("UPDATE files SET isPublished = :isPublished WHERE id = :fileId")
    suspend fun updatePublishStatus(fileId: String, isPublished: Boolean)

    // --- File Mutations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: String)

    @Query("UPDATE files SET downloadCount = downloadCount + 1, isDownloaded = 1, localPath = :localPath WHERE id = :fileId")
    suspend fun markFileDownloaded(fileId: String, localPath: String)

    @Query("SELECT COUNT(*) FROM channels")
    fun getChannelCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM files")
    fun getFileCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(downloadCount), 0) FROM files")
    fun getTotalDownloads(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM files")
    fun getTotalStorageBytes(): Flow<Long>

    // --- Download Records ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadRecord(record: DownloadRecordEntity)

    @Query("SELECT * FROM download_records ORDER BY timestamp DESC LIMIT 20")
    fun getRecentDownloadRecords(): Flow<List<DownloadRecordEntity>>
}
