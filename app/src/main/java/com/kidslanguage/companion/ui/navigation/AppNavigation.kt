package com.kidslanguage.companion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kidslanguage.companion.app.AppContainer
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.PracticeMode
import com.kidslanguage.companion.feature.category.CategoryScreen
import com.kidslanguage.companion.feature.choose.ChooseScreen
import com.kidslanguage.companion.feature.choose.ChooseViewModel
import com.kidslanguage.companion.feature.home.HomeScreen
import com.kidslanguage.companion.feature.home.HomeViewModel
import com.kidslanguage.companion.feature.mode.ModeSelectScreen
import com.kidslanguage.companion.feature.parent.DevAudioDiagnosticScreen
import com.kidslanguage.companion.feature.parent.ParentGateDialog
import com.kidslanguage.companion.feature.parent.ParentSettingsScreen
import com.kidslanguage.companion.feature.parent.ParentViewModel
import com.kidslanguage.companion.feature.repeat.RepeatScreen
import com.kidslanguage.companion.feature.repeat.RepeatViewModel
import com.kidslanguage.companion.feature.result.SessionCompleteScreen
import com.kidslanguage.companion.feature.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CATEGORY = "category/{language}"
    const val MODE_SELECT = "mode_select/{language}/{categoryId}"
    const val REPEAT = "repeat/{language}/{categoryId}"
    const val CHOOSE = "choose/{language}/{categoryId}"
    const val SESSION_COMPLETE = "session_complete"
    const val PARENT_SETTINGS = "parent_settings"
    const val DEV_DIAGNOSTICS = "dev_diagnostics"

    fun category(language: LanguageCode) = "category/${language.name}"
    fun modeSelect(language: LanguageCode, categoryId: String) = "mode_select/${language.name}/$categoryId"
    fun repeat(language: LanguageCode, categoryId: String) = "repeat/${language.name}/$categoryId"
    fun choose(language: LanguageCode, categoryId: String) = "choose/${language.name}/$categoryId"
}

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    var showParentGate by remember { mutableStateOf(false) }

    val homeViewModel = remember { HomeViewModel(container.settingsRepository, container.promptPlayer) }
    val parentViewModel = remember { ParentViewModel(container.settingsRepository) }

    if (showParentGate) {
        ParentGateDialog(
            viewModel = parentViewModel,
            onSuccess = {
                showParentGate = false
                navController.navigate(Routes.PARENT_SETTINGS)
            },
            onDismiss = { showParentGate = false }
        )
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onStartPractice = { lang ->
                    navController.navigate(Routes.category(lang))
                },
                onOpenParentMode = {
                    parentViewModel.regenerateGate()
                    showParentGate = true
                }
            )
        }

        composable(
            route = Routes.CATEGORY,
            arguments = listOf(navArgument("language") { type = NavType.StringType })
        ) { backStackEntry ->
            val langStr = backStackEntry.arguments?.getString("language") ?: LanguageCode.JA_JP.name
            val language = try { LanguageCode.valueOf(langStr) } catch (_: Exception) { LanguageCode.JA_JP }

            CategoryScreen(
                language = language,
                contentRepository = container.contentRepository,
                onCategorySelected = { catId ->
                    navController.navigate(Routes.modeSelect(language, catId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MODE_SELECT,
            arguments = listOf(
                navArgument("language") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val langStr = backStackEntry.arguments?.getString("language") ?: LanguageCode.JA_JP.name
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "greetings"
            val language = try { LanguageCode.valueOf(langStr) } catch (_: Exception) { LanguageCode.JA_JP }

            ModeSelectScreen(
                onModeSelected = { mode ->
                    when (mode) {
                        PracticeMode.REPEAT -> navController.navigate(Routes.repeat(language, categoryId))
                        PracticeMode.CHOOSE -> navController.navigate(Routes.choose(language, categoryId))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.REPEAT,
            arguments = listOf(
                navArgument("language") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val langStr = backStackEntry.arguments?.getString("language") ?: LanguageCode.JA_JP.name
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "greetings"
            val language = try { LanguageCode.valueOf(langStr) } catch (_: Exception) { LanguageCode.JA_JP }

            val repeatViewModel: RepeatViewModel = viewModel(viewModelStoreOwner = backStackEntry) {
                RepeatViewModel(
                    contentRepository = container.contentRepository,
                    promptPlayer = container.promptPlayer,
                    speechRecognizer = container.speechRecognizerEngine,
                    evaluationService = container.speechEvaluationService,
                    settingsRepository = container.settingsRepository,
                    soundEffectManager = container.soundEffectManager
                )
            }

            RepeatScreen(
                viewModel = repeatViewModel,
                language = language,
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onSessionComplete = {
                    navController.navigate(Routes.SESSION_COMPLETE)
                }
            )
        }

        composable(
            route = Routes.CHOOSE,
            arguments = listOf(
                navArgument("language") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val langStr = backStackEntry.arguments?.getString("language") ?: LanguageCode.JA_JP.name
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "greetings"
            val language = try { LanguageCode.valueOf(langStr) } catch (_: Exception) { LanguageCode.JA_JP }

            val chooseViewModel = remember(language, categoryId) {
                ChooseViewModel(
                    contentRepository = container.contentRepository,
                    promptPlayer = container.promptPlayer,
                    settingsRepository = container.settingsRepository,
                    soundEffectManager = container.soundEffectManager
                )
            }

            ChooseScreen(
                viewModel = chooseViewModel,
                language = language,
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onSessionComplete = {
                    navController.navigate(Routes.SESSION_COMPLETE)
                }
            )
        }

        composable(Routes.SESSION_COMPLETE) {
            SessionCompleteScreen(
                promptPlayer = container.promptPlayer,
                onPlayAgain = {
                    navController.popBackStack()
                },
                onBackHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PARENT_SETTINGS) {
            ParentSettingsScreen(
                viewModel = parentViewModel,
                onBack = { navController.popBackStack() },
                onOpenDevLab = { navController.navigate(Routes.DEV_DIAGNOSTICS) }
            )
        }

        composable(Routes.DEV_DIAGNOSTICS) {
            DevAudioDiagnosticScreen(
                container = container,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
