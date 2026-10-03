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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.ui.components.FileCard
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
import com.example.ui.viewmodel.DownloadProgressState
import com.example.ui.viewmodel.SortOption

@Composable
fun ChannelDetailScreen(
    channel: ChannelEntity?,
    files: List<FileEntity>,
    downloadsState: Map<String, DownloadProgressState>,
    searchQuery: String,
    sortOption: SortOption,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onFileClick: (FileEntity) -> Unit,
    onDownloadFile: (FileEntity) -> Unit,
    onOpenFile: (FileEntity) -> Unit,
    onUploadFileClick: (String) -> Unit,
    onEditFileClick: ((FileEntity) -> Unit)? = null,
    onDeleteFileClick: ((FileEntity) -> Unit)? = null,
    onEditChannelClick: ((ChannelEntity) -> Unit)? = null,
    onDeleteChannelClick: ((String) -> Unit)? = null
) {
    BackHandler { onBack() }

    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteChannelConfirm by remember { mutableStateOf(false) }

    if (channel == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .testTag("channel_detail_not_found"),
            contentAlignment = Alignment.Center
        ) {
            Text("Channel not found", style = MaterialTheme.typography.titleMedium, color = SoftIvory)
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { onUploadFileClick(channel.id) },
                    containerColor = ChampagneGold,
                    contentColor = ObsidianBlack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_upload_to_channel")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload to Channel", modifier = Modifier.size(24.dp))
                }
            }
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("channel_detail_screen"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Channel Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FileFormatUtils.getChannelIcon(channel.iconName),
                                    contentDescription = channel.name,
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftIvory
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElevatedSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Text(
                                            text = channel.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChampagneGold,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${files.size} files in channel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedTaupe
                                    )
                                }
                            }

                            if (isAdmin) {
                                Row {
                                    if (onEditChannelClick != null) {
                                        IconButton(onClick = { onEditChannelClick(channel) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Channel", tint = ChampagneGold)
                                        }
                                    }
                                    if (onDeleteChannelClick != null) {
                                        IconButton(onClick = { showDeleteChannelConfirm = true }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Channel", tint = MutedBurgundy)
                                        }
                                    }
                                }
                            }
                        }

                        if (channel.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = channel.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedTaupe,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Search & Sort bar inside channel (if files exist)
            if (files.isNotEmpty() || searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search files in channel...", color = MutedTaupe) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MutedTaupe, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("channel_search_input"),
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

                        Box {
                            Surface(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                color = GraphiteCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MutedTaupe)
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(ElevatedSurface)
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.displayName,
                                                color = if (sortOption == option) ChampagneGold else SoftIvory,
                                                fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSortChange(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Files List or Empty State
            if (files.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        LuxuryEmptyState(
                            title = if (searchQuery.isNotBlank()) "No Matching Files" else "Channel is Empty",
                            subtitle = if (searchQuery.isNotBlank()) "No files found matching \"$searchQuery\" in this channel."
                                       else "No documents or packages have been uploaded to this channel yet.",
                            icon = Icons.Default.InsertDriveFile,
                            actionLabel = if (isAdmin && searchQuery.isBlank()) "Upload First File" else null,
                            onActionClick = if (isAdmin && searchQuery.isBlank()) { { onUploadFileClick(channel.id) } } else null
                        )
                    }
                }
            } else {
                items(files, key = { it.id }) { file ->
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                        FileCard(
                            file = file,
                            downloadState = downloadsState[file.id],
                            isAdmin = isAdmin,
                            onClick = { onFileClick(file) },
                            onDownloadClick = { onDownloadFile(file) },
                            onOpenClick = { onOpenFile(file) },
                            onEditClick = if (isAdmin && onEditFileClick != null) { { onEditFileClick(file) } } else null,
                            onDeleteClick = if (isAdmin && onDeleteFileClick != null) { { onDeleteFileClick(file) } } else null
                        )
                    }
                }
            }
        }
    }

    // Confirmation for Channel Deletion
    if (showDeleteChannelConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteChannelConfirm = false },
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
                    text = "Are you sure you want to permanently delete \"${channel.name}\"? All ${files.size} files contained inside this channel will be removed from cloud storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTaupe
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteChannelConfirm = false
                        onDeleteChannelClick?.invoke(channel.id)
                    }
                ) {
                    Text("Delete Channel & Files", color = MutedBurgundy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChannelConfirm = false }) {
                    Text("Cancel", color = MutedTaupe)
                }
            },
            containerColor = ElevatedSurface
        )
    }
}
