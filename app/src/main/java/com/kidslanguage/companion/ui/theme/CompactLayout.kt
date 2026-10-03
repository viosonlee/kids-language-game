package com.kidslanguage.companion.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Short landscape windows need a layout with smaller decoration, not scaled density. */
@Composable
fun isCompactLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenHeightDp < 480
}
