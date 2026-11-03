package com.ghoststream.feature.history

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.directserve.core.resources.R
import com.ghostgramlabs.directserve.core.resources.ui.GhostCard
import com.ghostgramlabs.directserve.core.resources.ui.GhostSectionLabel
import com.ghostgramlabs.directserve.core.resources.ui.GhostSpacing
import com.ghostgramlabs.directserve.core.resources.ui.GhostUi
import com.ghoststream.core.model.QuickTextDevice
import com.ghoststream.core.model.QuickTextMessage
import com.ghoststream.core.model.QuickTextTargetType

/** Send text: write a message, pick who gets it, send. Past messages below with copy/open/delete. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickTextScreen(
    messages: List<QuickTextMessage>,
    devices: List<QuickTextDevice>,
    onBack: () -> Unit,
    onSend: (text: String, targetType: QuickTextTargetType, targetId: String?, targetName: String?) -> Unit,
    onCopyMessage: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onClearAll: () -> Unit,
    onRefreshDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val targetDevices = remember(devices) { devices.filterNot { it.isHostPhone } }
    val broadcastLabel = stringResource(R.string.quick_text_broadcast)
    var selectedTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeTargetId = selectedTargetId
        ?.takeIf { id -> id == BROADCAST_ID || targetDevices.any { it.id == id } }
        ?: targetDevices.firstOrNull()?.id
        ?: BROADCAST_ID

    LaunchedEffect(targetDevices) {
        val currentTargetId = selectedTargetId
        if (currentTargetId == null || (currentTargetId != BROADCAST_ID && targetDevices.none { it.id == currentTargetId })) {
            selectedTargetId = targetDevices.firstOrNull()?.id ?: BROADCAST_ID
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = GhostSpacing.screenHorizontal, end = GhostSpacing.screenHorizontal, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Text(
                    text = stringResource(R.string.home_tool_send_text),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefreshDevices) {
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.common_refresh))
                }
            }
        }

        item {
            GhostCard {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 1000) draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text(stringResource(R.string.quick_text_placeholder)) },
                )
                Text(
                    text = stringResource(R.string.quick_text_target_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    targetDevices.forEach { device ->
                        FilterChip(
                            selected = activeTargetId == device.id,
                            onClick = { selectedTargetId = device.id },
                            label = { Text(device.name) },
                            colors = chipColors,
                        )
                    }
                    FilterChip(
                        selected = activeTargetId == BROADCAST_ID,
                        onClick = { selectedTargetId = BROADCAST_ID },
                        label = { Text(broadcastLabel) },
                        colors = chipColors,
                    )
                }
                Button(
                    onClick = {
                        val isBroadcast = activeTargetId == BROADCAST_ID
                        val target = targetDevices.firstOrNull { it.id == activeTargetId }
                        onSend(
                            draft,
                            if (isBroadcast) QuickTextTargetType.BROADCAST else QuickTextTargetType.DEVICE,
                            if (isBroadcast) null else target?.id,
                            if (isBroadcast) broadcastLabel else target?.name,
                        )
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = GhostUi.buttonShape,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.quick_text_send))
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                GhostSectionLabel(stringResource(R.string.quick_text_history_title), modifier = Modifier.weight(1f))
                if (messages.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(stringResource(R.string.quick_text_clear_all), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (messages.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.quick_text_empty),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(messages, key = { it.id }) { message ->
                QuickTextMessageCard(
                    message = message,
                    onCopy = { onCopyMessage(message.text) },
                    onOpenLink = { onOpenLink(message.text) },
                    onDelete = { onDeleteMessage(message.id) },
                )
            }
        }
    }
}

@Composable
private fun QuickTextMessageCard(
    message: QuickTextMessage,
    onCopy: () -> Unit,
    onOpenLink: () -> Unit,
    onDelete: () -> Unit,
) {
    val targetLabel = if (message.targetType == QuickTextTargetType.BROADCAST) {
        stringResource(R.string.quick_text_broadcast)
    } else {
        message.targetName.orEmpty()
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = listOf(message.senderName, targetLabel, DateFormat.format("MMM d, h:mm a", message.timestampMs).toString())
                        .filter { it.isNotBlank() }
                        .joinToString(stringResource(R.string.common_separator_dot)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (message.isLink) {
                IconButton(onClick = onOpenLink) {
                    Icon(Icons.Outlined.Link, contentDescription = stringResource(R.string.quick_text_open_link), tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.quick_text_copy))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val BROADCAST_ID = "__broadcast__"
