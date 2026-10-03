package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedBurgundy
import com.example.ui.theme.MutedSage
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.SoftIvory
import com.example.ui.viewmodel.DownloadProgressState

@Composable
fun FileCard(
    file: FileEntity,
    downloadState: DownloadProgressState?,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onOpenClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onMoveClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDownloading = downloadState != null && downloadState.progress > 0f && !downloadState.isComplete
    val isComplete = file.isDownloaded || (downloadState?.isComplete == true)
    val progress = downloadState?.progress ?: 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "downloadProgress")

    val typeColor = FileFormatUtils.getFileTypeColor(file.fileType)
    val typeIcon = FileFormatUtils.getFileTypeIcon(file.fileType)

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_card_${file.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Badge Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = file.fileType,
                        tint = typeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ElevatedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = file.fileType.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text("•", style = MaterialTheme.typography.bodySmall, color = MutedTaupe)

                        Text(
                            text = FileFormatUtils.formatFileSize(file.fileSizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTaupe
                        )

                        Text("•", style = MaterialTheme.typography.bodySmall, color = MutedTaupe)

                        Text(
                            text = FileFormatUtils.formatRelativeTime(file.uploadTimestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTaupe
                        )
                    }
                }

                // Options Menu strictly available to verified admin
                if (isAdmin && (onEditClick != null || onDeleteClick != null || onMoveClick != null)) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .testTag("file_menu_button_${file.id}")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MutedTaupe
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(ElevatedSurface)
                        ) {
                            if (onEditClick != null) {
                                DropdownMenuItem(
                                    text = { Text("Edit Details", color = SoftIvory) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = ChampagneGold)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onEditClick()
                                    }
                                )
                            }
                            if (onMoveClick != null) {
                                DropdownMenuItem(
                                    text = { Text("Move to Channel", color = SoftIvory) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = ChampagneGold)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onMoveClick()
                                    }
                                )
                            }
                            if (onDeleteClick != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete File", color = MutedBurgundy) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MutedBurgundy
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (file.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = file.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedTaupe,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom action row: Download status and trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download count badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Downloads",
                        modifier = Modifier.size(14.dp),
                        tint = MutedTaupe
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${file.downloadCount} transfers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedTaupe
                    )
                }

                // Action button: Download, Progress, or Open
                when {
                    isDownloading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(18.dp),
                                color = ChampagneGold,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold
                            )
                        }
                    }
                    isComplete -> {
                        OutlinedButton(
                            onClick = onOpenClick,
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("file_open_btn_${file.id}"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MutedSage)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MutedSage
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open", style = MaterialTheme.typography.labelMedium, color = MutedSage)
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onDownloadClick,
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("file_download_btn_${file.id}"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ChampagneGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download", style = MaterialTheme.typography.labelMedium, color = ChampagneGold)
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ChampagneGold,
                    trackColor = ElevatedSurface
                )
            }
        }
    }

    // Confirmation Dialog for Admin File Deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
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
                    text = "Are you sure you want to permanently delete \"${file.name}\"? This action will remove the cloud file from storage and database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTaupe
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteClick?.invoke()
                    }
                ) {
                    Text("Delete", color = MutedBurgundy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = MutedTaupe)
                }
            },
            containerColor = ElevatedSurface
        )
    }
}
