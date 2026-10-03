package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.local.SqPlusDao
import com.example.data.model.ChannelEntity
import com.example.data.model.DownloadRecordEntity
import com.example.data.model.FileEntity
import com.example.data.remote.CloudBackendConfig
import com.example.data.remote.CloudSyncEngine
import com.example.data.remote.NetworkStatus
import com.example.data.supabase.AuthRepository
import com.example.data.supabase.ChannelRepository
import com.example.data.supabase.DocumentRepository
import com.example.data.supabase.RealtimeChangeEvent
import com.example.data.supabase.RealtimeSyncService
import com.example.data.supabase.StorageService
import com.example.data.supabase.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SqPlusRepository(
    private val dao: SqPlusDao,
    private val context: Context
) {
    val supabaseConfig = SupabaseConfig(context)
    val authRepository = AuthRepository(supabaseConfig)
    val channelRepository = ChannelRepository(supabaseConfig, authRepository)
    val documentRepository = DocumentRepository(supabaseConfig, authRepository)
    val storageService = StorageService(context, supabaseConfig, authRepository)
    val cloudSyncEngine: CloudSyncEngine = CloudSyncEngine(context)

    private val _isSupabaseAvailable = MutableStateFlow(true)
    val isSupabaseAvailable: StateFlow<Boolean> = _isSupabaseAvailable.asStateFlow()

    val networkStatus: StateFlow<NetworkStatus> = cloudSyncEngine.networkStatus
    val isAdmin: StateFlow<Boolean> = authRepository.isAdmin
    val adminEmail: StateFlow<String?> = authRepository.currentUserEmail

    val allChannels: Flow<List<ChannelEntity>> = dao.getAllChannels()
    val featuredChannels: Flow<List<ChannelEntity>> = dao.getFeaturedChannels()

    val allFiles: Flow<List<FileEntity>> = dao.getAllFiles()
    val publishedFiles: Flow<List<FileEntity>> = dao.getPublishedFiles()

    val recentFiles: Flow<List<FileEntity>> = dao.getRecentFiles(15)
    val recentPublishedFiles: Flow<List<FileEntity>> = dao.getRecentPublishedFiles(15)

    val popularFiles: Flow<List<FileEntity>> = dao.getPopularFiles(10)
    val popularPublishedFiles: Flow<List<FileEntity>> = dao.getPopularPublishedFiles(10)

    val totalChannels: Flow<Int> = dao.getChannelCount()
    val totalFiles: Flow<Int> = dao.getFileCount()
    val totalDownloads: Flow<Int> = dao.getTotalDownloads()
    val totalStorageBytes: Flow<Long> = dao.getTotalStorageBytes()
    val recentDownloads: Flow<List<DownloadRecordEntity>> = dao.getRecentDownloadRecords()

    private val activeDownloads = ConcurrentHashMap<String, Float>()
    private var realtimeSyncService: RealtimeSyncService? = null

    fun isDownloading(fileId: String): Boolean = activeDownloads.containsKey(fileId)
    fun getDownloadProgress(fileId: String): Float = activeDownloads[fileId] ?: 0f

    fun getChannel(channelId: String): Flow<ChannelEntity?> = dao.getChannelById(channelId)
    fun getFilesForChannel(channelId: String, publishedOnly: Boolean = false): Flow<List<FileEntity>> {
        return if (publishedOnly) dao.getPublishedFilesByChannel(channelId)
        else dao.getFilesByChannel(channelId)
    }
    fun getFile(fileId: String): Flow<FileEntity?> = dao.getFileById(fileId)

    /**
     * Connects Supabase Realtime WebSocket listeners and syncs initial data.
     */
    fun startRealtimeCloudSync(coroutineScope: CoroutineScope) {
        realtimeSyncService?.stop()
        val realtime = RealtimeSyncService(supabaseConfig, coroutineScope)
        realtimeSyncService = realtime
        realtime.start()

        // 1. Initial REST Sync
        coroutineScope.launch(Dispatchers.IO) {
            cleanAndSyncDatabase()
        }

        // 2. React to Realtime Changes from Supabase WebSocket
        coroutineScope.launch(Dispatchers.IO) {
            realtime.changeEvents.collect { event ->
                when (event) {
                    is RealtimeChangeEvent.ChannelChanged -> {
                        refreshChannelsFromCloud()
                    }
                    is RealtimeChangeEvent.DocumentChanged -> {
                        refreshDocumentsFromCloud()
                    }
                    is RealtimeChangeEvent.RefreshAll -> {
                        cleanAndSyncDatabase()
                    }
                }
            }
        }

        // 3. React to Admin authentication state changes
        coroutineScope.launch(Dispatchers.IO) {
            isAdmin.collect {
                refreshDocumentsFromCloud()
            }
        }
    }

    suspend fun cleanAndSyncDatabase() = withContext(Dispatchers.IO) {
        // Purge legacy demo channels & mock files
        val demoIds = SeedData.legacyDemoChannelIds.toList()
        dao.deleteChannelsByIds(demoIds)
        dao.deleteFilesByChannelIds(demoIds)

        // Fetch from Supabase
        refreshChannelsFromCloud()
        refreshDocumentsFromCloud()
    }

    private suspend fun refreshChannelsFromCloud() = withContext(Dispatchers.IO) {
        val result = channelRepository.fetchChannels()
        result.onSuccess { remoteChannels ->
            _isSupabaseAvailable.value = true
            val validChannels = remoteChannels.filter { !SeedData.isDemoChannel(it.id) }
            val localChannels = dao.getAllChannels().first()
            val remoteIds = validChannels.map { it.id }.toSet()

            for (local in localChannels) {
                if (local.id !in remoteIds && !SeedData.isDemoChannel(local.id)) {
                    dao.deleteChannelById(local.id)
                    dao.deleteFilesByChannelId(local.id)
                }
            }

            if (validChannels.isNotEmpty()) {
                dao.insertChannels(validChannels)
            }
        }.onFailure {
            // Keep local cached data if offline
        }
    }

    private suspend fun refreshDocumentsFromCloud() = withContext(Dispatchers.IO) {
        val adminMode = isAdmin.value
        val result = documentRepository.fetchDocuments(publishedOnly = !adminMode)
        result.onSuccess { remoteFiles ->
            _isSupabaseAvailable.value = true
            val validFiles = remoteFiles.filter { !SeedData.isDemoFile(it.id) && !SeedData.isDemoChannel(it.channelId) }
            val localFiles = dao.getAllFiles().first()
            val remoteIds = validFiles.map { it.id }.toSet()

            for (local in localFiles) {
                if (local.id !in remoteIds && !SeedData.isDemoFile(local.id)) {
                    dao.deleteFileById(local.id)
                }
            }

            if (validFiles.isNotEmpty()) {
                dao.insertFiles(validFiles)
                val channelIds = validFiles.map { it.channelId }.distinct()
                for (chId in channelIds) {
                    dao.syncChannelStats(chId)
                }
            }
        }.onFailure {
            // Keep local cached data if offline
        }
    }

    suspend fun createChannel(channel: ChannelEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val res = channelRepository.createChannel(channel)
        if (res.isSuccess) {
            dao.insertChannel(channel)
            Result.success(Unit)
        } else {
            // Fallback save to local dao if network glitch
            dao.insertChannel(channel)
            res
        }
    }

    suspend fun updateChannel(channel: ChannelEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val res = channelRepository.updateChannel(channel)
        dao.updateChannel(channel)
        res
    }

    suspend fun deleteChannel(channelId: String): Result<Int> = withContext(Dispatchers.IO) {
        val filesToDelete = dao.getFileListByChannel(channelId)
        val filesCount = filesToDelete.size

        for (file in filesToDelete) {
            storageService.deleteFile(channelId, file.id, file.name)
            documentRepository.deleteDocument(file.id)
        }

        channelRepository.deleteChannel(channelId)
        dao.deleteFilesByChannelId(channelId)
        dao.deleteChannelById(channelId)
        Result.success(filesCount)
    }

    suspend fun addFile(file: FileEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
            val localPayload = File(uploadsDir, file.name)
            if (!localPayload.exists()) {
                val header = "--- SQPlus Cloud File Payload ---\n" +
                        "File: ${file.name}\n" +
                        "Channel: ${file.channelId}\n" +
                        "Category: ${file.category}\n" +
                        "Created: ${file.uploadTimestamp}\n" +
                        "---------------------------------\n"
                FileOutputStream(localPayload).use { it.write(header.toByteArray()) }
            }

            // 1. Verify upload to Supabase Storage before saving metadata
            val uploadResult = storageService.uploadFile(
                channelId = file.channelId,
                documentId = file.id,
                fileName = file.name,
                file = localPayload,
                onProgress = {}
            )

            if (uploadResult.isFailure) {
                return@withContext Result.failure(
                    IllegalStateException("Storage upload failed: ${uploadResult.exceptionOrNull()?.message}. Metadata was not saved.")
                )
            }

            val downloadUrl = uploadResult.getOrNull() ?: file.downloadUrl
            if (downloadUrl.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Storage upload did not return a valid download URL. Metadata was not saved.")
                )
            }

            val finalFile = file.copy(
                downloadUrl = downloadUrl,
                localPath = localPayload.absolutePath
            )

            // 2. Persist metadata to Supabase PostgreSQL and local Room
            val createResult = documentRepository.createDocument(finalFile)
            dao.insertFile(finalFile)
            dao.syncChannelStats(finalFile.channelId)

            createResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFile(file: FileEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = documentRepository.updateDocument(file)
            dao.updateFile(file)
            dao.syncChannelStats(file.channelId)
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePublishStatus(file: FileEntity, isPublished: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = documentRepository.togglePublishStatus(file.id, isPublished)
            val updated = file.copy(isPublished = isPublished)
            dao.updateFile(updated)
            dao.syncChannelStats(file.channelId)
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFile(file: FileEntity, newChannelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val oldChannelId = file.channelId
            val updated = file.copy(channelId = newChannelId)
            val res = documentRepository.updateDocument(updated)
            dao.updateFile(updated)
            dao.syncChannelStats(oldChannelId)
            dao.syncChannelStats(newChannelId)
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(fileId: String, channelId: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            storageService.deleteFile(channelId, fileId, fileName)
            documentRepository.deleteDocument(fileId)
            dao.deleteFileById(fileId)
            dao.syncChannelStats(channelId)
            Result.success(Unit)
        } catch (e: Exception) {
            dao.deleteFileById(fileId)
            dao.syncChannelStats(channelId)
            Result.failure(e)
        }
    }

    suspend fun addRealUploadedFile(
        uri: Uri,
        channelId: String,
        category: String,
        isPublished: Boolean = true,
        onProgress: (Float) -> Unit = {}
    ): Result<FileEntity> = withContext(Dispatchers.IO) {
        val result = cloudSyncEngine.processRealUploadedFile(uri, channelId, category)
        if (result.isFailure) {
            return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to process local file"))
        }

        val fileEntity = result.getOrThrow()
        val localFile = fileEntity.localPath?.let { File(it) }
        if (localFile == null || !localFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Local file cache not found for upload."))
        }

        // 1. Verify that Supabase Storage file upload succeeds before saving document metadata
        val uploadResult = storageService.uploadFile(
            channelId = channelId,
            documentId = fileEntity.id,
            fileName = fileEntity.name,
            file = localFile,
            onProgress = onProgress
        )

        if (uploadResult.isFailure) {
            val error = uploadResult.exceptionOrNull()
            return@withContext Result.failure(
                IllegalStateException("Supabase Storage upload failed: ${error?.message}. Document metadata was not saved.")
            )
        }

        val downloadUrl = uploadResult.getOrNull() ?: ""
        if (downloadUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase Storage returned an empty download URL. Document metadata was not saved.")
            )
        }

        val finalEntity = fileEntity.copy(
            downloadUrl = downloadUrl,
            isPublished = isPublished
        )

        // 2. Only after storage upload succeeds, save metadata to Supabase PostgreSQL and Room
        val dbResult = documentRepository.createDocument(finalEntity)
        dao.insertFile(finalEntity)
        dao.syncChannelStats(channelId)

        if (dbResult.isFailure) {
            return@withContext Result.failure(
                dbResult.exceptionOrNull() ?: Exception("Failed to save document metadata in Supabase.")
            )
        }

        Result.success(finalEntity)
    }

    suspend fun executeDownload(
        file: FileEntity,
        onProgress: (Float) -> Unit,
        onSuccess: (File, String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (activeDownloads.containsKey(file.id)) {
            withContext(Dispatchers.Main) {
                onError("Download is already running for this file.")
            }
            return@withContext
        }

        activeDownloads[file.id] = 0f

        val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
        val targetFile = File(downloadsDir, file.name)

        // Download directly from Supabase Storage
        val downloadResult = storageService.downloadFile(
            downloadUrl = file.downloadUrl,
            destinationFile = targetFile,
            onProgress = { progress ->
                activeDownloads[file.id] = progress
                onProgress(progress)
            }
        )

        if (downloadResult.isSuccess) {
            activeDownloads.remove(file.id)
            dao.markFileDownloaded(file.id, targetFile.absolutePath)
            dao.insertDownloadRecord(
                DownloadRecordEntity(
                    id = UUID.randomUUID().toString(),
                    fileId = file.id,
                    fileName = file.name,
                    fileSizeBytes = file.fileSizeBytes
                )
            )
            // Increment download count in Supabase
            documentRepository.incrementDownloadCount(file.id)
            onSuccess(targetFile, "Verified-Supabase-Storage")
            return@withContext
        }

        // Fallback to local copy if available
        val existingLocal = file.localPath?.let { File(it) }
        if (existingLocal != null && existingLocal.exists()) {
            activeDownloads.remove(file.id)
            dao.markFileDownloaded(file.id, existingLocal.absolutePath)
            onSuccess(existingLocal, "Verified-Local-Copy")
            return@withContext
        }

        activeDownloads.remove(file.id)
        withContext(Dispatchers.Main) {
            onError("Cloud file could not be downloaded from Supabase Storage.")
        }
    }

    fun openDownloadedFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = getMimeType(file.name) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }

    // Auth Pass-Through
    suspend fun loginAdminWithSupabase(email: String, password: String): Result<Boolean> {
        return authRepository.loginWithEmail(email, password)
    }

    suspend fun sendPasswordReset(email: String): Result<Boolean> {
        return authRepository.sendPasswordReset(email)
    }

    suspend fun logoutAdmin(): Result<Unit> {
        return authRepository.logout()
    }

    fun getCloudConfig(): CloudBackendConfig = cloudSyncEngine.getCloudConfig()
    fun saveCloudConfig(config: CloudBackendConfig) {
        cloudSyncEngine.saveCloudConfig(config)
        supabaseConfig.setCustomConfig(config.endpointUrl, config.apiKey)
    }
}
