package com.kidslanguage.companion.feature.mode

import com.kidslanguage.companion.ui.theme.isCompactLandscape
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.core.model.PracticeMode
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.components.PrimaryLargeButton
import com.kidslanguage.companion.ui.components.SecondaryLargeButton
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun ModeSelectScreen(
    onModeSelected: (PracticeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    availableModes: List<PracticeMode>? = PracticeMode.entries,
) {
    val compact = isCompactLandscape()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(if (compact) 12.dp else AppSpacing.xl)
    ) {
        // Top Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(if (compact) 48.dp else 64.dp)
                .clip(CircleShape)
                .background(Primary100)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Primary600,
                modifier = Modifier.size(36.dp)
            )
        }

        // Center Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (compact) 52.dp else AppSpacing.xxl, end = if (compact) 12.dp else AppSpacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left Column: 2 Practice Mode options
            Column(
                modifier = Modifier.weight(0.55f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "选择练习玩法",
                    style = if (compact) AppTypography.titleMedium else AppTypography.displayLarge,
                    color = Neutral900
                )
                Spacer(modifier = Modifier.height(if (compact) 16.dp else AppSpacing.xxl))

                if (availableModes == null) {
                    androidx.compose.material3.CircularProgressIndicator()
                } else if (availableModes.isEmpty()) {
                    Text("这个场景还没有可用的练习，请返回选择其他场景。", color = Neutral900)
                }
                if (availableModes?.contains(PracticeMode.REPEAT) == true) PrimaryLargeButton(
                    text = "听一听，说一说",
                    icon = Icons.Default.Mic,
                    onClick = { onModeSelected(PracticeMode.REPEAT) },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    height = if (compact) 64.dp else 96.dp
                )

                Spacer(modifier = Modifier.height(if (compact) 12.dp else AppSpacing.xl))

                if (availableModes?.contains(PracticeMode.CHOOSE) == true) SecondaryLargeButton(
                    text = "听一听，选图片",
                    icon = Icons.Default.TouchApp,
                    onClick = { onModeSelected(PracticeMode.CHOOSE) },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    height = if (compact) 64.dp else 96.dp
                )
            }

            // Right Column: Mascot Praise
            Column(
                modifier = Modifier.weight(0.45f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MascotView(
                    state = MascotState.Praise,
                    size = if (compact) 180.dp else 320.dp
                )
            }
        }
    }
}
