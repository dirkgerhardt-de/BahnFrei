package de.dirkgerhardt.bahnfrei.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.bahnfrei.R
import de.dirkgerhardt.bahnfrei.data.*
import de.dirkgerhardt.bahnfrei.ui.components.ChanceAnzeige
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDetailScreen(
    navController: NavController,
    fileName: String,
    name: String,
    verein: String
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val savedRepo = remember { SavedResultsRepository(context) }

    val settings by SettingsRepository.settings.collectAsState()

    var entries by remember { mutableStateOf<List<MeldeEntry>?>(null) }
    var chancenCache by remember { mutableStateOf<Map<Int, ChancenInfo>>(emptyMap()) }

    LaunchedEffect(fileName, name) {
        android.util.Log.d("SavedDetail", "fileName='$fileName' nameArg='$name'")
        val geladen = savedRepo.loadEntries(fileName, name)
        android.util.Log.d("SavedDetail", "entries=${geladen.size}")
        entries = geladen

        val feld = savedRepo.loadFeld(fileName, name)
        chancenCache = if (geladen.isEmpty() || feld.isEmpty()) {
            emptyMap()
        } else {
            withContext(Dispatchers.Default) {
                MedalEstimator.chancenInfoBerechnen(geladen, feld, context.resources)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_zurueck)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_einstellungen)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val result = entries) {
            null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val headline = when {
                        result.isEmpty() -> stringResource(R.string.saved_keine)
                        verein.isNotBlank() -> stringResource(R.string.saved_name_verein, name, verein)
                        else -> name
                    }
                    Text(
                        headline,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = if (result.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                result.groupBy { it.tag }.forEach { (tag, tagEntries) ->
                    if (tag > 0) {
                        item {
                            Text(
                                stringResource(R.string.tag_nr, tag),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    items(tagEntries) { entry ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(
                                        R.string.wettkampf_titel,
                                        entry.wettkampf,
                                        entry.distanz,
                                        schwimmartAusgeschrieben(entry.schwimmart)
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    stringResource(R.string.lauf_bahn, entry.lauf, entry.bahn),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                entry.zeit?.let { zeit ->
                                    Text(
                                        stringResource(R.string.saved_meldezeit, zeit),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                ChanceAnzeige(chancenCache[entry.wettkampf], entry.wettkampf, settings)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Hilfsfunktion MUSS in dieser Datei stehen (file-private in Kotlin)
@Composable
private fun schwimmartAusgeschrieben(kuerzel: String): String = when (kuerzel) {
    "F" -> stringResource(R.string.schwimmart_freistil)
    "B" -> stringResource(R.string.schwimmart_brust)
    "R" -> stringResource(R.string.schwimmart_ruecken)
    "S" -> stringResource(R.string.schwimmart_schmetterling)
    "L" -> stringResource(R.string.schwimmart_lagen)
    else -> kuerzel
}

