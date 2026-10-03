package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.ui.components.AdminChannelDialog
import com.example.ui.components.AdminUploadDialog
import com.example.ui.components.FileDetailDialog
import com.example.ui.components.SqPlusBottomNav
import com.example.ui.components.SqPlusTopBar
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SqPlusViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainAppScaffold(
    viewModel: SqPlusViewModel
) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isSupabaseAvailable by viewModel.isSupabaseAvailable.collectAsStateWithLifecycle()
    val adminEmail by viewModel.adminEmail.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val cloudConfig by viewModel.cloudConfig.collectAsStateWithLifecycle()

    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val featuredChannels by viewModel.featuredChannels.collectAsStateWithLifecycle()
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val popularFiles by viewModel.popularFiles.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()

    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val channelFiles by viewModel.channelFiles.collectAsStateWithLifecycle()
    val channelSearchQuery by viewModel.channelSearchQuery.collectAsStateWithLifecycle()
    val channelSortOption by viewModel.channelSortOption.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val searchSort by viewModel.searchSort.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val matchingChannels by viewModel.matchingChannels.collectAsStateWithLifecycle()

    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val downloadsState by viewModel.downloadsState.collectAsStateWithLifecycle()
    val previewFile by viewModel.previewFile.collectAsStateWithLifecycle()

    // Dialog States
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadDialogTargetChannelId by remember { mutableStateOf<String?>(null) }
    var editingFileForUpload by remember { mutableStateOf<FileEntity?>(null) }

    var showChannelDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<ChannelEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = currentScreen != Screen.HOME) {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            SqPlusTopBar(
                currentScreen = currentScreen,
                isAdmin = isAdmin,
                title = when (currentScreen) {
                    Screen.HOME -> "SQPlus"
                    Screen.CHANNELS -> "Channels"
                    Screen.SEARCH -> "Search"
                    Screen.PROFILE -> "Profile"
                    Screen.CHANNEL_DETAIL -> selectedChannel?.name ?: "Channel"
                    Screen.ADMIN_DASHBOARD -> "Admin Console"
                },
                onBackClick = { viewModel.navigateBack() },
                onAdminClick = {
                    if (isAdmin) {
                        viewModel.navigateTo(Screen.ADMIN_DASHBOARD)
                    } else {
                        viewModel.navigateTo(Screen.PROFILE)
                    }
                }
            )
        },
        bottomBar = {
            SqPlusBottomNav(
                currentScreen = currentScreen,
                isAdmin = isAdmin,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("app_snackbar_host")
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_app_scaffold")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.HOME -> {
                        HomeScreen(
                            channels = allChannels,
                            featuredChannels = featuredChannels,
                            recentFiles = recentFiles,
                            popularFiles = popularFiles,
                            downloadsState = downloadsState,
                            isAdmin = isAdmin,
                            onChannelClick = { viewModel.selectChannel(it) },
                            onFileClick = { viewModel.openFilePreview(it) },
                            onDownloadFile = { viewModel.startDownload(it) },
                            onOpenFile = { viewModel.openDownloadedFile(it) },
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    Screen.CHANNELS -> {
                        ChannelsScreen(
                            channels = allChannels,
                            isAdmin = isAdmin,
                            onChannelClick = { viewModel.selectChannel(it) },
                            onCreateChannelClick = {
                                editingChannel = null
                                showChannelDialog = true
                            },
                            onEditChannelClick = {
                                editingChannel = it
                                showChannelDialog = true
                            },
                            onDeleteChannelClick = { viewModel.deleteChannel(it) }
                        )
                    }
                    Screen.CHANNEL_DETAIL -> {
                        ChannelDetailScreen(
                            channel = selectedChannel,
                            files = channelFiles,
                            downloadsState = downloadsState,
                            searchQuery = channelSearchQuery,
                            sortOption = channelSortOption,
                            isAdmin = isAdmin,
                            onBack = { viewModel.navigateBack() },
                            onSearchChange = { viewModel.setChannelSearchQuery(it) },
                            onSortChange = { viewModel.setChannelSortOption(it) },
                            onFileClick = { viewModel.openFilePreview(it) },
                            onDownloadFile = { viewModel.startDownload(it) },
                            onOpenFile = { viewModel.openDownloadedFile(it) },
                            onUploadFileClick = { channelId ->
                                uploadDialogTargetChannelId = channelId
                                editingFileForUpload = null
                                showUploadDialog = true
                            },
                            onEditFileClick = { file ->
                                editingFileForUpload = file
                                showUploadDialog = true
                            },
                            onDeleteFileClick = { file ->
                                viewModel.deleteFile(file)
                            },
                            onEditChannelClick = { ch ->
                                editingChannel = ch
                                showChannelDialog = true
                            },
                            onDeleteChannelClick = { chId ->
                                viewModel.deleteChannel(chId, selectedChannel?.name ?: "Channel")
                            }
                        )
                    }
                    Screen.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            searchFilter = searchFilter,
                            searchSort = searchSort,
                            matchingChannels = matchingChannels,
                            searchResults = searchResults,
                            downloadsState = downloadsState,
                            isAdmin = isAdmin,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onFilterChange = { viewModel.setSearchFilter(it) },
                            onSortChange = { viewModel.setSearchSort(it) },
                            onChannelClick = { viewModel.selectChannel(it) },
                            onFileClick = { viewModel.openFilePreview(it) },
                            onDownloadFile = { viewModel.startDownload(it) },
                            onOpenFile = { viewModel.openDownloadedFile(it) }
                        )
                    }
                    Screen.PROFILE -> {
                        ProfileScreen(
                            isAdmin = isAdmin,
                            adminEmail = adminEmail,
                            isSupabaseAvailable = isSupabaseAvailable,
                            themePreference = themePreference,
                            stats = dashboardStats,
                            networkStatus = networkStatus,
                            cloudConfig = cloudConfig,
                            onLoginAdminWithEmail = { email, pass, onResult -> viewModel.loginAdminWithSupabase(email, pass, onResult) },
                            onSendPasswordReset = { email -> viewModel.sendPasswordReset(email) },
                            onLogoutAdmin = { viewModel.logoutAdmin() },
                            onThemeChange = { viewModel.setThemePreference(it) },
                            onSaveCloudConfig = { viewModel.updateCloudConfig(it) },
                            onRefreshCloudSync = { viewModel.refreshCloudSync() },
                            onNavigateToAdminDashboard = { viewModel.navigateTo(Screen.ADMIN_DASHBOARD) }
                        )
                    }
                    Screen.ADMIN_DASHBOARD -> {
                        AdminDashboardScreen(
                            stats = dashboardStats,
                            channels = allChannels,
                            files = allFiles,
                            onBack = { viewModel.navigateBack() },
                            onCreateChannelClick = {
                                editingChannel = null
                                showChannelDialog = true
                            },
                            onUploadFileClick = { chId ->
                                uploadDialogTargetChannelId = chId
                                editingFileForUpload = null
                                showUploadDialog = true
                            },
                            onEditChannelClick = { ch ->
                                editingChannel = ch
                                showChannelDialog = true
                            },
                            onDeleteChannelClick = { chId, chName ->
                                viewModel.deleteChannel(chId, chName)
                            },
                            onEditFileClick = { file ->
                                editingFileForUpload = file
                                showUploadDialog = true
                            },
                            onDeleteFileClick = { file ->
                                viewModel.deleteFile(file)
                            },
                            onMoveFileClick = { file, newChId, newChName ->
                                viewModel.moveFile(file, newChId, newChName)
                            },
                            onTogglePublishFile = { file ->
                                viewModel.togglePublishStatus(file)
                            }
                        )
                    }
                }
            }
        }
    }

    // Active File Detail / Preview Modal
    previewFile?.let { file ->
        FileDetailDialog(
            file = file,
            downloadState = downloadsState[file.id],
            isAdmin = isAdmin,
            onDismiss = { viewModel.closeFilePreview() },
            onDownloadClick = { viewModel.startDownload(file) },
            onOpenClick = { viewModel.openDownloadedFile(file) },
            onEditClick = if (isAdmin) {
                {
                    editingFileForUpload = file
                    showUploadDialog = true
                }
            } else null,
            onDeleteClick = if (isAdmin) {
                {
                    viewModel.deleteFile(file)
                }
            } else null
        )
    }

    // Admin Upload / Edit File Dialog
    if (showUploadDialog) {
        AdminUploadDialog(
            channels = allChannels,
            preselectedChannelId = uploadDialogTargetChannelId,
            editingFile = editingFileForUpload,
            onDismiss = {
                showUploadDialog = false
                editingFileForUpload = null
            },
            onUploadSuccess = { channelId, name, description, fileType, fileSizeBytes, category, isPublished ->
                viewModel.uploadFile(
                    channelId = channelId,
                    name = name,
                    description = description,
                    fileType = fileType,
                    fileSizeBytes = fileSizeBytes,
                    category = category,
                    isPublished = isPublished
                )
            },
            onUploadRealFile = { uri, channelId, category, isPublished ->
                viewModel.uploadRealFileUri(uri, channelId, category, isPublished)
            },
            onUploadMultipleRealFiles = { uris, channelId, category, isPublished ->
                viewModel.uploadMultipleRealFiles(uris, channelId, category, isPublished)
            },
            onUpdateSuccess = { updatedFile ->
                viewModel.updateFile(updatedFile)
            }
        )
    }

    // Admin Create / Edit Channel Dialog
    if (showChannelDialog) {
        AdminChannelDialog(
            editingChannel = editingChannel,
            onDismiss = {
                showChannelDialog = false
                editingChannel = null
            },
            onSubmit = { name, description, category, iconName, isFeatured ->
                if (editingChannel != null) {
                    viewModel.updateChannel(
                        editingChannel!!.copy(
                            name = name,
                            description = description,
                            category = category,
                            iconName = iconName,
                            isFeatured = isFeatured
                        )
                    )
                } else {
                    viewModel.createChannel(
                        name = name,
                        description = description,
                        category = category,
                        iconName = iconName,
                        isFeatured = isFeatured
                    )
                }
            }
        )
    }
}
