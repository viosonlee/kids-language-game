package com.kidslanguage.companion.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.components.LanguageCard
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.components.debouncedClickable
import com.kidslanguage.companion.ui.components.PrimaryLargeButton
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral0
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartPractice: (LanguageCode) -> Unit,
    onOpenParentMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val selectedLanguage = settings.selectedLanguage
    val tapScale = remember { Animatable(1f) }
    val tapScope = rememberCoroutineScope()
    val tapInteraction = remember { MutableInteractionSource() }
    val lastTap = remember { longArrayOf(-1000L) }
    LifecycleResumeEffect(viewModel) {
        viewModel.resumeHome()
        onPauseOrDispose { viewModel.pauseHome() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(AppSpacing.xl)
    ) {
        // Parent Gate Icon in Top Right
        IconButton(
            onClick = onOpenParentMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(64.dp)
                .clip(CircleShape)
                .background(Primary100)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Parent Settings",
                tint = Primary600,
                modifier = Modifier.size(36.dp)
            )
        }

        // Center Content: Left Mascot, Right Choices
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = AppSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left Column: Mascot & Friendly Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(0.42f)
            ) {
                MascotView(
                    state = MascotState.Idle,
                    size = 340.dp,
                    modifier = Modifier
                        .scale(tapScale.value)
                        .clickable(
                            interactionSource = tapInteraction,
                            indication = null,
                            onClickLabel = "听兔兔打招呼"
                        ) {
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastTap[0] >= 1000L) {
                                lastTap[0] = now
                                viewModel.playGreeting()
                                tapScope.launch {
                                    tapScale.animateTo(0.96f, tween(100))
                                    tapScale.animateTo(1f, tween(180))
                                }
                            }
                        }
                )
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text(
                    text = "Momo Bunny 🐰",
                    style = AppTypography.titleLarge,
                    color = Primary600
                )
                Text(
                    text = "点我听兔兔跟泡泡打招呼哦~",
                    style = AppTypography.bodyMedium,
                    color = Neutral700
                )
            }

            // Right Column: Language Selection + Start Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(0.58f)
            ) {
                Text(
                    text = "选择要练习的语言",
                    style = AppTypography.titleLarge,
                    color = Neutral900
                )
                Spacer(modifier = Modifier.height(AppSpacing.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LanguageCode.entries.forEach { lang ->
                        LanguageCard(
                            language = lang,
                            isSelected = lang == selectedLanguage,
                            onClick = { viewModel.selectLanguage(lang) },
                            modifier = Modifier.width(170.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.xxxl))

                PrimaryLargeButton(
                    text = "开始练习",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onStartPractice(selectedLanguage) },
                    modifier = Modifier.fillMaxWidth(0.75f)
                )
            }
        }
    }
}
