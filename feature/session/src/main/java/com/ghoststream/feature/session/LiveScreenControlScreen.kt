package com.ghoststream.feature.session

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.directserve.core.resources.R
import com.ghostgramlabs.directserve.core.resources.ui.GhostCard
import com.ghostgramlabs.directserve.core.resources.ui.GhostSpacing
import com.ghostgramlabs.directserve.core.resources.ui.GhostUi
import com.ghostgramlabs.directserve.core.resources.ui.ghostStatusColors
import com.ghoststream.core.model.LiveAudioStatus
import com.ghoststream.core.model.LiveScreenSessionState
import com.ghoststream.core.model.LiveScreenStatus

/**
 * Mirror screen: one big Start button while stopped; while live it mirrors the sharing screen
 * (status bar with Stop, QR code and address, PIN switch) so both flows feel the same.
 */
@Composable
fun LiveScreenControlScreen(
    state: LiveScreenSessionState,
    pinProtectionEnabled: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCopyLink: () -> Unit,
    onTogglePinProtection: (Boolean) -> Unit,
    onRegeneratePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (state.isActive) {
            LiveStatusBar(state = state, onBack = onBack, onStop = onStop)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Text(
                    stringResource(R.string.home_tool_mirror),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

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
            if (!state.isActive) {
                item { StartCard(state = state, onStart = onStart) }
            } else {
                item {
                    ScanCard(
                        accessUrl = state.displayUrl,
                        onCopyLink = onCopyLink,
                        onShareLink = null,
                        stepNumber = null,
                    )
                }
                item {
                    PinSwitchCard(
                        enabled = pinProtectionEnabled,
                        pin = state.pin,
                        onTogglePinProtection = onTogglePinProtection,
                        onRegeneratePin = onRegeneratePin,
                    )
                }
                item { StreamInfo(state = state) }
            }
        }
    }
}

@Composable
private fun LiveStatusBar(
    state: LiveScreenSessionState,
    onBack: () -> Unit,
    onStop: () -> Unit,
) {
    val status = ghostStatusColors()
    val live = state.status == LiveScreenStatus.LIVE
    val background = if (live) status.successContainer else MaterialTheme.colorScheme.surfaceVariant
    val onBackground = if (live) status.onSuccessContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = background, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = onBackground)
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (live) status.success else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_tool_mirror) + stringResource(R.string.common_separator_dot) +
                        stringResource(if (live) R.string.live_screen_status_live else R.string.live_screen_status_starting),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.viewerName?.takeIf { state.viewerCount > 0 && it.isNotBlank() }
                        ?: stringResource(R.string.session_waiting_connect),
                    style = MaterialTheme.typography.bodySmall,
                    color = onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onStop,
                shape = GhostUi.buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.live_screen_stop))
            }
        }
    }
}

@Composable
private fun StartCard(
    state: LiveScreenSessionState,
    onStart: () -> Unit,
) {
    GhostCard {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.ScreenShare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            stringResource(R.string.live_screen_intro),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.live_screen_audio_limit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.lastError?.takeIf { it.isNotBlank() }?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = GhostUi.buttonShape,
        ) {
            Icon(Icons.Outlined.ScreenShare, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.live_screen_start), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StreamInfo(state: LiveScreenSessionState) {
    val audio = when (state.audioStatus) {
        LiveAudioStatus.AUDIO_LIVE -> stringResource(R.string.live_screen_audio_available)
        LiveAudioStatus.AUDIO_SILENT, LiveAudioStatus.AUDIO_FAILED -> stringResource(R.string.live_screen_audio_blocked_or_silent)
        LiveAudioStatus.AUDIO_UNAVAILABLE -> stringResource(R.string.live_screen_audio_unavailable)
        LiveAudioStatus.AUDIO_SUPPORTED, LiveAudioStatus.AUDIO_INITIALIZING -> null
    }
    val video = if (state.width != null && state.height != null) {
        stringResource(R.string.live_screen_stats, state.width ?: 0, state.height ?: 0, state.fps, state.bitrateKbps)
    } else {
        null
    }
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOfNotNull(audio, video).forEach { line ->
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.live_screen_audio_limit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
