package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelEntity
import com.example.ui.components.ChannelCard
import com.example.ui.components.LuxuryEmptyState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftIvory

@Composable
fun ChannelsScreen(
    channels: List<ChannelEntity>,
    isAdmin: Boolean,
    onChannelClick: (String) -> Unit,
    onCreateChannelClick: () -> Unit,
    onEditChannelClick: (ChannelEntity) -> Unit,
    onDeleteChannelClick: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Reports", "Software", "Design", "Finance", "Engineering", "Media")

    val filteredChannels = remember(channels, selectedCategory) {
        if (selectedCategory == "All") channels
        else channels.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onCreateChannelClick,
                    containerColor = ChampagneGold,
                    contentColor = ObsidianBlack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_create_channel")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Channel", modifier = Modifier.size(24.dp))
                }
            }
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("channels_screen")
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Vault Channels",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftIvory
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Browse and access encrypted document and software channels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTaupe
                )
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ChampagneGold.copy(alpha = 0.15f) else GraphiteCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) ChampagneGold else BorderSubtle
                        ),
                        modifier = Modifier.testTag("filter_cat_$cat")
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ChampagneGold else MutedTaupe,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Channels List or Empty State
            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    LuxuryEmptyState(
                        title = if (channels.isEmpty()) "No Channels Created" else "No Channels in \"$selectedCategory\"",
                        subtitle = if (channels.isEmpty()) "Your private vault currently has no active channels. Use the Admin Console to initialize your first distribution channel."
                                   else "Try selecting 'All' or a different category to view channels.",
                        icon = Icons.Default.Folder,
                        actionLabel = if (isAdmin && channels.isEmpty()) "Create First Channel" else null,
                        onActionClick = if (isAdmin && channels.isEmpty()) onCreateChannelClick else null
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isAdmin = isAdmin,
                            onClick = { onChannelClick(channel.id) },
                            onEditClick = if (isAdmin) { { onEditChannelClick(channel) } } else null,
                            onDeleteClick = if (isAdmin) { { onDeleteChannelClick(channel.id) } } else null
                        )
                    }
                }
            }
        }
    }
}
