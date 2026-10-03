package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.ui.components.FileFormatUtils
import com.example.ui.components.LuxuryEmptyState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedBurgundy
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftIvory
import com.example.ui.viewmodel.DashboardStats

@Composable
fun AdminDashboardScreen(
    stats: DashboardStats,
    channels: List<ChannelEntity>,
    files: List<FileEntity>,
    onBack: () -> Unit,
    onCreateChannelClick: () -> Unit,
    onUploadFileClick: (String?) -> Unit,
    onEditChannelClick: (ChannelEntity) -> Unit,
    onDeleteChannelClick: (String, String) -> Unit,
    onEditFileClick: (FileEntity) -> Unit,
    onDeleteFileClick: (FileEntity) -> Unit,
    onMoveFileClick: ((FileEntity, String, String) -> Unit)? = null,
    onTogglePublishFile: ((FileEntity) -> Unit)? = null
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var channelToDelete by remember { mutableStateOf<ChannelEntity?>(null) }
    var fileToDelete by remember { mutableStateOf<FileEntity?>(null) }
    var fileToMove by remember { mutableStateOf<FileEntity?>(null) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredChannels = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) channels
        else channels.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) || it.fileType.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Title
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Admin Management Console",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftIvory
                        )
                        Text(
                            text = "Authenticated Authority • Full Cloud Governance",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneGold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(1.dp, ChampagneGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Stats 2x2 Grid (Compact, luxury styling)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        title = "Channels",
                        value = "${stats.totalChannels}",
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Cloud Files",
                        value = "${stats.totalFiles}",
                        icon = Icons.Default.InsertDriveFile,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        title = "Storage In Use",
                        value = FileFormatUtils.formatFileSize(stats.totalStorageBytes),
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Total Transfers",
                        value = "${stats.totalDownloads}",
                        icon = Icons.Default.CloudDownload,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Primary Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCreateChannelClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_create_channel_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Channel", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onUploadFileClick(null) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_upload_file_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload File", color = ChampagneGold, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search in Admin Console
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter channels or files...", color = MutedTaupe) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_search_filter"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GraphiteCard,
                    unfocusedContainerColor = GraphiteCard,
                    focusedBorderColor = ChampagneGold,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = SoftIvory,
                    unfocusedTextColor = SoftIvory
                )
            )
        }

        // Tabs: Channels vs Files
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                containerColor = GraphiteCard,
                contentColor = ChampagneGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ChampagneGold
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Channels (${channels.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) ChampagneGold else MutedTaupe
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "All Files (${files.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) ChampagneGold else MutedTaupe
                        )
                    }
                )
            }
        }

        // Content: Channel Management
        if (selectedTab == 0) {
            if (filteredChannels.isEmpty()) {
                item {
                    LuxuryEmptyState(
                        title = if (channels.isEmpty()) "No Channels" else "No Matching Channels",
                        subtitle = if (channels.isEmpty()) "Your vault has no channels yet. Tap 'Create Channel' above to publish one."
                                   else "No channels match \"$searchQuery\".",
                        icon = Icons.Default.Folder,
                        actionLabel = if (channels.isEmpty()) "Create Channel" else null,
                        onActionClick = if (channels.isEmpty()) onCreateChannelClick else null
                    )
                }
            } else {
                items(filteredChannels, key = { "admin_ch_${it.id}" }) { ch ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_channel_item_${ch.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FileFormatUtils.getChannelIcon(ch.iconName),
                                    contentDescription = null,
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ch.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftIvory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${ch.category} • ${ch.fileCount} files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedTaupe
                                )
                            }

                            IconButton(
                                onClick = { onEditChannelClick(ch) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Channel", tint = ChampagneGold, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { channelToDelete = ch },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Channel", tint = MutedBurgundy, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // Content: File Management
            if (filteredFiles.isEmpty()) {
                item {
                    LuxuryEmptyState(
                        title = if (files.isEmpty()) "No Files Uploaded" else "No Matching Files",
                        subtitle = if (files.isEmpty()) "Your cloud storage is completely clear. Tap 'Upload File' above to add genuine content."
                                   else "No files match \"$searchQuery\".",
                        icon = Icons.Default.InsertDriveFile,
                        actionLabel = if (files.isEmpty()) "Upload File" else null,
                        onActionClick = if (files.isEmpty()) { { onUploadFileClick(null) } } else null
                    )
                }
            } else {
                items(filteredFiles, key = { "admin_file_${it.id}" }) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_file_item_${file.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FileFormatUtils.getFileTypeIcon(file.fileType),
                                    contentDescription = null,
                                    tint = FileFormatUtils.getFileTypeColor(file.fileType),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftIvory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${FileFormatUtils.formatFileSize(file.fileSizeBytes)} • ${file.fileType} • ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedTaupe
                                    )
                                    Text(
                                        text = if (file.isPublished) "LIVE" else "DRAFT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (file.isPublished) ChampagneGold else MutedBurgundy
                                    )
                                }
                            }

                            if (onTogglePublishFile != null) {
                                IconButton(
                                    onClick = { onTogglePublishFile(file) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (file.isPublished) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (file.isPublished) "Unpublish File" else "Publish File",
                                        tint = if (file.isPublished) ChampagneGold else MutedTaupe,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (onMoveFileClick != null && channels.size > 1) {
                                IconButton(
                                    onClick = { fileToMove = file },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.DriveFileMove, contentDescription = "Move File", tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                }
                            }

                            IconButton(
                                onClick = { onEditFileClick(file) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit File", tint = ChampagneGold, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { fileToDelete = file },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = MutedBurgundy, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation for Channel Deletion
    if (channelToDelete != null) {
        val channel = channelToDelete!!
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = {
                Text(
                    text = "Delete Channel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftIvory
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${channel.name}\"?\n\nDeleting this channel will also remove all associated files inside it from cloud storage and the database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTaupe
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteChannelClick(channel.id, channel.name)
                        channelToDelete = null
                    }
                ) {
                    Text("Delete Channel", color = MutedBurgundy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("Cancel", color = MutedTaupe)
                }
            },
            containerColor = ElevatedSurface
        )
    }

    // Confirmation for File Deletion
    if (fileToDelete != null) {
        val file = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = {
                Text(
                    text = "Delete File",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftIvory
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${file.name}\"?\n\nThis will remove the file from cloud storage and delete its metadata record.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTaupe
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFileClick(file)
                        fileToDelete = null
                    }
                ) {
                    Text("Delete File", color = MutedBurgundy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = MutedTaupe)
                }
            },
            containerColor = ElevatedSurface
        )
    }

    // Move File Dialog
    if (fileToMove != null && onMoveFileClick != null) {
        val file = fileToMove!!
        AlertDialog(
            onDismissRequest = { fileToMove = null },
            title = {
                Text("Move \"${file.name}\"", color = SoftIvory, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Select destination channel:", color = MutedTaupe, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    channels.filter { it.id != file.channelId }.forEach { ch ->
                        Surface(
                            onClick = {
                                onMoveFileClick(file, ch.id, ch.name)
                                fileToMove = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = GraphiteCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ch.name, color = SoftIvory, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { fileToMove = null }) { Text("Cancel", color = MutedTaupe) }
            },
            containerColor = ElevatedSurface
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SoftIvory
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MutedTaupe
            )
        }
    }
}
