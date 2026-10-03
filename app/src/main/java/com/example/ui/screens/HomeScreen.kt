package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.FileCard
import com.example.ui.components.FileFormatUtils
import com.example.ui.components.LuxuryEmptyState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SoftIvory
import com.example.ui.viewmodel.DownloadProgressState
import com.example.ui.viewmodel.Screen
import java.util.Calendar

@Composable
fun HomeScreen(
    channels: List<ChannelEntity>,
    featuredChannels: List<ChannelEntity>,
    recentFiles: List<FileEntity>,
    popularFiles: List<FileEntity>,
    downloadsState: Map<String, DownloadProgressState>,
    isAdmin: Boolean,
    onChannelClick: (String) -> Unit,
    onFileClick: (FileEntity) -> Unit,
    onDownloadFile: (FileEntity) -> Unit,
    onOpenFile: (FileEntity) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Header & Personalized Greeting
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTaupe
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SQPlus Cloud Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SoftIvory
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ElevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "Private Edition",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ChampagneGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Global Search Bar Trigger
                Surface(
                    onClick = { onNavigate(Screen.SEARCH) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar_trigger"),
                    shape = RoundedCornerShape(16.dp),
                    color = GraphiteCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ChampagneGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search vault channels, documents, or binaries...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedTaupe
                        )
                    }
                }
            }
        }

        // Quick Metrics Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.Folder,
                    value = "${channels.size}",
                    label = "Channels",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.InsertDriveFile,
                    value = "${channels.sumOf { it.fileCount }}",
                    label = "Vault Files",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.CloudDownload,
                    value = "${recentFiles.sumOf { it.downloadCount }}",
                    label = "Transfers",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Empty Library State or Content Sections
        if (channels.isEmpty() && recentFiles.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                    LuxuryEmptyState(
                        title = "Vault Library is Pristine",
                        subtitle = "No distribution channels or files have been uploaded yet. As the administrator, access the console to create channels and deploy files.",
                        icon = Icons.Default.CloudQueue,
                        actionLabel = if (isAdmin) "Open Admin Console" else null,
                        onActionClick = if (isAdmin) { { onNavigate(Screen.ADMIN_DASHBOARD) } } else null
                    )
                }
            }
        } else {
            // Featured Channels Section
            if (channels.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    SectionHeader(
                        title = "Channels",
                        actionLabel = "View All",
                        onActionClick = { onNavigate(Screen.CHANNELS) },
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val displayChannels = if (featuredChannels.isNotEmpty()) featuredChannels else channels
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayChannels, key = { it.id }) { channel ->
                            FeaturedChannelCard(
                                channel = channel,
                                onClick = { onChannelClick(channel.id) }
                            )
                        }
                    }
                }
            }

            // Recently Added Files Section
            if (recentFiles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(22.dp))
                    SectionHeader(
                        title = "Recent Uploads",
                        actionLabel = "Explore",
                        onActionClick = { onNavigate(Screen.SEARCH) },
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(recentFiles.take(5), key = { it.id }) { file ->
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)) {
                        FileCard(
                            file = file,
                            downloadState = downloadsState[file.id],
                            isAdmin = false,
                            onClick = { onFileClick(file) },
                            onDownloadClick = { onDownloadFile(file) },
                            onOpenClick = { onOpenFile(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ChampagneGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftIvory
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedTaupe
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SoftIvory
        )

        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier.clickable(onClick = onActionClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ChampagneGold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturedChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FileFormatUtils.getChannelIcon(channel.iconName),
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SoftIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${channel.fileCount} files • ${channel.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MutedTaupe
            )
        }
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}
