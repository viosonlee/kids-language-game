package com.kidslanguage.companion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kidslanguage.companion.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class UiContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val light = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (light + 0.05f) / (dark + 0.05f)
    }

    @Test fun filledButtonGradientKeepsWhiteLabelsReadable() {
        assertTrue(contrast(Primary700, Neutral0) >= 4.5f)
        assertTrue(contrast(Primary800, Neutral0) >= 4.5f)
    }

    @Test fun lightAndDisabledButtonsKeepDarkLabelsReadable() {
        assertTrue(contrast(Primary800, Neutral0) >= 4.5f)
        assertTrue(contrast(Neutral700, Neutral300) >= 4.5f)
        assertTrue(contrast(Neutral900, AccentPurple) >= 4.5f)
    }

    @Test fun typographyDoesNotOverrideComponentContentColors() {
        listOf(AppTypography.labelLarge, AppTypography.labelMedium, AppTypography.bodyLarge,
            AppTypography.bodyMedium, AppTypography.titleLarge, AppTypography.titleMedium,
            AppTypography.displayLarge).forEach { assertEquals(Color.Unspecified, it.color) }
    }
}
