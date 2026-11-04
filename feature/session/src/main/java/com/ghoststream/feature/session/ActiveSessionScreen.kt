package com.ghoststream.feature.session

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgramlabs.directserve.core.resources.R
import com.ghostgramlabs.directserve.core.resources.ui.GhostCard
import com.ghostgramlabs.directserve.core.resources.ui.GhostSpacing
import com.ghostgramlabs.directserve.core.resources.ui.GhostStepHeader
import com.ghostgramlabs.directserve.core.resources.ui.GhostUi
import com.ghostgramlabs.directserve.core.resources.ui.ghostStatusColors
import com.ghostgramlabs.directserve.core.resources.ui.ghostSwitchColors
import com.ghostgramlabs.directserve.core.resources.util.LocalizationUtils
import com.ghoststream.core.model.BlockedClient
import com.ghoststream.core.model.ConnectedClient
import com.ghoststream.core.model.SessionState
import com.ghoststream.core.model.UploadRequest
import com.ghoststream.core.model.deviceIdentity
import com.ghoststream.core.model.displayDeviceName
import com.ghoststream.core.model.resolvedAccessUrl
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * The live sharing screen: one status bar with an always-visible Stop button, then the QR code
 * and address, the PIN switch and the list of connected devices.
 */
@Composable
fun ActiveSessionScreen(
    sessionState: SessionState,
    browserPrepTargetCount: Int,
    browserPrepQueuedCount: Int,
    browserPrepReadyCount: Int,
    browserPrepPendingCount: Int,
    browserPrepProgressPercent: Int,
    browserPrepActiveFileName: String?,
    browserPrepManuallyTriggered: Boolean,
    hapticOnDeviceConnect: Boolean,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onBack: () -> Unit,
    onTogglePinProtection: (Boolean) -> Unit,
    onPrepareBrowserFiles: () -> Unit,
    onStopBrowserFiles: () -> Unit,
    onStopSharing: () -> Unit,
    onBlockClient: (String) -> Unit,
    onUnblockClient: (String) -> Unit,
    onRegeneratePin: () -> Unit,
    onDisconnectAll: () -> Unit,
    pendingUploadRequest: UploadRequest? = null,
    onResolveUploadRequest: (String, Boolean) -> Unit = { _, _ -> },
    deviceNicknames: Map<String, String> = emptyMap(),
    onUpdateDeviceNickname: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val accessUrl = remember(sessionState.sessionUrl, sessionState.networkAvailability.localAddress, sessionState.serverPort) {
        sessionState.resolvedAccessUrl()
    }
    LaunchedEffect(sessionState.connectedClients.size, hapticOnDeviceConnect) {
        if (hapticOnDeviceConnect && sessionState.connectedClients.isNotEmpty()) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SessionStatusBar(sessionState = sessionState, onBack = onBack, onStopSharing = onStopSharing)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = GhostSpacing.screenHorizontal,
                end = GhostSpacing.screenHorizontal,
                top = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScanCard(accessUrl = accessUrl, onCopyLink = onCopyLink, onShareLink = onShareLink)
            }
            item {
                PinCard(
                    sessionState = sessionState,
                    onTogglePinProtection = onTogglePinProtection,
                    onRegeneratePin = onRegeneratePin,
                )
            }
            item {
                ConnectedDevicesCard(
                    sessionState = sessionState,
                    onBlockClient = onBlockClient,
                    onDisconnectAll = onDisconnectAll,
                    deviceNicknames = deviceNicknames,
                    onUpdateDeviceNickname = onUpdateDeviceNickname,
                )
            }
            if (sessionState.blockedClients.isNotEmpty()) {
                item {
                    BlockedDevicesCard(
                        blockedClients = sessionState.blockedClients,
                        onUnblockClient = onUnblockClient,
                    )
                }
            }
        }
    }

    pendingUploadRequest?.let { request ->
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
}

@Composable
private fun SessionStatusBar(
    sessionState: SessionState,
    onBack: () -> Unit,
    onStopSharing: () -> Unit,
) {
    val status = ghostStatusColors()
    val live = sessionState.isSharing
    val background = if (live) status.successContainer else MaterialTheme.colorScheme.surfaceVariant
    val onBackground = if (live) status.onSuccessContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = background, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = onBackground,
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (live) status.success else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (live) stringResource(R.string.home_sharing_on) else stringResource(R.string.session_state_preparing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onBackground,
                )
                val count = sessionState.connectedClients.size
                // Ticks only while this screen is visible, so the elapsed time stays current.
                val now by produceState(System.currentTimeMillis()) {
                    while (true) {
                        delay(1_000)
                        value = System.currentTimeMillis()
                    }
                }
                val elapsed = sessionState.transferStats.startedAtEpochMs?.let {
                    LocalizationUtils.formatDuration(LocalContext.current, now - it)
                }
                Text(
                    text = listOfNotNull(
                        if (count == 0) {
                            stringResource(R.string.session_waiting_connect)
                        } else {
                            pluralStringResource(R.plurals.session_devices_connected, count, count)
                        },
                        elapsed,
                    ).joinToString(stringResource(R.string.common_separator_dot)),
                    style = MaterialTheme.typography.bodySmall,
                    color = onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onStopSharing,
                shape = GhostUi.buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.session_stop_short))
            }
        }
    }
}

