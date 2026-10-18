package de.dirkgerhardt.easymeldelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.dirkgerhardt.easymeldelist.data.SettingsRepository
import de.dirkgerhardt.easymeldelist.data.ThemeModus
import de.dirkgerhardt.easymeldelist.ui.SavedParticipantsScreen
import de.dirkgerhardt.easymeldelist.ui.screens.BrandingScreen
import de.dirkgerhardt.easymeldelist.ui.screens.HomeScreen
import de.dirkgerhardt.easymeldelist.ui.screens.LibraryScreen
import de.dirkgerhardt.easymeldelist.ui.screens.ResultsScreen
import de.dirkgerhardt.easymeldelist.ui.screens.SavedDetailScreen
import de.dirkgerhardt.easymeldelist.ui.screens.SettingsScreen
import de.dirkgerhardt.easymeldelist.ui.screens.UploadWizardScreen
import de.dirkgerhardt.easymeldelist.ui.theme.EasyMeldelistTheme

class MainActivity : ComponentActivity() {

    private val vm: MeldelistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            // Instanziierung lädt die gespeicherten Einstellungen in den StateFlow
            remember { SettingsRepository(applicationContext) }
            val settings by SettingsRepository.settings.collectAsState()
            val darkTheme = when (settings.themeModus) {
                ThemeModus.SYSTEM -> isSystemInDarkTheme()
                ThemeModus.HELL -> false
                ThemeModus.DUNKEL -> true
            }

            EasyMeldelistTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                // Statusleisten-Icons (Akku, Uhr, Netz) an das Theme anpassen:
                // Hell -> dunkle Icons, Dunkel -> helle Icons
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                val showBranding = remember { mutableStateOf(true) }

                if (showBranding.value) {
                    BrandingScreen(onFinished = { showBranding.value = false })
                } else {
                    Surface {
                        MeldelistNavGraph(vm)
                    }
                }
            }
        }
    }
}

@Composable
fun MeldelistNavGraph(vm: MeldelistViewModel) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("library") { LibraryScreen(navController) }
        composable("upload") { UploadWizardScreen(navController, vm) }
        composable("search/{fileName}") { backStackEntry ->
            UploadWizardScreen(
                navController,
                vm,
                prefillFile = backStackEntry.arguments?.getString("fileName") ?: ""
            )
        }
        composable("results/{fileName}/{nachname}/{vorname}/{verein}") { backStackEntry ->
            ResultsScreen(
                navController = navController,
                fileName = backStackEntry.arguments?.getString("fileName") ?: "",
                nachname = backStackEntry.arguments?.getString("nachname") ?: "",
                vorname = backStackEntry.arguments?.getString("vorname") ?: "",
                verein = backStackEntry.arguments?.getString("verein")?.takeIf { it.isNotBlank() }
            )
        }
        composable("saved/{fileName}") { backStackEntry ->
            SavedParticipantsScreen(
                navController = navController,
                fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            )
        }
        composable("savedDetail/{fileName}/{name}/{verein}") { backStackEntry ->
            SavedDetailScreen(
                navController = navController,
                fileName = backStackEntry.arguments?.getString("fileName") ?: "",
                name = backStackEntry.arguments?.getString("name") ?: "",
                verein = backStackEntry.arguments?.getString("verein") ?: ""
            )
        }
        composable("settings") {
            SettingsScreen(navController)
        }
    }
}