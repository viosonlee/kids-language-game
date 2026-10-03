package com.kidslanguage.companion.feature.result

import com.kidslanguage.companion.ui.theme.isCompactLandscape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.kidslanguage.companion.core.audio.PaoPaoVoices
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.components.PrimaryLargeButton
import com.kidslanguage.companion.ui.components.SecondaryLargeButton
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Primary800

@Composable
fun SessionCompleteScreen(
    promptPlayer: PromptPlayer? = null,
    onPlayAgain: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = isCompactLandscape()
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(promptPlayer) {
        val playback = scope.launch { promptPlayer?.playVoiceAsset(PaoPaoVoices.CELEBRATE) }
        onPauseOrDispose {
            if (playback.isActive) {
                playback.cancel()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(if (compact) 12.dp else AppSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        val mascotSize = if (compact) 88.dp else (maxHeight * 0.38f).coerceAtMost(240.dp)
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MascotView(
                state = MascotState.Great,
                size = mascotSize
            )
            Spacer(modifier = Modifier.height(if (compact) 4.dp else AppSpacing.md))
            Text(
                text = "🎉 泡泡太棒啦！练习完成！ 🎉",
                style = if (compact) AppTypography.titleMedium else AppTypography.displayLarge,
                textAlign = TextAlign.Center,
                color = Primary800
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "泡泡今天表现超级棒，小兔子为你骄傲！",
                style = if (compact) AppTypography.labelMedium else AppTypography.titleMedium,
                textAlign = TextAlign.Center,
                color = Neutral700
            )

            Spacer(modifier = Modifier.height(if (compact) 8.dp else AppSpacing.lg))

            Row(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)
            ) {
                PrimaryLargeButton(
                    text = "再玩一次",
                    height = if (compact) 56.dp else 80.dp,
                    icon = Icons.Default.Refresh,
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1f)
                )

                SecondaryLargeButton(
                    text = "回主页",
                    height = if (compact) 56.dp else 72.dp,
                    icon = Icons.Default.Home,
                    onClick = onBackHome,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
