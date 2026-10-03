package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftIvory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminUploadDialog(
    channels: List<ChannelEntity>,
    preselectedChannelId: String? = null,
    editingFile: FileEntity? = null,
    onDismiss: () -> Unit,
    onUploadSuccess: (
        channelId: String,
        name: String,
        description: String,
        fileType: String,
        fileSizeBytes: Long,
        category: String,
        isPublished: Boolean
    ) -> Unit,
    onUploadRealFile: ((Uri, String, String, Boolean) -> Unit)? = null,
    onUploadMultipleRealFiles: ((List<Uri>, String, String, Boolean) -> Unit)? = null,
    onUpdateSuccess: ((FileEntity) -> Unit)? = null
) {
    var selectedChannelId by remember {
        mutableStateOf(
            editingFile?.channelId
                ?: preselectedChannelId
                ?: channels.firstOrNull()?.id
                ?: ""
        )
    }
    var channelDropdownExpanded by remember { mutableStateOf(false) }

    var fileName by remember { mutableStateOf(editingFile?.name ?: "") }
    var description by remember { mutableStateOf(editingFile?.description ?: "") }
    var fileType by remember { mutableStateOf(editingFile?.fileType ?: "PDF") }
    var category by remember { mutableStateOf(editingFile?.category ?: "Reports") }
    var isPublished by remember { mutableStateOf(editingFile?.isPublished ?: true) }
    var fileSizeMb by remember {
        mutableStateOf(
            if (editingFile != null) String.format("%.1f", editingFile.fileSizeBytes / (1024.0 * 1024.0))
            else "4.5"
        )
    }

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()
    val isEditMode = editingFile != null

    val fileTypes = listOf("PDF", "ZIP", "APK", "DOCX", "PPTX", "XLSX", "MP4", "TXT", "RAR")
    val categories = listOf("Reports", "Software", "Design", "Finance", "Engineering", "Media", "General")

    val singleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUris = listOf(uri)
            val path = uri.path ?: ""
            val guessedName = path.substringAfterLast('/').ifEmpty { "device_upload" }
            if (fileName.isBlank()) {
                fileName = guessedName
            }
            val ext = guessedName.substringAfterLast('.', "").uppercase()
            if (ext.isNotEmpty()) {
                fileType = ext
            }
        }
    }

    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            if (uris.size == 1) {
                val uri = uris.first()
                val path = uri.path ?: ""
                val guessedName = path.substringAfterLast('/').ifEmpty { "device_upload" }
                fileName = guessedName
                val ext = guessedName.substringAfterLast('.', "").uppercase()
                if (ext.isNotEmpty()) fileType = ext
            } else {
                fileName = "${uris.size} files queued for upload"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("admin_upload_dialog")
                .clip(RoundedCornerShape(24.dp)),
            color = ElevatedSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditMode) "Edit File Metadata" else "Cloud Upload Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SoftIvory
                        )
                        Text(
                            text = if (isEditMode) "Update cloud distribution details" else "Host documents securely on Supabase Storage & PostgreSQL",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneGold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("upload_dialog_close")
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedTaupe)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real Device File Pickers (Single or Multiple)
                if (!isEditMode && onUploadRealFile != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { singleFileLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("pick_device_file_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick File", color = ChampagneGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { multipleFilesLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("pick_multiple_files_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                        ) {
                            Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Multiple", color = ChampagneGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    if (selectedUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GraphiteCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedUris.size == 1) "Selected: $fileName" else "${selectedUris.size} files ready for upload",
                                    color = SoftIvory,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Channel Selector
                Text(
                    text = "Target Distribution Channel",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftIvory
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentChannel = channels.find { it.id == selectedChannelId }
                    Surface(
                        onClick = { channelDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = GraphiteCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_channel_selector")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentChannel?.name ?: "Select target channel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = SoftIvory
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ChampagneGold)
                        }
                    }

                    DropdownMenu(
                        expanded = channelDropdownExpanded,
                        onDismissRequest = { channelDropdownExpanded = false },
                        modifier = Modifier.background(ElevatedSurface)
                    ) {
                        channels.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text(ch.name, color = SoftIvory) },
                                onClick = {
                                    selectedChannelId = ch.id
                                    channelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File Name / Title
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Document Title / File Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_filename_input"),
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

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftIvory
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            onClick = { category = cat },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) ChampagneGold.copy(alpha = 0.18f) else GraphiteCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ChampagneGold else BorderSubtle
                            )
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) ChampagneGold else MutedTaupe,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Notes (optional)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_description_input"),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Publish Immediately Switch
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GraphiteCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPublished) ChampagneGold.copy(alpha = 0.5f) else BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isPublished) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (isPublished) ChampagneGold else MutedTaupe,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isPublished) "Publish Document Immediately" else "Save as Draft (Unpublished)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPublished) SoftIvory else MutedTaupe
                                )
                                Text(
                                    text = if (isPublished) "Document appears in real time on all users' screens" else "Visible only to admin in dashboard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPublished) ChampagneGold else MutedTaupe
                                )
                            }
                        }

                        Switch(
                            checked = isPublished,
                            onCheckedChange = { isPublished = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = ChampagneGold,
                                uncheckedThumbColor = MutedTaupe,
                                uncheckedTrackColor = ElevatedSurface
                            )
                        )
                    }
                }

                // Upload Progress Animation
                AnimatedVisibility(visible = isUploading) {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Uploading & verifying Supabase Storage...",
                                style = MaterialTheme.typography.labelMedium,
                                color = ChampagneGold
                            )
                            Text(
                                text = "${(uploadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ChampagneGold,
                            trackColor = GraphiteCard
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (selectedUris.size > 1 && onUploadMultipleRealFiles != null) {
                            onUploadMultipleRealFiles(selectedUris, selectedChannelId, category, isPublished)
                            onDismiss()
                            return@Button
                        }

                        if (selectedUris.size == 1 && onUploadRealFile != null) {
                            onUploadRealFile(selectedUris.first(), selectedChannelId, category, isPublished)
                            onDismiss()
                            return@Button
                        }

                        if (fileName.isBlank()) return@Button
                        if (isEditMode && editingFile != null) {
                            val parsedBytes = ((fileSizeMb.toDoubleOrNull() ?: 2.0) * 1024 * 1024).toLong()
                            onUpdateSuccess?.invoke(
                                editingFile.copy(
                                    channelId = selectedChannelId,
                                    name = fileName.trim(),
                                    description = description.trim(),
                                    fileType = fileType.uppercase().trim(),
                                    fileSizeBytes = parsedBytes,
                                    category = category,
                                    isPublished = isPublished
                                )
                            )
                            onDismiss()
                        } else {
                            isUploading = true
                            coroutineScope.launch {
                                for (step in 1..10) {
                                    delay(60L)
                                    uploadProgress = step / 10f
                                }
                                val parsedBytes = ((fileSizeMb.toDoubleOrNull() ?: 2.0) * 1024 * 1024).toLong()
                                onUploadSuccess(
                                    selectedChannelId,
                                    fileName.trim(),
                                    description.trim(),
                                    fileType.uppercase().trim(),
                                    parsedBytes,
                                    category,
                                    isPublished
                                )
                                delay(80L)
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("upload_submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Save Changes" else if (selectedUris.size > 1) "Upload & Publish ${selectedUris.size} Files" else if (isPublished) "Publish to Cloud Vault" else "Save as Draft",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
