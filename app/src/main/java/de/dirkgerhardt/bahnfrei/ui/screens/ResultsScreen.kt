package de.dirkgerhardt.bahnfrei.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.dirkgerhardt.bahnfrei.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.bahnfrei.data.*
import de.dirkgerhardt.bahnfrei.util.urlEncode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    navController: NavController,
    fileName: String,
    nachname: String,
    vorname: String,
    verein: String?
) {
    val context = LocalContext.current
    val repository = remember { MeldelistRepository(context) }
    val parser = remember { MeldelistParser(context) }
    val savedRepo = remember { SavedResultsRepository(context) }

    var entries by remember { mutableStateOf<List<MeldeEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    var wettkampfFeld by remember { mutableStateOf<Map<Int, List<RivalenZeit>>>(emptyMap()) }

    LaunchedEffect(fileName, nachname, vorname, verein) {
        android.util.Log.d("SEARCH_DEBUG", "fileName='$fileName' nachname='$nachname' vorname='$vorname' verein='$verein'")
        val feld = HashMap<Int, MutableList<RivalenZeit>>()
        entries = try {
            error = null
            parser.findSwimmer(
                repository.open(fileName), nachname, vorname, verein
            ) { wk, lauf, jahrgang, zeitMs, name ->
                val isOwn = name.contains("$nachname,", ignoreCase = true) &&
                        name.contains(vorname, ignoreCase = true)
                if (!isOwn) {
                    val list = feld[wk]
                    if (list == null) {
                        feld[wk] = mutableListOf(RivalenZeit(jahrgang, zeitMs))
                    } else {
                        list.add(RivalenZeit(jahrgang, zeitMs))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BahnFrei", "Suche fehlgeschlagen", e)
            error = e.message
            emptyList()
        }
        wettkampfFeld = feld
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.cd_erfolg),
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.title_speichern_erfolgreich)) },
            text = { Text(stringResource(R.string.results_gespeichert)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        navController.navigate("saved/${urlEncode(fileName)}") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.results_anzeigen)) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDialog = false
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.results_neue_suche)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_suchergebnis)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_zurueck)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Buttons immer sichtbar am unteren Rand – kein Scrollen nötig
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.abbrechen)) }

                    Button(
                        onClick = {
                            entries?.let { result ->
                                savedRepo.save(
                                    fileName,
                                    "$nachname, $vorname",
                                    verein,
                                    result,
                                    wettkampfFeld
                                )
                                showDialog = true
                            }
                        },
                        enabled = entries?.isNotEmpty() == true,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.speichern)) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val result = entries) {
                null -> {}
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        val headline = when {
                            result.isEmpty() && error != null ->
                                stringResource(R.string.results_fehler, error ?: "")
                            result.isEmpty() -> stringResource(R.string.results_keine)
                            verein != null ->
                                stringResource(R.string.results_name_verein, nachname, vorname, verein)
                            else -> stringResource(R.string.results_name, nachname, vorname)
                        }
                        Text(
                            headline,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = if (result.isEmpty() && error != null)
                                MaterialTheme.colorScheme.error
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
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text(
                                        stringResource(
                                            R.string.wettkampf_titel,
                                            entry.wettkampf,
                                            entry.distanz,
                                            schwimmartAusgeschrieben(entry.schwimmart)
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        stringResource(R.string.lauf_bahn, entry.lauf, entry.bahn),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (entries == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    ElevatedCard(modifier = Modifier.padding(32.dp)) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 6.dp,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                stringResource(R.string.lade_daten, nachname, vorname),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun schwimmartAusgeschrieben(kuerzel: String): String = when (kuerzel) {
    "F" -> stringResource(R.string.schwimmart_freistil)
    "B" -> stringResource(R.string.schwimmart_brust)
    "R" -> stringResource(R.string.schwimmart_ruecken)
    "S" -> stringResource(R.string.schwimmart_schmetterling)
    "L" -> stringResource(R.string.schwimmart_lagen)
    else -> kuerzel
}


