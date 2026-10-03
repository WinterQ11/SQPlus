package com.example.data.supabase

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class StorageService(
    private val context: Context,
    private val config: SupabaseConfig,
    private val authRepository: AuthRepository,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "StorageService"
        const val MAX_FILE_SIZE_BYTES = 500L * 1024L * 1024L // 500 MB
        val SUPPORTED_TYPES = setOf("PDF", "DOC", "DOCX", "XLSX", "PPTX", "ZIP", "APK", "TXT", "MP4")
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    suspend fun uploadFile(
        channelId: String,
        documentId: String,
        fileName: String,
        file: File,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
        }

        if (file.length() > MAX_FILE_SIZE_BYTES) {
            return@withContext Result.failure(
                IllegalArgumentException("File size exceeds 500MB limit (${file.length()} bytes)")
            )
        }

        val baseUrl = config.getSupabaseUrl()
        val bucket = config.getStorageBucket()
        val storagePath = "channels/$channelId/$documentId/$fileName"
        val endpoint = "$baseUrl/storage/v1/object/$bucket/$storagePath"

        val mimeType = getMimeType(fileName)
        val mediaType = mimeType.toMediaType()

        val requestBody = object : RequestBody() {
            override fun contentType() = mediaType
            override fun contentLength() = file.length()
            override fun writeTo(sink: BufferedSink) {
                val totalBytes = file.length()
                var bytesWritten = 0L
                file.source().use { source ->
                    val buffer = okio.Buffer()
                    var read: Long
                    while (source.read(buffer, 8192L).also { read = it } != -1L) {
                        sink.write(buffer, read)
                        bytesWritten += read
                        if (totalBytes > 0) {
                            val progress = (bytesWritten.toFloat() / totalBytes).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                }
            }
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("x-upsert", "true")
            .post(requestBody)

        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    val publicDownloadUrl = "$baseUrl/storage/v1/object/public/$bucket/$storagePath"
                    Result.success(publicDownloadUrl)
                } else {
                    val err = response.body?.string().orEmpty()
                    Log.e(TAG, "Storage upload failed: HTTP ${response.code} $err")
                    Result.failure(IllegalStateException("Supabase Storage upload failed: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Storage upload exception", e)
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(downloadUrl)
            authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Download failed with HTTP ${response.code}")
                    )
                }

                val body = response.body ?: return@withContext Result.failure(IllegalStateException("Empty body"))
                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalBytes > 0) {
                        onProgress((totalRead.toFloat() / totalBytes).coerceIn(0f, 1f))
                    }
                }

                outputStream.flush()
                outputStream.close()
                Result.success(destinationFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(channelId: String, documentId: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val bucket = config.getStorageBucket()
        val storagePath = "channels/$channelId/$documentId/$fileName"
        val endpoint = "$baseUrl/storage/v1/object/$bucket/$storagePath"

        val requestBuilder = Request.Builder().url(endpoint).delete()
        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Failed to delete storage asset: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