/** QR code plus the typed address; shared by the sharing and Live Screen screens. */
@Composable
internal fun ScanCard(
    accessUrl: String?,
    onCopyLink: () -> Unit,
    onShareLink: (() -> Unit)?,
    stepNumber: Int? = 3,
) {
    GhostCard {
        if (stepNumber != null) {
            GhostStepHeader(number = stepNumber, title = stringResource(R.string.session_scan_title))
        } else {
            Text(
                stringResource(R.string.session_scan_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Box(
                    modifier = Modifier.size(232.dp).padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    accessUrl?.let { url ->
                        val qrBitmap = remember(url) { generateQrBitmap(url) }
                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = stringResource(R.string.session_qr_content_desc),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } ?: Text(
                        text = stringResource(R.string.session_qr_preparing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.72f),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.session_or_type),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Browsers accept the address without the scheme, and it is easier to type.
                val address = accessUrl?.removePrefix("http://")?.trimEnd('/')
                if (address != null) {
                    // An address split over two lines is easy to mistype, so shrink it to fit one line.
                    val baseSize = MaterialTheme.typography.titleMedium.fontSize
                    var fontSize by remember(address) { mutableStateOf(baseSize) }
                    var fitted by remember(address) { mutableStateOf(false) }
                    Text(
                        text = address,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = fontSize,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).clipToBounds().drawWithContent { if (fitted) drawContent() },
                        maxLines = 1,
                        softWrap = false,
                        onTextLayout = { layout ->
                            if (layout.hasVisualOverflow && fontSize.value > 8f) {
                                fontSize = (fontSize.value * 0.92f).sp
                            } else {
                                fitted = true
                            }
                        },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.session_waiting_local_link),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(onClick = onCopyLink, shape = RoundedCornerShape(10.dp)) {
                    Text(stringResource(R.string.session_copy_short))
                }
                if (onShareLink != null) {
                    IconButton(onClick = onShareLink) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.session_share))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinCard(
    sessionState: SessionState,
    onTogglePinProtection: (Boolean) -> Unit,
    onRegeneratePin: () -> Unit,
) {
    PinSwitchCard(
        enabled = sessionState.authEnabled,
        pin = sessionState.pin,
        onTogglePinProtection = onTogglePinProtection,
        onRegeneratePin = onRegeneratePin,
    )
}

/** "Ask for a PIN" switch with the current PIN and a New PIN action when it is on. */
@Composable
internal fun PinSwitchCard(
    enabled: Boolean,
    pin: String?,
    onTogglePinProtection: (Boolean) -> Unit,
    onRegeneratePin: () -> Unit,
) {
    GhostCard(contentPadding = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = onTogglePinProtection,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.session_pin_switch),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.session_pin_switch_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = null, colors = ghostSwitchColors())
        }
        if (enabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.session_pin_value, pin ?: "----"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRegeneratePin) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.session_new_pin))
                }
            }
        }
    }
}

@Composable
private fun ConnectedDevicesCard(
    sessionState: SessionState,
    onBlockClient: (String) -> Unit,
    onDisconnectAll: () -> Unit,
    deviceNicknames: Map<String, String>,
    onUpdateDeviceNickname: (String, String) -> Unit,
) {
    GhostCard(contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.session_connected_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (sessionState.connectedClients.size > 1) {
                TextButton(onClick = onDisconnectAll) {
                    Text(stringResource(R.string.session_disconnect_all))
                }
            }
        }
        if (sessionState.connectedClients.isEmpty()) {
            Text(
                stringResource(R.string.session_no_devices),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            sessionState.connectedClients.forEach { client ->
                ConnectedClientRow(
                    client = client,
                    onBlockClient = onBlockClient,
                    deviceNicknames = deviceNicknames,
                    onUpdateDeviceNickname = onUpdateDeviceNickname,
                )
            }
            Text(
                stringResource(R.string.session_metric_sent) + ": " +
                    LocalizationUtils.formatBytes(LocalContext.current, sessionState.transferStats.totalBytesSent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectedClientRow(
    client: ConnectedClient,
    onBlockClient: (String) -> Unit,
    deviceNicknames: Map<String, String>,
    onUpdateDeviceNickname: (String, String) -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var newNickname by remember { mutableStateOf(deviceNicknames[client.ipAddress] ?: "") }
    val displayName = remember(client.ipAddress, deviceNicknames) {
        displayDeviceName(client.ipAddress, deviceNicknames)
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.session_rename_device)) },
            text = {
                OutlinedTextField(
                    value = newNickname,
                    onValueChange = { if (it.length <= 30) newNickname = it },
                    label = { Text(stringResource(R.string.session_nickname_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateDeviceNickname(client.ipAddress, newNickname.trim())
                    showRenameDialog = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 52.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOfNotNull(client.ipAddress, client.displayName?.takeIf { it.isNotBlank() })
                    .distinct()
                    .joinToString(stringResource(R.string.common_separator_dot)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { showRenameDialog = true }) {
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.session_rename_device))
        }
        IconButton(onClick = { onBlockClient(client.ipAddress) }) {
            Icon(
                Icons.Outlined.Block,
                contentDescription = stringResource(R.string.session_block_device),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun BlockedDevicesCard(
    blockedClients: List<BlockedClient>,
    onUnblockClient: (String) -> Unit,
) {
    GhostCard(contentPadding = 16.dp) {
        Text(
            stringResource(R.string.session_blocked_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        blockedClients.forEach { blocked ->
            val identity = deviceIdentity(blocked.ipAddress)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(identity.generatedName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        listOf(identity.ipAddress, blocked.note).joinToString(stringResource(R.string.common_separator_dot)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onUnblockClient(blocked.ipAddress) }) {
                    Text(stringResource(R.string.session_unblock))
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String): Bitmap? {
    return runCatching {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 720, 720)
        Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.WHITE,
                    )
                }
            }
        }
    }.getOrNull()
}
