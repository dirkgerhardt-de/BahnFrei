package de.dirkgerhardt.bahnfrei.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.app.Activity
import de.dirkgerhardt.bahnfrei.R
import de.dirkgerhardt.bahnfrei.data.SettingsRepository
import de.dirkgerhardt.bahnfrei.data.Sprache

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpracheSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val settings by SettingsRepository.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_sprache)) },
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
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModusOption(
                titel = stringResource(R.string.sprache_deutsch),
                selected = settings.sprache == Sprache.DEUTSCH,
                onClick = {
                    if (settings.sprache != Sprache.DEUTSCH) {
                        repo.setSprache(Sprache.DEUTSCH)
                        (context as? Activity)?.recreate()
                    }
                }
            )

            ModusOption(
                titel = stringResource(R.string.sprache_english),
                selected = settings.sprache == Sprache.ENGLISH,
                onClick = {
                    if (settings.sprache != Sprache.ENGLISH) {
                        repo.setSprache(Sprache.ENGLISH)
                        (context as? Activity)?.recreate()
                    }
                }
            )
        }
    }
}

