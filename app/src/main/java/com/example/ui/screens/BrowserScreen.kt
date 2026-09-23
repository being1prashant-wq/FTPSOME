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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FtpFileItem
import com.example.data.model.MediaCategory
import com.example.ui.components.TvButton
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvGoldTertiary
import com.example.ui.theme.TvSkySecondary
import com.example.ui.theme.TvStatusGreen
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextTertiary

enum class FileSortOrder(val label: String) {
    NAME_ASC("Name A-Z"),
    DATE_DESC("Newest First"),
    SIZE_DESC("Largest First")
}

@Composable
fun BrowserScreen(
    currentPath: String,
    files: List<FtpFileItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigateInto: (FtpFileItem) -> Unit,
    onNavigateBack: () -> Unit,
    onFileSelected: (FtpFileItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<MediaCategory?>(null) }
    var sortOrder by remember { mutableStateOf(FileSortOrder.NAME_ASC) }
    var isGridView by remember { mutableStateOf(true) }

    val filteredFiles = remember(files, selectedCategory, sortOrder) {
        val catFiltered = if (selectedCategory == null) {
            files
        } else {
            files.filter { it.isDirectory || it.category == selectedCategory }
        }

        when (sortOrder) {
            FileSortOrder.NAME_ASC -> catFiltered.sortedWith(
                compareByDescending<FtpFileItem> { it.isDirectory }.thenBy { it.name.lowercase() }
            )
            FileSortOrder.DATE_DESC -> catFiltered.sortedWith(
                compareByDescending<FtpFileItem> { it.isDirectory }.thenByDescending { it.lastModifiedTime }
            )
            FileSortOrder.SIZE_DESC -> catFiltered.sortedWith(
                compareByDescending<FtpFileItem> { it.isDirectory }.thenByDescending { it.sizeBytes }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Path Header & Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TvSurface, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Path Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (currentPath != "/") {
                    TvButton(
                        onClick = onNavigateBack,
                        isPrimary = false,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Parent folder", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Up", fontSize = 13.sp)
                    }
                }
                Text(
                    text = currentPath,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls: Category Filter, Sort, Grid/List Toggle, Refresh
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Filter Pills
                CategoryFilterPill(
                    label = "All",
                    isSelected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
                CategoryFilterPill(
                    label = "Videos",
                    isSelected = selectedCategory == MediaCategory.VIDEO,
                    onClick = { selectedCategory = MediaCategory.VIDEO }
                )
                CategoryFilterPill(
                    label = "Music",
                    isSelected = selectedCategory == MediaCategory.AUDIO,
                    onClick = { selectedCategory = MediaCategory.AUDIO }
                )
                CategoryFilterPill(
                    label = "Photos",
                    isSelected = selectedCategory == MediaCategory.IMAGE,
                    onClick = { selectedCategory = MediaCategory.IMAGE }
                )

                Spacer(Modifier.width(8.dp))

                // Sort toggle
                TvButton(
                    onClick = {
                        sortOrder = when (sortOrder) {
                            FileSortOrder.NAME_ASC -> FileSortOrder.DATE_DESC
                            FileSortOrder.DATE_DESC -> FileSortOrder.SIZE_DESC
                            FileSortOrder.SIZE_DESC -> FileSortOrder.NAME_ASC
                        }
                    },
                    isPrimary = false,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sortOrder.label, fontSize = 12.sp)
                }

                // Grid / List toggle
                TvButton(
                    onClick = { isGridView = !isGridView },
                    isPrimary = false,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                TvButton(
                    onClick = onRefresh,
                    isPrimary = false,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = TvCyanPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("Reading remote folder contents...", color = TvTextSecondary, fontSize = 14.sp)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
                        Spacer(Modifier.height(16.dp))
                        TvButton(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }
                filteredFiles.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = TvTextTertiary, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("This folder is empty", color = TvTextSecondary, fontSize = 15.sp)
                    }
                }
                isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFiles, key = { it.fullPath }) { item ->
                            FileGridCard(
                                item = item,
                                onClick = {
                                    if (item.isDirectory) {
                                        onNavigateInto(item)
                                    } else {
                                        onFileSelected(item)
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFiles, key = { it.fullPath }) { item ->
                            FileListCard(
                                item = item,
                                onClick = {
                                    if (item.isDirectory) {
                                        onNavigateInto(item)
                                    } else {
                                        onFileSelected(item)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TvButton(
        onClick = onClick,
        isPrimary = isSelected,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun FileGridCard(
    item: FtpFileItem,
    onClick: () -> Unit
) {
    val icon = getCategoryIcon(item.category)
    val iconColor = getCategoryColor(item.category)

    TvFocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (!item.isDirectory && item.extension.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(TvSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.extension.uppercase(),
                            color = iconColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.formattedSize,
                    color = TvTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FileListCard(
    item: FtpFileItem,
    onClick: () -> Unit
) {
    val icon = getCategoryIcon(item.category)
    val iconColor = getCategoryColor(item.category)

    TvFocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!item.isDirectory && item.extension.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(TvSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.extension.uppercase(),
                            color = iconColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = item.formattedSize,
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.width(70.dp)
                )
            }
        }
    }
}

private fun getCategoryIcon(category: MediaCategory): ImageVector {
    return when (category) {
        MediaCategory.DIRECTORY -> Icons.Default.Folder
        MediaCategory.VIDEO -> Icons.Default.Movie
        MediaCategory.AUDIO -> Icons.Default.Audiotrack
        MediaCategory.IMAGE -> Icons.Default.Image
        MediaCategory.SUBTITLE -> Icons.Default.Subtitles
        MediaCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}

private fun getCategoryColor(category: MediaCategory): Color {
    return when (category) {
        MediaCategory.DIRECTORY -> TvCyanPrimary
        MediaCategory.VIDEO -> TvGoldTertiary
        MediaCategory.AUDIO -> TvSkySecondary
        MediaCategory.IMAGE -> TvStatusGreen
        MediaCategory.SUBTITLE -> Color(0xFFC084FC)
        MediaCategory.OTHER -> Color.White.copy(alpha = 0.6f)
    }
}
