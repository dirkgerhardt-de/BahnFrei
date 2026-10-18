package de.dirkgerhardt.bahnfrei.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.dirkgerhardt.bahnfrei.R
import de.dirkgerhardt.bahnfrei.data.AnzeigeModus
import de.dirkgerhardt.bahnfrei.data.AppSettings
import de.dirkgerhardt.bahnfrei.data.ChancenInfo
import de.dirkgerhardt.bahnfrei.data.Medaille
import de.dirkgerhardt.bahnfrei.data.MedalEstimator
import kotlin.math.roundToInt

/**
 * Reine Anzeige der vorberechneten Chancen – identisch für
 * Ergebnis-Screen und gespeicherte Ergebnisse.
 *
 * MEDAILLENCHANCEN-Modus:
 * - Gesamt ≥ 50 %:  "🥈 Silber bei maximum Power – 🥉 Bronze ist stabil"
 * - Gesamt < 50 %:  Motivationstext (wenn in Settings aktiviert), sonst nichts
 *
 * PRO-Modus:
 * - IMMER alle exakten Prozentwerte (Gold, Silber, Bronze, Gesamt),
 *   auch bei 0% – keine Filter, keine Unterdrückung.
 */
@Composable
fun ChanceAnzeige(
    info: ChancenInfo?,
    wettkampfNr: Int = 0,
    settings: AppSettings = AppSettings()
) {
    if (info == null) {
        return
    }
    Spacer(Modifier.height(12.dp))

    // ZENTRALE STELLE: Perspektive mit Schwelle und 0%-Filter berechnen
    val perspektive = MedalEstimator.zweiteMedaille(
        info.chancen,
        info.medaille,
        settings.perspektiveSchwelle
    )

    when (settings.modus) {
        AnzeigeModus.MEDAILLENCHANCEN -> {
            if (info.medaille == null) {
                // Keine Medaille (Gesamtchance < 50 %)
                if (settings.motivationAnzeigen) {
                    if (info.chancen.gesamt > 0.0) {
                        Text(info.text, style = MaterialTheme.typography.bodySmall)
                    } else {
                        val sprueche = stringArrayResource(R.array.motivationssprueche)
                        Text(
                            info.motivationsSpruchIndex?.let { sprueche.getOrNull(it) } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Motivation aus: nichts anzeigen
                return
            }

            // Medaille vorhanden: kombinierte Anzeige als ein Text
            val text = nuechternerText(info, wettkampfNr, perspektive)
            Text(text, style = MaterialTheme.typography.bodySmall)
        }

        AnzeigeModus.PRO -> {
            Text(prozentZeile(info), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Text für den Medaillenchancen-Modus.
 * Perspektive LINKS (Vision), Primär RECHTS (Basis).
 * Format: "🥈 Silber bei maximum Power – 🥉 Bronze ist stabil"
 */
@Composable
private fun nuechternerText(
    info: ChancenInfo,
    salt: Int,
    perspektive: Medaille?
): String {
    val medaille = info.medaille ?: return ""

    val hauptPool = when {
        info.chancen.gesamt >= 0.70 -> stringArrayResource(R.array.chance_haupt_hoch)
        info.chancen.gesamt >= 0.50 -> stringArrayResource(R.array.chance_haupt_mittel)
        else -> stringArrayResource(R.array.chance_haupt_niedrig)
    }
    val schlussPool = stringArrayResource(R.array.chance_schluss)

    // Streuwert aus den Nachkommastellen der Chance – jeder Wettkampf hat
    // dort andere Werte, das bricht die Kopplung an die Wettkampf-Nummer
    val streu = (info.chancen.gesamt * 1000).toInt()

    val idxHaupt = poolIndex(salt, streu, hauptPool.size)
    val idxSchluss = poolIndex(salt + 1, streu, schlussPool.size)

    val satz = stringResource(
        R.string.chance_satz,
        medaille.emoji,
        stringResource(medaille.nameRes),
        hauptPool[idxHaupt]
    )

    return if (perspektive != null) {
        stringResource(
            R.string.chance_perspektive,
            perspektive.emoji,
            stringResource(perspektive.nameRes),
            schlussPool[idxSchluss]
        ) + " · " + satz
    } else {
        satz
    }
}

/**
 * PRO-Modus: Zeigt IMMER alle Prozentwerte, ohne Filter –
 * auch 0% wird explizit angezeigt (vollständige Transparenz).
 */
@Composable
private fun prozentZeile(info: ChancenInfo): String {
    fun pct(wert: Double): Int = (wert * 100).roundToInt()
    return stringResource(
        R.string.prozent_zeile,
        pct(info.chancen.gold),
        pct(info.chancen.silber),
        pct(info.chancen.bronze),
        pct(info.chancen.gesamt)
    )
}

/**
 * Deterministischer Index über einen Murmur-artigen Hash.
 * Verteilt auch bei nur ungeraden oder gehäuften Wettkampf-Nummern
 * gleichmäßig über den Pool, ohne dass sich Paarungen wiederholen.
 */
private fun poolIndex(salt: Int, streu: Int, size: Int): Int {
    var x = salt * 374761393 + streu * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return ((x % size) + size) % size
}

