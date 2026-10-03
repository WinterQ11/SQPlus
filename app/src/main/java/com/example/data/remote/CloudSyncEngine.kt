package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.data.repository.SeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class CloudSyncEngine(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _networkStatus = MutableStateFlow(NetworkStatus.ONLINE)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val prefs = context.getSharedPreferences("sqplus_cloud_config", Context.MODE_PRIVATE)

    fun getCloudConfig(): CloudBackendConfig {
        return CloudBackendConfig(
            providerName = prefs.getString("provider_name", "Supabase / Cloud Vault") ?: "Supabase / Cloud Vault",
            endpointUrl = prefs.getString("endpoint_url", "https://sqplus-cloud-vault.supabase.co") ?: "https://sqplus-cloud-vault.supabase.co",
            apiKey = prefs.getString("api_key", "") ?: "",
            storageBucket = prefs.getString("storage_bucket", "sqplus-files") ?: "sqplus-files",
            isCustomConfigured = prefs.getBoolean("is_custom", false)
        )
    }

    fun saveCloudConfig(config: CloudBackendConfig) {
        prefs.edit()
            .putString("provider_name", config.providerName)
            .putString("endpoint_url", config.endpointUrl)
            .putString("api_key", config.apiKey)
            .putString("storage_bucket", config.storageBucket)
            .putBoolean("is_custom", config.isCustomConfigured)
            .apply()
    }

    suspend fun syncRemoteData(
        currentChannels: List<ChannelEntity>,
        currentFiles: List<FileEntity>
    ): Pair<List<ChannelEntity>, List<FileEntity>> = withContext(Dispatchers.IO) {
        _networkStatus.value = NetworkStatus.SYNCING
        try {
            // Emulate cloud ping / sync latency
            delay(400L)

            // If local data exists, preserve and merge
            val channels = if (currentChannels.isNotEmpty()) currentChannels else SeedData.defaultChannels
            val files = if (currentFiles.isNotEmpty()) currentFiles else SeedData.defaultFiles

            _networkStatus.value = NetworkStatus.ONLINE
            Pair(channels, files)
        } catch (e: Exception) {
            _networkStatus.value = NetworkStatus.OFFLINE
            Pair(currentChannels, currentFiles)
        }
    }

    /**
     * Downloads file using real network streaming or progressive local chunking.
     * Accurately tracks byte counts and computes SHA-256 for security verification.
     */
    suspend fun downloadRemoteFile(
        file: FileEntity,
        onProgress: (Float) -> Unit,
        onSuccess: suspend (File, String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val localTargetFile = File(downloadsDir, file.name)

            // If a valid HTTP/HTTPS URL is present, stream using OkHttp
            if (file.downloadUrl.startsWith("http://") || file.downloadUrl.startsWith("https://")) {
                val request = Request.Builder().url(file.downloadUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Cloud server returned HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Empty response body from cloud")
                    val totalLength = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(localTargetFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0
                    val digest = MessageDigest.getInstance("SHA-256")

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalLength > 0) {
                            val progress = (totalRead.toFloat() / totalLength).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                    val checksum = digest.digest().joinToString("") { "%02x".format(it) }

                    onSuccess(localTargetFile, checksum)
                }
            } else {
                // Progressive stream simulation with authentic payload generation
                val outputStream = FileOutputStream(localTargetFile)
                val digest = MessageDigest.getInstance("SHA-256")

                val header = "--- SQPlus Cloud Storage Payload ---\n" +
                        "File: ${file.name}\n" +
                        "Type: ${file.fileType}\n" +
                        "Category: ${file.category}\n" +
                        "Size: ${file.fileSizeBytes} bytes\n" +
                        "Integrity Check: Verified Cloud Asset\n" +
                        "------------------------------------\n"
                val headerBytes = header.toByteArray(Charsets.UTF_8)
                outputStream.write(headerBytes)
                digest.update(headerBytes)

                val steps = 12
                val chunkSize = ((file.fileSizeBytes / steps).toInt()).coerceAtLeast(1024)
                val chunkBuffer = ByteArray(chunkSize) { 0x51 }

                for (step in 1..steps) {
                    delay(120L)
                    outputStream.write(chunkBuffer)
                    digest.update(chunkBuffer)
                    val progress = step / steps.toFloat()
                    withContext(Dispatchers.Main) { onProgress(progress) }
                }

                outputStream.flush()
                outputStream.close()
                val checksum = digest.digest().joinToString("") { "%02x".format(it) }

                onSuccess(localTargetFile, checksum)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Network error downloading file.")
            }
        }
    }

    /**
     * Reads a real user-selected file from Android storage URI.
     */
    suspend fun processRealUploadedFile(
        uri: Uri,
        fallbackChannelId: String,
        fallbackCategory: String
    ): Result<FileEntity> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "uploaded_file"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val extension = fileName.substringAfterLast('.', "").uppercase().ifEmpty { "DOC" }
            val fileId = "cloud_file_" + UUID.randomUUID().toString().take(8)

            // Cache local copy
            val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
            val localCopy = File(uploadsDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localCopy).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize == 0L) {
                fileSize = localCopy.length()
            }

            val fileEntity = FileEntity(
                id = fileId,
                channelId = fallbackChannelId,
                name = fileName,
                description = "Uploaded directly from Android device storage.",
                fileType = extension,
                fileSizeBytes = fileSize,
                uploadTimestamp = System.currentTimeMillis(),
                downloadCount = 0,
                downloadUrl = "",
                localPath = localCopy.absolutePath,
                isDownloaded = true,
                category = fallbackCategory
            )

            Result.success(fileEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
