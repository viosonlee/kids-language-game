package com.kidslanguage.companion.feature.repeat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.components.*
import com.kidslanguage.companion.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepeatRecoveryDialog(
    problem: RepeatProblem,
    onDismiss: () -> Unit,
    onBrowse: () -> Unit,
    onParent: () -> Unit,
    onRetry: (() -> Unit)?,
    onChoose: (() -> Unit)?,
    onReplay: (() -> Unit)?,
) {
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)) {
        Surface(shape = RoundedCornerShape(AppRadius.dialog), color = Neutral0) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(AppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MascotView(MascotState.Listen, size = 112.dp)
                    Icon(if (problem == RepeatProblem.NO_SPEECH) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (problem == RepeatProblem.NO_SPEECH) "再说一次" else "说话练习暂不可用",
                        tint = Primary600, modifier = Modifier.size(72.dp))
                }
                Text(problem.message, style = AppTypography.bodyMedium, color = Neutral900,
                    textAlign = TextAlign.Center)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    if (onRetry != null) SecondaryLargeButton(
                        text = "再试一次", onClick = onRetry, height = 88.dp,
                        icon = if (problem == RepeatProblem.MODEL) Icons.Default.Refresh else Icons.Default.Mic,
                    )
                    if (onChoose != null) PrimaryLargeButton(
                        text = "玩选图", onClick = onChoose, icon = Icons.Default.PhotoLibrary, height = 88.dp,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    if (onReplay != null) SpeakerButton(onReplay, size = 88.dp)
                    IconButton(onClick = onBrowse, modifier = Modifier.size(88.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "换个场景", tint = Primary600, modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = onParent, modifier = Modifier.size(88.dp)) {
                        Icon(Icons.Default.Settings, "请家长帮忙", tint = Primary600, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}
