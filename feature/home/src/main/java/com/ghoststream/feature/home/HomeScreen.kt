package com.ghoststream.feature.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.remember
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.directserve.core.resources.R
import com.ghostgramlabs.directserve.core.resources.ui.GhostCard
import com.ghostgramlabs.directserve.core.resources.ui.GhostSectionLabel
import com.ghostgramlabs.directserve.core.resources.ui.GhostSpacing
import com.ghostgramlabs.directserve.core.resources.ui.GhostStepHeader
import com.ghostgramlabs.directserve.core.resources.ui.GhostUi
import com.ghostgramlabs.directserve.core.resources.ui.ghostStatusColors
import com.ghostgramlabs.directserve.core.resources.util.LocalizationUtils
import com.ghoststream.core.model.AppSettings
import com.ghoststream.core.model.ConnectionDiagnostics
import com.ghoststream.core.model.LibraryState
import com.ghoststream.core.model.NearbyDevice
import com.ghoststream.core.model.NearbyDiscoveryState
import com.ghoststream.core.model.NetworkType
import com.ghoststream.core.model.RecentSession
import com.ghoststream.core.model.SessionState
import com.ghoststream.core.model.UploadRequest
import com.ghoststream.core.model.deviceIdentity

/**
 * Home is the three-step flow: choose what to share, start sharing, then (on the session
 * screen) scan the QR code. Everything else sits in the small "More" grid.
 */
