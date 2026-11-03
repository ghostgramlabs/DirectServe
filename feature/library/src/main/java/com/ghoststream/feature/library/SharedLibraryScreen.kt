package com.ghoststream.feature.library

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ghostgramlabs.directserve.core.resources.R
import com.ghostgramlabs.directserve.core.resources.ui.GhostSectionLabel
import com.ghostgramlabs.directserve.core.resources.ui.GhostSpacing
import com.ghostgramlabs.directserve.core.resources.ui.GhostUi
import com.ghostgramlabs.directserve.core.resources.util.LocalizationUtils
import com.ghoststream.core.media.CompatibilityJob
import com.ghoststream.core.media.CompatibilityStatus
import com.ghoststream.core.model.LibraryState
import com.ghoststream.core.model.MediaCategory
import com.ghoststream.core.model.PlaybackMode
import com.ghoststream.core.model.SharedFolder
import com.ghoststream.core.model.SharedItem

/**
 * "What you're sharing": search, filter, remove with one tap, and one Add button at the bottom.
 * Sorting and "remove all" live in the overflow menu so they don't compete with the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLibraryScreen(
    libraryState: LibraryState,
    compatibilityJobs: Map<String, CompatibilityJob>,
    libraryImportingCount: Int,
    showThumbnails: Boolean,
    onBack: () -> Unit,
    onPrepareItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onOpenAddFiles: () -> Unit,
    onOpenAddFolder: () -> Unit,
    onOpenBatchSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("all") }
    var sortOption by rememberSaveable { mutableStateOf("newest") }
    var overflowExpanded by remember { mutableStateOf(false) }
    var addMenuExpanded by remember { mutableStateOf(false) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }

    val hasLibraryContent = libraryState.items.isNotEmpty() || libraryState.folders.isNotEmpty()

    LaunchedEffect(hasLibraryContent) {
        if (!hasLibraryContent) showClearAllDialog = false
    }

    val categories = listOf("all", "videos", "photos", "music", "files")
    val filteredItems = libraryState.items
        .filter { item ->
            selectedCategory == "all" ||
                (selectedCategory == "videos" && item.category == MediaCategory.VIDEO) ||
                (selectedCategory == "photos" && item.category == MediaCategory.PHOTO) ||
                (selectedCategory == "music" && item.category == MediaCategory.MUSIC) ||
                (selectedCategory == "files" && item.category == MediaCategory.FILE)
        }
        .filter { item -> query.isBlank() || item.displayName.contains(query, ignoreCase = true) }
        .let { items ->
            when (sortOption) {
                "name" -> items.sortedBy { it.displayName.lowercase() }
                "size" -> items.sortedByDescending { it.sizeBytes }
                else -> items.sortedByDescending { it.dateAddedEpochMs }
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.library_title_simple),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    pluralStringResource(
                        R.plurals.home_saved_shares_items_count,
                        libraryState.summary.totalItems,
                        libraryState.summary.totalItems,
                    ) + stringResource(R.string.common_separator_dot) +
                        LocalizationUtils.formatBytes(LocalContext.current, libraryState.summary.totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = GhostSpacing.screenHorizontal,
                end = GhostSpacing.screenHorizontal,
                top = 8.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasLibraryContent) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(stringResource(R.string.common_search)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(categoryLabel(category)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                }
            }

            if (libraryImportingCount > 0) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            stringResource(R.string.library_importing_body, libraryImportingCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (!hasLibraryContent) {
                item {
                    LibraryEmptyState(
                        title = stringResource(R.string.library_empty_title),
                        description = stringResource(R.string.library_empty_body),
                    )
                }
            } else {
                if (libraryState.folders.isNotEmpty() && selectedCategory == "all" && query.isBlank()) {
                    item { GhostSectionLabel(stringResource(R.string.library_section_folders), modifier = Modifier.padding(top = 8.dp)) }
                    items(libraryState.folders, key = { "folder-" + it.id }) { folder ->
                        FolderRow(folder = folder, onRemoveFolder = onRemoveFolder)
                    }
                }

                item {
                    GhostSectionLabel(
                        stringResource(R.string.library_section_files) + stringResource(R.string.common_separator_dot) +
                            pluralStringResource(R.plurals.library_items_in_view, filteredItems.size, filteredItems.size),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (filteredItems.isEmpty()) {
                    item {
                        LibraryEmptyState(
                            title = stringResource(R.string.library_empty_filter_title),
                            description = stringResource(R.string.library_empty_filter_body),
                        )
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        LibraryItemRow(
                            item = item,
                            compatibilityJob = compatibilityJobs[item.id],
                            showThumbnails = showThumbnails,
                            onPrepareItem = onPrepareItem,
                            onRemoveItem = onRemoveItem,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GhostSpacing.screenHorizontal, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { addMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = GhostUi.buttonShape,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.library_add_files_or_folders), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_add_from_suggestions)) },
                        leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                        onClick = {
                            addMenuExpanded = false
                            onOpenBatchSelect()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_add_files)) },
                        leadingIcon = { Icon(Icons.Outlined.NoteAdd, contentDescription = null) },
                        onClick = {
                            addMenuExpanded = false
                            onOpenAddFiles()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_add_whole_folder)) },
                        leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            addMenuExpanded = false
                            onOpenAddFolder()
                        },
                    )
                }
            }
            if (hasLibraryContent) {
                Box {
                    OutlinedButton(
                        onClick = { overflowExpanded = true },
                        modifier = Modifier.size(52.dp),
                        shape = GhostUi.buttonShape,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.library_organize_title))
                    }
                    DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                        listOf("newest", "name", "size").forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.library_sort_prefix, sortOptionLabel(option)),
                                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = { Icon(Icons.Outlined.Sort, contentDescription = null) },
                                onClick = {
                                    sortOption = option
                                    overflowExpanded = false
                                },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.library_clear_all_action), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                overflowExpanded = false
                                showClearAllDialog = true
                            },
                        )
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.library_clear_all_title)) },
            text = { Text(stringResource(R.string.library_clear_all_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDialog = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.library_clear_all_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
internal fun LibraryEmptyState(title: String, description: String) {
    Column(
        modifier = Modifier.padding(horizontal = GhostSpacing.screenHorizontal).fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun FolderRow(
    folder: SharedFolder,
    onRemoveFolder: (String) -> Unit,
) {
    RowCard {
        Row(
            modifier = Modifier.heightIn(min = 60.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(folder.displayName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        R.string.library_folder_summary,
                        folder.fileCount,
                        LocalizationUtils.formatBytes(LocalContext.current, folder.totalSizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onRemoveFolder(folder.id) }) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.common_remove) + " " + folder.displayName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LibraryItemRow(
    item: SharedItem,
    compatibilityJob: CompatibilityJob?,
    showThumbnails: Boolean,
    onPrepareItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
) {
    val needsPreparation = item.category == MediaCategory.VIDEO && item.playbackDecision.mode != PlaybackMode.DIRECT
    RowCard {
        Column {
            Row(
                modifier = Modifier.heightIn(min = 64.dp).padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryItemVisual(item = item, showThumbnails = showThumbnails)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(item.displayName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = listOfNotNull(
                            itemTypeLabel(item.category),
                            item.durationMs?.let { LocalizationUtils.formatDurationTimestamp(LocalContext.current, it) },
                            LocalizationUtils.formatBytes(LocalContext.current, item.sizeBytes),
                            if (item.subtitleMatch != null) stringResource(R.string.library_subtitle) else null,
                            if (!item.isAvailable) stringResource(R.string.library_unavailable) else null,
                        ).joinToString(stringResource(R.string.common_separator_dot)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onRemoveItem(item.id) }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_remove) + " " + item.displayName,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (needsPreparation) {
                PreparationRow(item = item, compatibilityJob = compatibilityJob, onPrepareItem = onPrepareItem)
            }
        }
    }
}

/** Videos that can't play directly in every browser get a one-tap "Prepare" here (manual only). */
@Composable
private fun PreparationRow(
    item: SharedItem,
    compatibilityJob: CompatibilityJob?,
    onPrepareItem: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = compatibilityJob?.let { compatibilityStatusLabel(it) } ?: stringResource(R.string.session_browser_prep_manual_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        when (compatibilityJob?.status) {
            CompatibilityStatus.QUEUED,
            CompatibilityStatus.ANALYZING,
            CompatibilityStatus.PREPARING,
            CompatibilityStatus.FINALIZING,
            -> CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(18.dp), strokeWidth = 2.dp)

            CompatibilityStatus.PLAYABLE_NOW,
            CompatibilityStatus.READY,
            -> Text(
                stringResource(R.string.library_ready),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp),
            )

            else -> TextButton(onClick = { onPrepareItem(item.id) }, enabled = item.isAvailable) {
                Text(
                    if (compatibilityJob?.status == CompatibilityStatus.FAILED) {
                        stringResource(R.string.library_try_again)
                    } else {
                        stringResource(R.string.library_prepare)
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryItemVisual(
    item: SharedItem,
    showThumbnails: Boolean,
) {
    val shape = RoundedCornerShape(8.dp)
    // Coil has no video decoder in this app, so only photos get a real preview.
    if (showThumbnails && item.category == MediaCategory.PHOTO) {
        AsyncImage(
            model = Uri.parse(item.uri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (item.category) {
                    MediaCategory.VIDEO -> Icons.Outlined.Movie
                    MediaCategory.PHOTO -> Icons.Outlined.Photo
                    MediaCategory.MUSIC -> Icons.Outlined.MusicNote
                    MediaCategory.FILE -> Icons.AutoMirrored.Outlined.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun itemTypeLabel(category: MediaCategory): String {
    return when (category) {
        MediaCategory.VIDEO -> stringResource(R.string.library_type_video)
        MediaCategory.PHOTO -> stringResource(R.string.library_type_photo)
        MediaCategory.MUSIC -> stringResource(R.string.library_type_music)
        MediaCategory.FILE -> stringResource(R.string.library_type_file)
    }
}

@Composable
private fun compatibilityStatusLabel(job: CompatibilityJob): String {
    if (job.streamable && job.status != CompatibilityStatus.READY) {
        return stringResource(R.string.library_compat_ready_to_play, job.message)
    }
    val prefix = when (job.status) {
        CompatibilityStatus.IDLE -> stringResource(R.string.library_compat_not_prepared)
        CompatibilityStatus.QUEUED -> stringResource(R.string.library_compat_queued)
        CompatibilityStatus.ANALYZING -> stringResource(R.string.library_compat_preparing)
        CompatibilityStatus.PREPARING -> stringResource(R.string.library_compat_preparing)
        CompatibilityStatus.FINALIZING -> stringResource(R.string.library_compat_preparing)
        CompatibilityStatus.PLAYABLE_NOW -> stringResource(R.string.library_compat_ready)
        CompatibilityStatus.READY -> stringResource(R.string.library_compat_ready)
        CompatibilityStatus.FAILED -> stringResource(R.string.library_compat_unavailable)
        CompatibilityStatus.STALLED -> stringResource(R.string.library_compat_unavailable)
    }
    return stringResource(R.string.library_compat_message, prefix, job.message)
}

@Composable
private fun categoryLabel(category: String): String = when (category) {
    "videos" -> stringResource(R.string.library_category_videos)
    "photos" -> stringResource(R.string.library_category_photos)
    "music" -> stringResource(R.string.library_category_music)
    "files" -> stringResource(R.string.library_category_files)
    else -> stringResource(R.string.library_category_all)
}

@Composable
private fun sortOptionLabel(option: String): String = when (option) {
    "name" -> stringResource(R.string.library_sort_name)
    "size" -> stringResource(R.string.library_sort_size)
    else -> stringResource(R.string.library_sort_newest)
}
