package de.dirkgerhardt.bahnfrei.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.bahnfrei.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_einstellungen)) },
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
            SettingsLinkCard(
                titel = stringResource(R.string.title_appearance),
                beschreibung = stringResource(R.string.settings_beschr_appearance),
                onClick = { navController.navigate("settings/appearance") }
            )

            SettingsLinkCard(
                titel = stringResource(R.string.title_medaillenchancen),
                beschreibung = stringResource(R.string.settings_beschr_chancen),
                onClick = { navController.navigate("settings/chancen") }
            )

            SettingsLinkCard(
                titel = stringResource(R.string.title_sprache),
                beschreibung = stringResource(R.string.settings_beschr_sprache),
                onClick = { navController.navigate("settings/sprache") }
            )
        }
    }
}

@Composable
private fun SettingsLinkCard(
    titel: String,
    beschreibung: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(titel, style = MaterialTheme.typography.titleMedium)
                Text(
                    beschreibung,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

@Composable
internal fun ModusOption(
    titel: String,
    selected: Boolean,
    onClick: () -> Unit,
    beschreibung: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(titel, style = MaterialTheme.typography.titleMedium)
                if (beschreibung.isNotEmpty()) {
                    Text(
                        beschreibung,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}