@Composable
fun HomeScreen(
    hasAllFilesAccess: Boolean,
    libraryState: LibraryState,
    sessionState: SessionState,
    settings: AppSettings,
    recentSessions: List<RecentSession>,
    connectionDiagnostics: ConnectionDiagnostics,
    nearbyDiscoveryState: NearbyDiscoveryState,
    connectingNearbyDeviceId: String?,
    pendingUploadRequest: UploadRequest?,
    isStartingShare: Boolean,
    isLiveScreenActive: Boolean,
    onStartSharing: () -> Unit,
    onRefreshConnection: () -> Unit,
    onRefreshNearby: () -> Unit,
    onOpenNearbyDevice: (NearbyDevice) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenLiveScreen: () -> Unit,
    onOpenQuickText: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDlna: () -> Unit,
    libraryImportingCount: Int,
    onImportAllMedia: () -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onOpenBatchSelect: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onResolveUploadRequest: (String, Boolean) -> Unit,
    onToggleShareVideos: (Boolean) -> Unit,
    onToggleShareMusic: (Boolean) -> Unit,
    onToggleSharePhotos: (Boolean) -> Unit,
    onToggleShareFiles: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAllFilesDialog by rememberSaveable { mutableStateOf(false) }
    var showNoNetworkDialog by rememberSaveable { mutableStateOf(false) }

    val hasLibraryContent = libraryState.items.isNotEmpty() || libraryState.folders.isNotEmpty()
    val canStartSession = sessionState.networkAvailability.isWifiOrHotspotReady
    val addEverything = {
        if (hasAllFilesAccess) onImportAllMedia() else showAllFilesDialog = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = GhostSpacing.screenHorizontal,
            end = GhostSpacing.screenHorizontal,
            top = 8.dp,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHeader(onOpenSettings = onOpenSettings) }

        item {
            NetworkStatusCard(
                sessionState = sessionState,
                onOpenSession = onStartSharing,
                onRefreshConnection = onRefreshConnection,
            )
        }

        item {
            ChooseStepCard(
                libraryState = libraryState,
                settings = settings,
                hasLibraryContent = hasLibraryContent,
                libraryImportingCount = libraryImportingCount,
                enabled = !isLiveScreenActive,
                onAddEverything = addEverything,
                onPickFiles = onPickFiles,
                onPickFolder = onPickFolder,
                onOpenBatchSelect = onOpenBatchSelect,
                onOpenLibrary = onOpenLibrary,
                onToggleVideos = onToggleShareVideos,
                onToggleMusic = onToggleShareMusic,
                onTogglePhotos = onToggleSharePhotos,
                onToggleFiles = onToggleShareFiles,
            )
        }

        if (!sessionState.isSharing) {
            item {
                StartStepCard(
                    isStartingShare = isStartingShare,
                    enabled = !isStartingShare && !isLiveScreenActive,
                    onStart = {
                        if (canStartSession) onStartSharing() else showNoNetworkDialog = true
                    },
                )
            }
        }

        item {
            MoreTools(
                quickTextEnabled = settings.quickTextEnabled,
                isLiveScreenActive = isLiveScreenActive,
                dlnaEnabled = settings.dlnaEnabled,
                onOpenLiveScreen = onOpenLiveScreen,
                onOpenQuickText = onOpenQuickText,
                onOpenHistory = onOpenHistory,
                onOpenDlna = onOpenDlna,
            )
        }

        // Only shown when another phone running DirectServe is actually on the network;
        // the empty-state explainer confused people who just wanted the browser link.
        if (nearbyDiscoveryState.devices.isNotEmpty()) {
            item {
                NearbyDevicesSection(
                    devices = nearbyDiscoveryState.devices,
                    connectingNearbyDeviceId = connectingNearbyDeviceId,
                    onRefreshNearby = onRefreshNearby,
                    onOpenNearbyDevice = onOpenNearbyDevice,
                )
            }
        }
    }

    if (showAllFilesDialog) {
        AlertDialog(
            onDismissRequest = { showAllFilesDialog = false },
            icon = { Icon(Icons.Outlined.Collections, contentDescription = null) },
            title = { Text(stringResource(R.string.home_all_files_access_title)) },
            text = { Text(stringResource(R.string.home_all_files_access_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showAllFilesDialog = false
                        onRequestAllFilesAccess()
                    },
                    shape = GhostUi.buttonShape,
                ) {
                    Text(stringResource(R.string.home_all_files_access_grant))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAllFilesDialog = false
                        onImportAllMedia()
                    },
                ) {
                    Text(stringResource(R.string.home_all_files_access_media_only))
                }
            },
        )
    }

    if (showNoNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showNoNetworkDialog = false },
            icon = { Icon(Icons.Outlined.NetworkCheck, contentDescription = null) },
            title = { Text(stringResource(R.string.home_network_required_title)) },
            text = { Text(stringResource(R.string.home_network_required_body)) },
            confirmButton = {
                TextButton(onClick = { showNoNetworkDialog = false }) {
                    Text(stringResource(R.string.common_dismiss))
                }
            },
        )
    }

    pendingUploadRequest?.let { request ->
        UploadRequestDialog(request = request, onResolveUploadRequest = onResolveUploadRequest)
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_brand_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.home_brand_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            onClick = onOpenSettings,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.home_settings_content_desc),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(
    sessionState: SessionState,
    onOpenSession: () -> Unit,
    onRefreshConnection: () -> Unit,
) {
    val status = ghostStatusColors()
    val network = sessionState.networkAvailability
    when {
        sessionState.isSharing -> Surface(
            onClick = onOpenSession,
            shape = GhostUi.cardShape,
            color = status.successContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.size(12.dp).background(status.success, CircleShape))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_sharing_on),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = status.onSuccessContainer,
                    )
                    Text(
                        connectedLabel(sessionState.connectedClients.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = status.onSuccessContainer,
                    )
                }
                Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = status.onSuccessContainer)
                Text(
                    stringResource(R.string.home_show_qr),
                    style = MaterialTheme.typography.labelLarge,
                    color = status.onSuccessContainer,
                )
            }
        }

        network.isWifiOrHotspotReady -> GhostCard(contentPadding = 14.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Wifi, contentDescription = null, tint = status.success)
                Text(
                    text = stringResource(
                        if (network.type == NetworkType.HOTSPOT) R.string.home_network_hotspot else R.string.home_network_wifi,
                    ) + stringResource(R.string.common_separator_dot) + stringResource(R.string.home_ready_to_share),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        else -> GhostCard(contentPadding = 14.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_network_none),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.home_network_none_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRefreshConnection) {
                    Text(stringResource(R.string.common_refresh))
                }
            }
        }
    }
}

