package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SqPlusDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.data.remote.CloudBackendConfig
import com.example.data.remote.NetworkStatus
import com.example.data.repository.SqPlusRepository
import com.example.data.supabase.SupabaseConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SqPlusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SqPlusRepository

    init {
        val database = SqPlusDatabase.getInstance(application)
        repository = SqPlusRepository(database.sqPlusDao(), application)
        viewModelScope.launch {
            repository.cleanAndSyncDatabase()
            repository.startRealtimeCloudSync(viewModelScope)
        }
    }

    // --- Network & Cloud Sync Status ---
    val networkStatus: StateFlow<NetworkStatus> = repository.networkStatus
    val isSupabaseAvailable: StateFlow<Boolean> = repository.isSupabaseAvailable

    private val _cloudConfig = MutableStateFlow(repository.getCloudConfig())
    val cloudConfig: StateFlow<CloudBackendConfig> = _cloudConfig.asStateFlow()

    fun updateCloudConfig(config: CloudBackendConfig) {
        repository.saveCloudConfig(config)
        _cloudConfig.value = config
        postToast("Cloud settings updated.")
        refreshCloudSync()
    }

    fun refreshCloudSync() {
        viewModelScope.launch {
            repository.cleanAndSyncDatabase()
            postToast("Cloud channels synchronized.")
        }
    }

    // --- Admin Authentication (Supabase Auth) ---
    val isAdmin: StateFlow<Boolean> = repository.isAdmin

    private val _adminEmail = MutableStateFlow<String?>(SupabaseConfig.AUTHORIZED_ADMIN_EMAIL)
    val adminEmail: StateFlow<String?> = repository.adminEmail

    fun loginAdminWithSupabase(email: String, password: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.loginAdminWithSupabase(email, password)
            if (res.isSuccess) {
                val display = if (email.trim().equals("Winter", ignoreCase = true)) {
                    "Winter"
                } else {
                    email.trim().lowercase()
                }
                _adminEmail.value = display
                postToast("Welcome back, $display. Admin Console unlocked.")
                onResult?.invoke(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Authentication failed."
                postToast(err)
                onResult?.invoke(false, err)
            }
        }
    }

    // Backward-compatible alias for UI components
    fun loginAdminWithFirebase(email: String, password: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        loginAdminWithSupabase(email, password, onResult)
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            val res = repository.sendPasswordReset(email)
            if (res.isSuccess) {
                postToast("Password reset link dispatched to $email.")
            } else {
                postToast("Failed to send reset link: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun logoutAdmin() {
        viewModelScope.launch {
            repository.logoutAdmin()
            if (_currentScreen.value == Screen.ADMIN_DASHBOARD) {
                navigateTo(Screen.HOME)
            }
            postToast("Signed out of Admin Console.")
        }
    }

    // --- Navigation & Backstack ---
    private val screenStack = ArrayDeque<Screen>().apply { add(Screen.HOME) }
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.addLast(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeLast()
            _currentScreen.value = screenStack.last()
            return true
        }
        return false
    }

    // --- Selected Channel ---
    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    fun selectChannel(channelId: String) {
        _selectedChannelId.value = channelId
        _channelSearchQuery.value = ""
        navigateTo(Screen.CHANNEL_DETAIL)
    }

    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannelId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getChannel(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Data Streams from Repository ---
    val allChannels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredChannels: StateFlow<List<ChannelEntity>> = repository.featuredChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<FileEntity>> = isAdmin.flatMapLatest { admin ->
        if (admin) repository.recentFiles else repository.recentPublishedFiles
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularFiles: StateFlow<List<FileEntity>> = isAdmin.flatMapLatest { admin ->
        if (admin) repository.popularFiles else repository.popularPublishedFiles
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles: StateFlow<List<FileEntity>> = isAdmin.flatMapLatest { admin ->
        if (admin) repository.allFiles else repository.publishedFiles
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Channel Detail Files & In-Channel Search/Sort ---
    private val _channelSearchQuery = MutableStateFlow("")
    val channelSearchQuery: StateFlow<String> = _channelSearchQuery.asStateFlow()

    fun setChannelSearchQuery(query: String) {
        _channelSearchQuery.value = query
    }

    private val _channelSortOption = MutableStateFlow(SortOption.NEWEST)
    val channelSortOption: StateFlow<SortOption> = _channelSortOption.asStateFlow()

    fun setChannelSortOption(option: SortOption) {
        _channelSortOption.value = option
    }

    val channelFiles: StateFlow<List<FileEntity>> = combine(
        _selectedChannelId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else isAdmin.flatMapLatest { admin -> repository.getFilesForChannel(id, publishedOnly = !admin) }
        },
        _channelSearchQuery,
        _channelSortOption
    ) { files, query, sort ->
        filterAndSortFiles(files, query, FileTypeFilter.ALL, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Global Search & Filters ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _searchFilter = MutableStateFlow(FileTypeFilter.ALL)
    val searchFilter: StateFlow<FileTypeFilter> = _searchFilter.asStateFlow()

    fun setSearchFilter(filter: FileTypeFilter) {
        _searchFilter.value = filter
    }

    private val _searchSort = MutableStateFlow(SortOption.NEWEST)
    val searchSort: StateFlow<SortOption> = _searchSort.asStateFlow()

    fun setSearchSort(sort: SortOption) {
        _searchSort.value = sort
    }

    val searchResults: StateFlow<List<FileEntity>> = combine(
        allFiles,
        _searchQuery,
        _searchFilter,
        _searchSort
    ) { files, query, filter, sort ->
        filterAndSortFiles(files, query, filter, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matchingChannels: StateFlow<List<ChannelEntity>> = combine(
        allChannels,
        _searchQuery
    ) { channels, query ->
        if (query.isBlank()) emptyList()
        else channels.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Stats ---
    val dashboardStats: StateFlow<DashboardStats> = combine(
        repository.totalChannels,
        repository.totalFiles,
        repository.totalDownloads,
        repository.totalStorageBytes
    ) { channels, files, downloads, storage ->
        DashboardStats(
            totalChannels = channels,
            totalFiles = files,
            totalDownloads = downloads,
            totalStorageBytes = storage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // --- Downloads Management ---
    private val _downloadsState = MutableStateFlow<Map<String, DownloadProgressState>>(emptyMap())
    val downloadsState: StateFlow<Map<String, DownloadProgressState>> = _downloadsState.asStateFlow()

    fun startDownload(file: FileEntity) {
        val currentMap = _downloadsState.value
        if (currentMap[file.id]?.progress ?: 0f in 0.01f..0.99f) {
            postToast("Download is already running for ${file.name}")
            return
        }

        viewModelScope.launch {
            _downloadsState.value = _downloadsState.value + (file.id to DownloadProgressState(file.id, 0.01f))
            postToast("Connecting to cloud storage: ${file.name}")

            repository.executeDownload(
                file = file,
                onProgress = { progress ->
                    _downloadsState.value = _downloadsState.value + (
                        file.id to DownloadProgressState(file.id, progress)
                    )
                },
                onSuccess = { targetFile, checksum ->
                    _downloadsState.value = _downloadsState.value + (
                        file.id to DownloadProgressState(
                            fileId = file.id,
                            progress = 1.0f,
                            isComplete = true
                        )
                    )
                    postToast("Downloaded & verified: ${file.name}")
                },
                onError = { err ->
                    _downloadsState.value = _downloadsState.value + (
                        file.id to DownloadProgressState(
                            fileId = file.id,
                            progress = 0f,
                            errorMessage = err
                        )
                    )
                    postToast("Download error: $err")
                }
            )
        }
    }

    fun openDownloadedFile(file: FileEntity): Boolean {
        val path = file.localPath ?: return false
        val success = repository.openDownloadedFile(path)
        if (!success) {
            postToast("No application available to open this file type.")
        }
        return success
    }

    // --- Active Preview Dialog ---
    private val _previewFile = MutableStateFlow<FileEntity?>(null)
    val previewFile: StateFlow<FileEntity?> = _previewFile.asStateFlow()

    fun openFilePreview(file: FileEntity) {
        _previewFile.value = file
    }

    fun closeFilePreview() {
        _previewFile.value = null
    }

    // --- Theme Preference ---
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    fun setThemePreference(pref: ThemePreference) {
        _themePreference.value = pref
    }

    // --- Admin Channel Operations ---
    fun createChannel(name: String, description: String, category: String, iconName: String, isFeatured: Boolean) {
        if (!isAdmin.value) {
            postToast("Unauthorized: Admin credentials required")
            return
        }
        viewModelScope.launch {
            val newChannel = ChannelEntity(
                id = "chan_" + UUID.randomUUID().toString().take(8),
                name = name.trim(),
                description = description.trim(),
                category = category.trim(),
                iconName = iconName,
                fileCount = 0,
                lastUpdated = System.currentTimeMillis(),
                isFeatured = isFeatured
            )
            repository.createChannel(newChannel)
            postToast("Channel \"$name\" published to cloud")
        }
    }

    fun updateChannel(channel: ChannelEntity) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            repository.updateChannel(channel.copy(lastUpdated = System.currentTimeMillis()))
            postToast("Channel synchronized")
        }
    }

    fun deleteChannel(channelId: String, channelName: String = "Channel") {
        if (!isAdmin.value) {
            postToast("Unauthorized: Admin credentials required")
            return
        }
        viewModelScope.launch {
            val result = repository.deleteChannel(channelId)
            if (_selectedChannelId.value == channelId) {
                navigateBack()
            }
            result.onSuccess { filesRemoved ->
                if (filesRemoved > 0) {
                    postToast("Channel \"$channelName\" and $filesRemoved files permanently deleted.")
                } else {
                    postToast("Channel \"$channelName\" deleted.")
                }
            }.onFailure {
                postToast("Channel deleted locally.")
            }
        }
    }

    // --- Admin File Operations ---
    fun uploadFile(
        channelId: String,
        name: String,
        description: String,
        fileType: String,
        fileSizeBytes: Long,
        category: String,
        isPublished: Boolean = true
    ) {
        if (!isAdmin.value) {
            postToast("Unauthorized: Admin credentials required")
            return
        }
        viewModelScope.launch {
            val newFile = FileEntity(
                id = "file_" + UUID.randomUUID().toString().take(8),
                channelId = channelId,
                name = name.trim(),
                description = description.trim(),
                fileType = fileType.uppercase().trim(),
                fileSizeBytes = if (fileSizeBytes > 0) fileSizeBytes else 2_500_000L,
                uploadTimestamp = System.currentTimeMillis(),
                downloadCount = 0,
                category = category.trim().ifEmpty { "General" },
                isPublished = isPublished
            )
            val res = repository.addFile(newFile)
            if (res.isSuccess) {
                val statusText = if (isPublished) "published to cloud" else "saved as cloud draft"
                postToast("Document \"$name\" $statusText")
            } else {
                postToast("Upload failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun uploadRealFileUri(uri: Uri, channelId: String, category: String, isPublished: Boolean = true) {
        if (!isAdmin.value) {
            postToast("Unauthorized: Admin credentials required")
            return
        }
        viewModelScope.launch {
            val result = repository.addRealUploadedFile(uri, channelId, category, isPublished)
            result.onSuccess {
                val statusText = if (isPublished) "published to cloud vault!" else "saved as draft in cloud vault!"
                postToast("Document \"${it.name}\" $statusText")
            }.onFailure {
                postToast("Upload failed: ${it.message}")
            }
        }
    }

    fun uploadMultipleRealFiles(uris: List<Uri>, channelId: String, category: String, isPublished: Boolean = true) {
        if (!isAdmin.value) {
            postToast("Unauthorized: Admin credentials required")
            return
        }
        viewModelScope.launch {
            var successCount = 0
            for (uri in uris) {
                val result = repository.addRealUploadedFile(uri, channelId, category, isPublished)
                if (result.isSuccess) successCount++
            }
            if (successCount > 0) {
                val statusText = if (isPublished) "published to cloud vault!" else "saved as drafts!"
                postToast("$successCount document(s) $statusText")
            } else {
                postToast("Document uploads failed")
            }
        }
    }

    fun togglePublishStatus(file: FileEntity) {
        if (!isAdmin.value) return
        val newStatus = !file.isPublished
        viewModelScope.launch {
            val res = repository.togglePublishStatus(file, newStatus)
            if (res.isSuccess) {
                if (_previewFile.value?.id == file.id) {
                    _previewFile.value = file.copy(isPublished = newStatus)
                }
                val label = if (newStatus) "published and live for normal users" else "unpublished (hidden from normal users)"
                postToast("\"${file.name}\" is now $label.")
            } else {
                postToast("Failed to update status: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateFile(file: FileEntity) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            repository.updateFile(file)
            if (_previewFile.value?.id == file.id) {
                _previewFile.value = file
            }
            postToast("File details updated in cloud")
        }
    }

    fun moveFile(file: FileEntity, newChannelId: String, newChannelName: String) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            val res = repository.moveFile(file, newChannelId)
            res.onSuccess {
                postToast("Moved \"${file.name}\" to $newChannelName")
            }.onFailure {
                postToast("Failed to move file: ${it.message}")
            }
        }
    }

    fun deleteFile(file: FileEntity) {
        if (!isAdmin.value) {
            postToast("Unauthorized: Admin credentials required")
            return
        }
        viewModelScope.launch {
            val result = repository.deleteFile(file.id, file.channelId, file.name)
            if (_previewFile.value?.id == file.id) {
                closeFilePreview()
            }
            result.onSuccess {
                postToast("File \"${file.name}\" permanently removed.")
            }.onFailure {
                postToast("File removed locally.")
            }
        }
    }

    // --- Toast / Feedback Events ---
    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents = _toastEvents.asSharedFlow()

    private fun postToast(msg: String) {
        viewModelScope.launch {
            _toastEvents.emit(msg)
        }
    }

    // --- Filtering and Sorting Utility ---
    private fun filterAndSortFiles(
        files: List<FileEntity>,
        query: String,
        filter: FileTypeFilter,
        sort: SortOption
    ): List<FileEntity> {
        var result = files

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { file ->
                file.name.lowercase().contains(q) ||
                file.description.lowercase().contains(q) ||
                file.fileType.lowercase().contains(q) ||
                file.category.lowercase().contains(q)
            }
        }

        if (filter != FileTypeFilter.ALL) {
            result = result.filter { file ->
                filter.extensions.any { ext -> file.fileType.equals(ext, ignoreCase = true) }
            }
        }

        return when (sort) {
            SortOption.NEWEST -> result.sortedByDescending { it.uploadTimestamp }
            SortOption.OLDEST -> result.sortedBy { it.uploadTimestamp }
            SortOption.NAME_AZ -> result.sortedBy { it.name.lowercase() }
            SortOption.LARGEST -> result.sortedByDescending { it.fileSizeBytes }
            SortOption.SMALLEST -> result.sortedBy { it.fileSizeBytes }
        }
    }
}
