package de.dirkgerhardt.bahnfrei.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.bahnfrei.R
import de.dirkgerhardt.bahnfrei.data.SettingsRepository
import de.dirkgerhardt.bahnfrei.data.ThemeModus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val settings by SettingsRepository.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_appearance)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_zurueck)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.appearance_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ModusOption(
                titel = stringResource(R.string.theme_system),
                beschreibung = stringResource(R.string.theme_system_beschr),
                selected = settings.themeModus == ThemeModus.SYSTEM,
                onClick = { repo.setThemeModus(ThemeModus.SYSTEM) }
            )

            ModusOption(
                titel = stringResource(R.string.theme_hell),
                beschreibung = stringResource(R.string.theme_hell_beschr),
                selected = settings.themeModus == ThemeModus.HELL,
                onClick = { repo.setThemeModus(ThemeModus.HELL) }
            )

            ModusOption(
                titel = stringResource(R.string.theme_dunkel),
                beschreibung = stringResource(R.string.theme_dunkel_beschr),
                selected = settings.themeModus == ThemeModus.DUNKEL,
                onClick = { repo.setThemeModus(ThemeModus.DUNKEL) }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.dynamic_color),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.dynamic_color_beschr),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = { repo.setDynamicColor(it) }
                    )
                }
            }
        }
    }
}

