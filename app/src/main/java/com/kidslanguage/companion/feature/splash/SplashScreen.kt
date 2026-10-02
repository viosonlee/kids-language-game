package com.kidslanguage.companion.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Primary800
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MascotView(
                state = MascotState.Idle,
                size = 320.dp,
                animate = true
            )
            Spacer(modifier = Modifier.height(AppSpacing.xl))
            Text(
                text = "Kids Language Companion",
                style = AppTypography.displayLarge,
                color = Primary800
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "听懂 · 开口 · 快乐学语言",
                style = AppTypography.titleMedium
            )
        }
    }
}
