package com.ghoststream.feature.history

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.directserve.core.resources.R
import com.ghostgramlabs.directserve.core.resources.ui.GhostSpacing
import com.ghostgramlabs.directserve.core.resources.util.LocalizationUtils
import com.ghoststream.core.model.TransferDirection
import com.ghoststream.core.model.TransferRecord
import com.ghoststream.core.model.formatHistoryPeer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Received and sent files, one compact row each, with Open (received) and Delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<TransferRecord>,
    onBack: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClearSentHistory: () -> Unit,
    onClearReceivedHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val showingReceived = selectedTab == 0
    val filteredHistory = history.filter {
        it.direction == if (showingReceived) TransferDirection.RECEIVED else TransferDirection.SENT
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (showingReceived) onClearReceivedHistory() else onClearSentHistory()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                stringResource(R.string.home_tool_history),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (filteredHistory.isNotEmpty()) {
                IconButton(onClick = { showClearConfirmation = true }) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = stringResource(R.string.history_clear_title),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GhostSpacing.screenHorizontal, vertical = 12.dp),
        ) {
            listOf(R.string.history_tab_received, R.string.history_sent).forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                ) {
                    Text(stringResource(label))
                }
            }
        }

        if (filteredHistory.isEmpty()) {
            EmptyHistoryState(
                message = stringResource(
                    if (showingReceived) R.string.history_empty_received else R.string.history_empty_sent,
                ),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = GhostSpacing.screenHorizontal,
                    end = GhostSpacing.screenHorizontal,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredHistory, key = { it.id }) { record ->
                    HistoryItem(
                        record = record,
                        onDelete = { onDeleteRecord(record.id) },
                        onOpen = { record.fileUri?.let(onOpenFile) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    record: TransferRecord,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    val dateStr = remember(record.timestampMs) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(record.timestampMs))
    }
    val timeStr = remember(record.timestampMs) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(record.timestampMs))
    }
    val received = record.direction == TransferDirection.RECEIVED
    val canOpen = received && record.fileUri != null

    Surface(
        onClick = onOpen,
        enabled = canOpen,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 64.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (received) Icons.Outlined.Download else Icons.Outlined.Upload,
                    contentDescription = stringResource(if (received) R.string.history_received else R.string.history_sent),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(record.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOf(
                        LocalizationUtils.formatBytes(LocalContext.current, record.sizeBytes),
                        formatHistoryPeer(record.peer),
                        stringResource(R.string.history_date_time_format, dateStr, timeStr),
                    ).joinToString(stringResource(R.string.common_separator_dot)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (canOpen) {
                IconButton(onClick = onOpen) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.history_open),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.history_delete_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
