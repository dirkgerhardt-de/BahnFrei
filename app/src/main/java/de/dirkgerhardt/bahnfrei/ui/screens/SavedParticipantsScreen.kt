package de.dirkgerhardt.bahnfrei.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.dirkgerhardt.bahnfrei.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.bahnfrei.data.GespeicherterTeilnehmer
import de.dirkgerhardt.bahnfrei.data.SavedResultsRepository
import de.dirkgerhardt.bahnfrei.util.enc
import de.dirkgerhardt.bahnfrei.util.urlEncode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedParticipantsScreen(
    navController: NavController,
    fileName: String
) {
    val context = LocalContext.current
    val repo = remember { SavedResultsRepository(context) }

    var participants by remember { mutableStateOf<List<GespeicherterTeilnehmer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(fileName) {
        scope.launch {
            val geladen = withContext(Dispatchers.IO) {
                repo.loadParticipants(fileName)
            }
            participants = geladen
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_gespeicherte_teilnehmer)) },
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
        floatingActionButton = {
            if (!loading) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("search/${enc(fileName)}") },
                    icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.participants_hinzufuegen)) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                participants.isEmpty() -> Text(
                    stringResource(R.string.participants_leer),
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(participants) { p ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(
                                        "savedDetail/${urlEncode(fileName)}/${urlEncode(p.name)}/${urlEncode(p.verein ?: "")}"
                                    )
                                }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall)
                                p.verein?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

