package com.kidslanguage.companion.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun SessionCompleteScreen(
    promptPlayer: PromptPlayer? = null,
    onPlayAgain: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        promptPlayer?.playVoiceAsset(PaoPaoVoices.CELEBRATE)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(AppSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MascotView(
                state = MascotState.Great,
                size = 320.dp
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(
                text = "🎉 泡泡太棒啦！练习完成！ 🎉",
                style = AppTypography.displayLarge,
                color = Primary600
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "泡泡今天表现超级棒，小兔子为你骄傲！",
                style = AppTypography.titleMedium,
                color = Neutral700
            )

            Spacer(modifier = Modifier.height(AppSpacing.xxxl))

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)
            ) {
                PrimaryLargeButton(
                    text = "再玩一次",
                    icon = Icons.Default.Refresh,
                    onClick = onPlayAgain,
                    modifier = Modifier.width(240.dp)
                )

                SecondaryLargeButton(
                    text = "回主页",
                    icon = Icons.Default.Home,
                    onClick = onBackHome,
                    modifier = Modifier.width(200.dp)
                )
            }
        }
    }
}