/** "+ Add" with every way to add in one menu, so no option hides behind another screen. */
@Composable
private fun AddContentButton(
    enabled: Boolean,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onOpenBatchSelect: () -> Unit,
    onAddEverything: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = GhostUi.buttonShape,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.home_add), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_add_files)) },
                leadingIcon = { Icon(Icons.Outlined.NoteAdd, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPickFiles()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_add_whole_folder)) },
                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPickFolder()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_add_from_suggestions)) },
                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenBatchSelect()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_add_everything)) },
                leadingIcon = { Icon(Icons.Outlined.Collections, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddEverything()
                },
            )
        }
    }
}

@Composable
private fun ChooseStepCard(
    libraryState: LibraryState,
    settings: AppSettings,
    hasLibraryContent: Boolean,
    libraryImportingCount: Int,
    enabled: Boolean,
    onAddEverything: () -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onOpenBatchSelect: () -> Unit,
    onOpenLibrary: () -> Unit,
    onToggleVideos: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onTogglePhotos: (Boolean) -> Unit,
    onToggleFiles: (Boolean) -> Unit,
) {
    GhostCard(modifier = Modifier.alpha(if (enabled) 1f else 0.5f)) {
        GhostStepHeader(number = 1, title = stringResource(R.string.home_step_choose))

        if (libraryImportingCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.home_adding),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (!hasLibraryContent) {
            Text(
                stringResource(R.string.home_step_choose_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAddEverything,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = GhostUi.buttonShape,
            ) {
                Text(stringResource(R.string.home_add_everything), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // The system pickers open straight from here, and whatever is picked is added at once.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickFiles,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = GhostUi.buttonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Outlined.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.library_add_files), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onPickFolder,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = GhostUi.buttonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.library_add_whole_folder), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeTile(
                    icon = Icons.Outlined.PlayCircle,
                    label = stringResource(R.string.home_content_filter_videos),
                    count = libraryState.summary.videos,
                    selected = settings.shareVideos,
                    enabled = enabled,
                    onToggle = onToggleVideos,
                    modifier = Modifier.weight(1f),
                )
                TypeTile(
                    icon = Icons.Outlined.Image,
                    label = stringResource(R.string.home_content_filter_photos),
                    count = libraryState.summary.photos,
                    selected = settings.sharePhotos,
                    enabled = enabled,
                    onToggle = onTogglePhotos,
                    modifier = Modifier.weight(1f),
                )
                TypeTile(
                    icon = Icons.Outlined.LibraryMusic,
                    label = stringResource(R.string.home_content_filter_music),
                    count = libraryState.summary.music,
                    selected = settings.shareMusic,
                    enabled = enabled,
                    onToggle = onToggleMusic,
                    modifier = Modifier.weight(1f),
                )
                TypeTile(
                    icon = Icons.Outlined.Description,
                    label = stringResource(R.string.home_content_filter_files),
                    count = libraryState.summary.files,
                    selected = settings.shareFiles,
                    enabled = enabled,
                    onToggle = onToggleFiles,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = pluralStringResource(
                    R.plurals.home_saved_shares_items_count,
                    libraryState.summary.totalItems,
                    libraryState.summary.totalItems,
                ) + stringResource(R.string.common_separator_dot) +
                    LocalizationUtils.formatBytes(LocalContext.current, libraryState.summary.totalBytes) +
                    stringResource(R.string.common_separator_dot) + stringResource(R.string.home_types_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenLibrary,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = GhostUi.buttonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Text(stringResource(R.string.home_change_selection), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                AddContentButton(
                    enabled = enabled,
                    onPickFiles = onPickFiles,
                    onPickFolder = onPickFolder,
                    onOpenBatchSelect = onOpenBatchSelect,
                    onAddEverything = onAddEverything,
                )
            }
        }
    }
}

@Composable
private fun TypeTile(
    icon: ImageVector,
    label: String,
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val content = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .heightIn(min = 92.dp)
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = onToggle),
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, border),
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun StartStepCard(
    isStartingShare: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
) {
    GhostCard {
        GhostStepHeader(number = 2, title = stringResource(R.string.home_step_start))
        Button(
            onClick = onStart,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
            shape = GhostUi.buttonShape,
        ) {
            if (isStartingShare) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.home_button_starting), style = MaterialTheme.typography.titleMedium)
            } else {
                Icon(Icons.Outlined.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.home_button_start_session), style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(
            stringResource(R.string.home_step_start_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MoreTools(
    quickTextEnabled: Boolean,
    isLiveScreenActive: Boolean,
    dlnaEnabled: Boolean,
    onOpenLiveScreen: () -> Unit,
    onOpenQuickText: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDlna: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostSectionLabel(stringResource(R.string.home_more))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolTile(
                icon = Icons.Outlined.ScreenShare,
                label = stringResource(R.string.home_tool_mirror),
                highlighted = isLiveScreenActive,
                onClick = onOpenLiveScreen,
                modifier = Modifier.weight(1f),
            )
            if (quickTextEnabled) {
                ToolTile(
                    icon = Icons.Outlined.Textsms,
                    label = stringResource(R.string.home_tool_send_text),
                    enabled = !isLiveScreenActive,
                    onClick = onOpenQuickText,
                    modifier = Modifier.weight(1f),
                )
            }
            ToolTile(
                icon = Icons.Outlined.History,
                label = stringResource(R.string.home_tool_history),
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f),
            )
            ToolTile(
                icon = Icons.Outlined.Tv,
                label = stringResource(R.string.home_tool_tv),
                highlighted = dlnaEnabled,
                onClick = onOpenDlna,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 84.dp).alpha(if (enabled) 1f else 0.45f),
        shape = GhostUi.cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NearbyDevicesSection(
    devices: List<NearbyDevice>,
    connectingNearbyDeviceId: String?,
    onRefreshNearby: () -> Unit,
    onOpenNearbyDevice: (NearbyDevice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhostSectionLabel(stringResource(R.string.home_nearby_open_title), modifier = Modifier.weight(1f))
            TextButton(onClick = onRefreshNearby) { Text(stringResource(R.string.common_refresh)) }
        }
        GhostCard(contentPadding = 14.dp) {
            devices.take(3).forEach { device ->
                val identity = deviceIdentity(device.address)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            device.deviceLabel?.takeIf { it.isNotBlank() } ?: identity.nameWithIp,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (device.authRequired) {
                            Text(
                                stringResource(R.string.home_nearby_device_pin),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val connecting = connectingNearbyDeviceId == device.id
                    Button(
                        onClick = { onOpenNearbyDevice(device) },
                        enabled = !connecting,
                        shape = GhostUi.buttonShape,
                    ) {
                        if (connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.home_nearby_action_open))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadRequestDialog(
    request: UploadRequest,
    onResolveUploadRequest: (String, Boolean) -> Unit,
) {
    val requesterIdentity = deviceIdentity(request.requesterIp)
    AlertDialog(
        onDismissRequest = { onResolveUploadRequest(request.id, false) },
        icon = { Icon(Icons.Outlined.Collections, contentDescription = null) },
        title = { Text(stringResource(R.string.home_upload_request_title)) },
        text = {
            val fileText = if (request.fileCount > 1) {
                stringResource(R.string.home_upload_request_files, request.fileCount)
            } else {
                stringResource(R.string.home_upload_request_single)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_upload_request_body, requesterIdentity.nameWithIp, fileText))
                if (request.fileCount == 1) {
                    Text(
                        request.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    stringResource(
                        R.string.home_upload_request_total_size,
                        LocalizationUtils.formatBytes(LocalContext.current, request.sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onResolveUploadRequest(request.id, true) }, shape = GhostUi.buttonShape) {
                Text(stringResource(R.string.common_accept))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onResolveUploadRequest(request.id, false) }, shape = GhostUi.buttonShape) {
                Text(stringResource(R.string.common_decline))
            }
        },
    )
}

@Composable
private fun connectedLabel(count: Int): String =
    if (count == 0) {
        stringResource(R.string.session_waiting_connect)
    } else {
        pluralStringResource(R.plurals.session_devices_connected, count, count)
    }
