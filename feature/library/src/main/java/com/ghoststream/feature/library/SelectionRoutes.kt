package com.ghoststream.feature.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import com.ghostgramlabs.directserve.core.resources.util.LocalizationUtils
import com.ghostgramlabs.directserve.core.resources.R as SharedR
import com.ghostgramlabs.directserve.core.resources.R
import com.ghoststream.core.model.SmartSelectionGroup

@Composable
fun BatchSelectRoute(
    groups: List<SmartSelectionGroup>,
    isLoading: Boolean,
    hasMediaAccess: Boolean,
    onBack: () -> Unit,
    onRequestAccess: () -> Unit,
    onRefresh: () -> Unit,
    onAddGroup: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // With groups to show, use the multi-select list (tick groups or Select all, then Add selected).
    if (hasMediaAccess && !isLoading && groups.isNotEmpty()) {
        BatchSelectScreen(
            groups = groups,
            onBack = onBack,
            onAddGroup = onAddGroup,
            onRefresh = onRefresh,
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.selection_smart_picks_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.selection_smart_picks_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (hasMediaAccess) {
                    OutlinedButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(16.dp),
                        colors = selectionSecondaryButtonColors(),
                    ) {
                        Text(stringResource(R.string.common_refresh))
                    }
                }
            }
        }

        if (!hasMediaAccess) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = selectionPanelColor()),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(stringResource(R.string.selection_media_access_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.selection_media_access_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRequestAccess,
                            shape = RoundedCornerShape(16.dp),
                            colors = selectionPrimaryButtonColors(),
                        ) {
                            Text(stringResource(R.string.selection_grant_access))
                        }
                    }
                }
            }
        } else if (isLoading) {
            item {
                LibraryEmptyState(
                    title = stringResource(R.string.selection_loading_groups_title),
                    description = stringResource(R.string.selection_loading_groups_body),
                )
            }
        } else if (groups.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = selectionPanelColor()),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(stringResource(R.string.selection_no_groups_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.selection_no_groups_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(16.dp),
                            colors = selectionSecondaryButtonColors(),
                        ) {
                            Text(stringResource(R.string.selection_refresh_groups))
                        }
                    }
                }
            }
        } else {
            items(groups, key = { it.id }) { group ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = selectionPanelColor()),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(group.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(group.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Text(
                            pluralStringResource(SharedR.plurals.library_items_in_view, group.itemCount, group.itemCount) +
                                stringResource(SharedR.string.common_separator_pipe) +
                                LocalizationUtils.formatBytes(context, group.totalSizeBytes)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onAddGroup(group.uris.map(Uri::parse))
                                onBack()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = selectionPrimaryButtonColors(),
                        ) {
                            Text(stringResource(R.string.selection_add_group))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(18.dp)) }
    }
}

@Composable
private fun selectionPanelColor() = MaterialTheme.colorScheme.surface

@Composable
private fun selectionPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun selectionSecondaryButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    contentColor = MaterialTheme.colorScheme.primary,
)
