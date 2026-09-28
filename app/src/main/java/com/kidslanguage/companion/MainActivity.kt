package com.kidslanguage.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kidslanguage.companion.ui.navigation.AppNavigation
import com.kidslanguage.companion.ui.theme.KidsLanguageTheme
import com.kidslanguage.companion.ui.theme.Neutral50

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system bars for immersive preschool tablet experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        val app = application as KidsLanguageApp

        setContent {
            KidsLanguageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Neutral50
                ) {
                    AppNavigation(container = app.container)
                }
            }
        }
    }

    override fun onStop() {
        (application as KidsLanguageApp).container.soundEffectManager.stop()
        super.onStop()
    }
}
