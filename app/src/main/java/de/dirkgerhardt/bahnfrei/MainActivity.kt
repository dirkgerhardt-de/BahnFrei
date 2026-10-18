package de.dirkgerhardt.bahnfrei

import android.content.Context
import android.content.res.Configuration
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
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.dirkgerhardt.bahnfrei.data.SettingsRepository
import de.dirkgerhardt.bahnfrei.data.Sprache
import de.dirkgerhardt.bahnfrei.data.ThemeModus
import de.dirkgerhardt.bahnfrei.ui.SavedParticipantsScreen
import de.dirkgerhardt.bahnfrei.ui.screens.AppearanceSettingsScreen
import de.dirkgerhardt.bahnfrei.ui.screens.ChancenSettingsScreen
import de.dirkgerhardt.bahnfrei.ui.screens.HomeScreen
import de.dirkgerhardt.bahnfrei.ui.screens.LibraryScreen
import de.dirkgerhardt.bahnfrei.ui.screens.ResultsScreen
import de.dirkgerhardt.bahnfrei.ui.screens.SavedDetailScreen
import de.dirkgerhardt.bahnfrei.ui.screens.SettingsScreen
import de.dirkgerhardt.bahnfrei.ui.screens.SpracheSettingsScreen
import de.dirkgerhardt.bahnfrei.ui.screens.UploadWizardScreen
import de.dirkgerhardt.bahnfrei.ui.theme.BahnFreiTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: MeldelistViewModel by viewModels()

    /**
     * Wendet die in den Einstellungen gewählte Sprache an, unabhängig von der
     * Systemsprache. Deutsch → Locale "de" (greift auf die Standard-values/ zurück),
     * Englisch → values-en/.
     */
    override fun attachBaseContext(newBase: Context) {
        val locale = when (SettingsRepository.gespeicherteSprache(newBase)) {
            Sprache.DEUTSCH -> Locale.GERMAN
            Sprache.ENGLISH -> Locale.ENGLISH
        }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

            BahnFreiTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                // Statusleisten-Icons (Akku, Uhr, Netz) an das Theme anpassen:
                // Hell -> dunkle Icons, Dunkel -> helle Icons
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                Surface {
                    MeldelistNavGraph(vm)
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
        composable("settings/appearance") {
            AppearanceSettingsScreen(navController)
        }
        composable("settings/chancen") {
            ChancenSettingsScreen(navController)
        }
        composable("settings/sprache") {
            SpracheSettingsScreen(navController)
        }
    }
}