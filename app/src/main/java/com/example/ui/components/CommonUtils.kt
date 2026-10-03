package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BadgeApk
import com.example.ui.theme.BadgeCode
import com.example.ui.theme.BadgeDoc
import com.example.ui.theme.BadgeMedia
import com.example.ui.theme.BadgePdf
import com.example.ui.theme.BadgeSpreadsheet
import com.example.ui.theme.BadgeZip
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.SoftIvory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileFormatUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
                sdf.format(Date(timestamp))
            }
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    fun getFileTypeColor(fileType: String): Color {
        return when (fileType.uppercase()) {
            "PDF" -> BadgePdf
            "ZIP", "RAR", "TAR", "7Z" -> BadgeZip
            "APK" -> BadgeApk
            "MP4", "MOV", "AVI", "MKV", "PNG", "JPG", "JPEG" -> BadgeMedia
            "DOC", "DOCX", "PPT", "PPTX", "TXT" -> BadgeDoc
            "XLS", "XLSX", "CSV" -> BadgeSpreadsheet
            "JSON", "XML", "BIN", "SH" -> BadgeCode
            else -> BadgeDoc
        }
    }

    fun getFileTypeIcon(fileType: String): ImageVector {
        return when (fileType.uppercase()) {
            "PDF" -> Icons.Default.PictureAsPdf
            "ZIP", "RAR", "TAR", "7Z" -> Icons.Default.Description
            "APK" -> Icons.Default.Android
            "MP4", "MOV", "AVI" -> Icons.Default.Movie
            "PNG", "JPG", "JPEG" -> Icons.Default.Image
            "DOC", "DOCX", "TXT" -> Icons.Default.Description
            "PPT", "PPTX" -> Icons.Default.Description
            "XLS", "XLSX", "CSV" -> Icons.Default.TableChart
            "JSON", "XML", "BIN" -> Icons.Default.Code
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    fun getChannelIcon(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "folder_special" -> Icons.Default.FolderSpecial
            "android" -> Icons.Default.Android
            "palette" -> Icons.Default.Palette
            "analytics" -> Icons.Default.Analytics
            "build" -> Icons.Default.Build
            "videocam" -> Icons.Default.Videocam
            else -> Icons.Default.Folder
        }
    }
}

@Composable
fun LuxuryEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.CloudQueue,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GraphiteCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SoftIvory,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedTaupe,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = Color(0xFF101113)
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
