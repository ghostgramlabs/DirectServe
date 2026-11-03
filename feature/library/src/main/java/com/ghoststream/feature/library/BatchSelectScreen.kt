package com.ghoststream.feature.library

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.directserve.core.resources.R
import com.ghostgramlabs.directserve.core.resources.ui.GhostSpacing
import com.ghostgramlabs.directserve.core.resources.ui.GhostUi
import com.ghostgramlabs.directserve.core.resources.util.LocalizationUtils
import com.ghoststream.core.model.SmartSelectionGroup

/**
 * "Use Phone Library": tick the groups you want (or Select all) and add them in one go.
 */
@Composable
fun BatchSelectScreen(
    groups: List<SmartSelectionGroup>,
    onBack: () -> Unit,
    onAddGroup: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
) {
    val selectedIds = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf<String>() }
    val allSelected = groups.isNotEmpty() && groups.all { it.id in selectedIds }
    val selectedGroups = groups.filter { it.id in selectedIds }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                stringResource(R.string.selection_smart_picks_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.common_refresh))
                }
            }
            if (groups.isNotEmpty()) {
                TextButton(onClick = {
                    if (allSelected) selectedIds.clear() else {
                        selectedIds.clear()
                        selectedIds.addAll(groups.map { it.id })
                    }
                }) {
                    Text(stringResource(if (allSelected) R.string.web_btn_clear_selection else R.string.web_btn_select_all))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = GhostSpacing.screenHorizontal,
                end = GhostSpacing.screenHorizontal,
                top = 4.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.selection_smart_picks_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (groups.isEmpty()) {
                item {
                    LibraryEmptyState(
                        title = stringResource(R.string.selection_no_groups_title),
                        description = stringResource(R.string.selection_no_groups_body),
                    )
                }
            } else {
                items(groups, key = { it.id }) { group ->
                    val checked = group.id in selectedIds
                    Surface(
                        shape = GhostUi.cardShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (checked) 2.dp else 1.dp,
                            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { on -> if (on) selectedIds.add(group.id) else selectedIds.remove(group.id) },
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    group.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    pluralStringResource(R.plurals.home_saved_shares_items_count, group.itemCount, group.itemCount) +
                                        stringResource(R.string.common_separator_dot) +
                                        LocalizationUtils.formatBytes(LocalContext.current, group.totalSizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }
        }

        if (groups.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GhostSpacing.screenHorizontal, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.web_selection_count, selectedGroups.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        onAddGroup(selectedGroups.flatMap { it.uris }.distinct().map(Uri::parse))
                        onBack()
                    },
                    enabled = selectedGroups.isNotEmpty(),
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = GhostUi.buttonShape,
                ) {
                    Text(stringResource(R.string.selection_add_selected))
                }
            }
        }
    }
}
