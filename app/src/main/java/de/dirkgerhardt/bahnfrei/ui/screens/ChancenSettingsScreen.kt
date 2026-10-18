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
import de.dirkgerhardt.bahnfrei.data.AnzeigeModus
import de.dirkgerhardt.bahnfrei.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChancenSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val settings by SettingsRepository.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_medaillenchancen)) },
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
                stringResource(R.string.chancen_anzeige),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ModusOption(
                titel = stringResource(R.string.title_medaillenchancen),
                beschreibung = stringResource(R.string.chancen_medaillen_beschr),
                selected = settings.modus == AnzeigeModus.MEDAILLENCHANCEN,
                onClick = { repo.setModus(AnzeigeModus.MEDAILLENCHANCEN) }
            )

            ModusOption(
                titel = stringResource(R.string.chancen_pro_titel),
                beschreibung = stringResource(R.string.chancen_pro_beschr),
                selected = settings.modus == AnzeigeModus.PRO,
                onClick = { repo.setModus(AnzeigeModus.PRO) }
            )

            // Motivation bei geringer Gesamtchance (<50 %)
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
                            stringResource(R.string.chancen_motivation_titel),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.chancen_motivation_beschr),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.motivationAnzeigen,
                        onCheckedChange = { repo.setMotivationAnzeigen(it) }
                    )
                }
            }

            Text(
                stringResource(R.string.chancen_perspektive),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.chancen_perspektive_zeigen),
                        style = MaterialTheme.typography.titleSmall
                    )

                    Slider(
                        value = settings.perspektiveSchwelle.toFloat(),
                        onValueChange = {
                            repo.setPerspektiveSchwelle(it.toDouble())
                        },
                        valueRange = 0f..0.5f,
                        steps = 9,  // 5 %-Intervalle: 0 %, 5 %, 10 %, ..., 50 %
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        stringResource(R.string.chancen_perspektive_hilfe),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

