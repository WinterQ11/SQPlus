package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.ui.components.ChannelCard
import com.example.ui.components.FileCard
import com.example.ui.components.LuxuryEmptyState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftIvory
import com.example.ui.viewmodel.DownloadProgressState
import com.example.ui.viewmodel.FileTypeFilter
import com.example.ui.viewmodel.SortOption

@Composable
fun SearchScreen(
    searchQuery: String,
    searchFilter: FileTypeFilter,
    searchSort: SortOption,
    matchingChannels: List<ChannelEntity>,
    searchResults: List<FileEntity>,
    downloadsState: Map<String, DownloadProgressState>,
    isAdmin: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterChange: (FileTypeFilter) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onChannelClick: (String) -> Unit,
    onFileClick: (FileEntity) -> Unit,
    onDownloadFile: (FileEntity) -> Unit,
    onOpenFile: (FileEntity) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("search_screen")
    ) {
        // Search Input and Sort Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by name, type, or tag...", color = MutedTaupe) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
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
                    .height(52.dp)
                    .testTag("global_search_input"),
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
                    modifier = Modifier.height(52.dp)
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
                                    color = if (searchSort == option) ChampagneGold else SoftIvory,
                                    fontWeight = if (searchSort == option) FontWeight.Bold else FontWeight.Normal
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

        // File Type Filter Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(FileTypeFilter.values()) { filter ->
                val isSelected = searchFilter == filter
                Surface(
                    onClick = { onFilterChange(filter) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) ChampagneGold.copy(alpha = 0.15f) else GraphiteCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ChampagneGold else BorderSubtle
                    )
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ChampagneGold else MutedTaupe,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Results or Empty State
        if (matchingChannels.isEmpty() && searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                LuxuryEmptyState(
                    title = if (searchQuery.isNotBlank()) "No Matching Assets" else "Vault Search Ready",
                    subtitle = if (searchQuery.isNotBlank()) "No channels or files matched your criteria \"$searchQuery\"."
                               else "Enter keywords or select a file type filter to query the private vault repository.",
                    icon = Icons.Default.Search
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (matchingChannels.isNotEmpty()) {
                    item {
                        Text(
                            text = "Matching Channels (${matchingChannels.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftIvory,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(matchingChannels, key = { "search_ch_${it.id}" }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isAdmin = false,
                            onClick = { onChannelClick(channel.id) }
                        )
                    }
                }

                if (searchResults.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Matching Files (${searchResults.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftIvory,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(searchResults, key = { "search_file_${it.id}" }) { file ->
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
