package de.dirkgerhardt.bahnfrei.data

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Volatile
private var pdfBoxBereit = false

/** Lädt die PDFBox-Ressourcen einmalig, beim ersten PDF-Zugriff statt beim App-Start. */
private fun pdfBoxInitialisieren(context: Context) {
    if (pdfBoxBereit) return
    synchronized(MeldelistParser::class.java) {
        if (!pdfBoxBereit) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxBereit = true
        }
    }
}

data class Participant(
    val nachname: String,
    val vorname: String,
    val verein: String
)

/** Ergebnis der Ein-Pass-Analyse einer Meldeliste. */
data class MeldelistAnalysis(
    val eventName: String?,
    val clubs: List<String>,
    val participants: List<Participant>
)

/**
 * Portierung des Desktop-MeldelistExtractor auf Android.
 * Arbeitet auf einem InputStream statt auf einem Dateipfad.
 *
 * Unterstützte Formate:
 *  - EasyWk:      "Bahn 1 Holler, Frieda 2010 SV GR.-W. Holzkirchen 00:43,00"
 *                 (Name "Nachname, Vorname", Jahrgang ohne Suffix)
 *  - KM-Stil:     "Bahn 1 Elea Sammek 2017/Jugend E TSV Hohenbrunn-Riemerling 04:05,35"
 *                 (Name "Vorname Nachname", Jahrgang mit Altersklassen-Suffix)
 */
class MeldelistParser(private val context: Context) {

    /** Staffel-Zeilen: enthalten keinen Schwimmer/Jahrgang ("1. Mannschaft (X)") */
    private val staffelRegex = Regex("""Mannschaft|\(X\)""")

    /** Optionales Altersklassen-Suffix hinter dem Jahrgang: "/Jugend D", "/AK 20", "/Junioren" */
    private val akSuffix =
        """(?:/(?:Jugend(?:\s+[A-Z]+)?|AK(?:\s+\d{1,3})?|Junioren))?"""

    private val wettkampfRegex = Regex(
        """(?:noch\s+)?Wettkampf\s+(\d+)\s+-\s+(\d+)m\s+(Freistil|Brust|Rücken|Schmetterling|Lagen)"""
    )
    private val laufRegex = Regex("""Lauf\s+(\d+)/\d+""")

    // Gruppen: 1=Bahn, 2=Name, 3=Jahrgang, 4=Verein, 5=Meldezeit
    private val bahnRegex = Regex(
        """^Bahn\s+(\d+)\s+(.+?)\s+(\d{4})$akSuffix\s+(.+?)\s+(\d\d:\d\d,\d{2})"""
    )

    // Tag-Erkennung: "Tag 1", "Tag 2", "Samstag", "Sonntag", Datumsmuster
    private val tagRegex = Regex("""(?:Tag\s*[12]|Samstag|Sonntag|(?:\d{2}\.\d{2}\.\d{2,4}))""")

    private val styleMap = mapOf(
        "Freistil" to "F", "Brust" to "B", "Rücken" to "R",
        "Schmetterling" to "S", "Lagen" to "L"
    )

    /** Namenspräfixe, die zum Nachname gehören ("Luis Alting van Geusau"). */
    private val namensPraefixe =
        setOf("von", "van", "de", "dos", "vom", "zu", "der", "di", "da", "del", "della")

    /** Zeilen, die niemals der Veranstaltungsname sind. */
    private val genericLines = listOf(
        "easywk", "meldeergebnis", "stand:", "veranstaltet von",
        "auszug", "seite", "wettkampf", "lauf", "bahn"
    )

    // --- Titel-Extraktion (Deckblatt-Kopf) ---

    /**
     * Inhaltsmarker des Deckblatts: Ab diesen Zeilen endet der Titelblock
     * sicher (Organisationsteil, Zeitplan, Hinweise ...).
     */
    private val kopfBlocker = listOf(
        "veranstalter", "ausrichter", "veranstaltungsort", "wettkampfbecken",
        "einlass", "einschwimmen", "kampfrichter", "schriftführer",
        "zeitplan", "unverbindliche", "hinweis", "ausschreibung",
        "http://", "https://", "www."
    )

    /** Datumsangaben im Kopf: "am 14.11.2026", "vom 03.10.2026 bis 04.10.2026" u. Fragmente. */
    private val kopfDatumRegex = Regex("""\d{2}\.\d{2}\.\d{2,4}""")

    /** Ortszeile: "im Freizeitbad Unterschleißheim" */
    private val ortRegex = Regex("""^im\s+(.+)$""")

    /** Adress-Bestandteile: Postleitzahl beendet den Titelblock. */
    private val plzRegex = Regex("""\b\d{5}\b""")

    /**
     * Baut den Veranstaltungsnamen generisch aus dem Kopf der ersten Seite(n):
     *  - Datumszeilen ("am ...", "vom ... bis ...") werden verworfen
     *  - die Ortszeile ("im <Bad> <Ort>") wird verworfen, der ORT aber extrahiert
     *    (letztes Wort) und hinten angehängt
     *  - Zusatzzeilen ("mit Altersklassenwertung") werden verworfen
     *  - ab Inhaltsmarkern (Veranstalter, Zeitplan, Hinweise, Adresse, URLs,
     *    PLZ ...) wird gestoppt
     *
     * Ergebnisse:
     *  - Riemerlingen (EasyWk):  "Internationales Riemerlinger Herbstschwimmfest 2026"
     *  - KM2026:                 "Kreismeisterschaften Kreis VI Lagen und lange
     *                             Strecken 2026 Unterschleißheim"
     *
     * Rückgabe null, falls kein Titelblock erkannt wird -> Aufrufer nutzt Fallback.
     */
    private fun extractEventName(allLines: List<String>): String? {
        val titel = mutableListOf<String>()
        var ort: String? = null

        for (raw in allLines.take(25)) {
            val line = raw.trim()
            if (line.isEmpty() || line.length <= 5) continue
            val lower = line.lowercase()

            // Generische Kopfzeilen (MELDEERGEBNIS, "Seite", "Stand:") überspringen
            if (genericLines.any { lower.contains(it) }) continue

            // Inhaltsmarker / Zeitplan / Adresse / URL: Titelblock ist zu Ende
            if (kopfBlocker.any { lower.contains(it) }) break
            if (plzRegex.containsMatchIn(line)) break

            // Datumszeilen ("-Fragmente", "am 14.11.2026", "vom ... bis ...") verwerfen
            if (kopfDatumRegex.containsMatchIn(line)) continue

            // Ortszeile: "im Freizeitbad Unterschleißheim" -> Ort merken
            ortRegex.find(line)?.let { m ->
                ort = m.groupValues[1].split(Regex("\\s+")).lastOrNull()
                continue
            }

            // Zusatzzeilen wie "mit Altersklassenwertung" verwerfen
            if (lower.startsWith("mit ")) continue

            titel.add(line)
            if (titel.size >= 3) break
        }

        if (titel.isEmpty()) return null
        val name = titel.joinToString(" ")
        return ort?.let { "$name $it" } ?: name
    }

    /** Liest alle Textzeilen einer Meldeliste. */
    private fun readLines(input: java.io.InputStream): List<String> {
        pdfBoxInitialisieren(this@MeldelistParser.context)
        return PDDocument.load(input.readBytes()).use { document ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.getText(document).lineSequence()
                .map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
    }

    /** Bestimmt den Tag aus einer Textzeile (1, 2, oder 0 = unbekannt). */
    private fun bestimmeTagZeile(line: String): Int {
        val lower = line.lowercase()
        return when {
            "samstag" in lower || "stag 1" in lower || "tag 1" in lower || "stag" in lower -> 1
            "sonntag" in lower || "stag 2" in lower || "tag 2" in lower -> 2
            else -> 0
        }
    }

    /**
     * Zerlegt einen rohen Namen in (Nachname, Vorname).
     *  - EasyWk:  "Holler, Frieda"       -> ("Holler", "Frieda")
     *  - KM-Stil: "Johanna Gerhardt"     -> ("Gerhardt", "Johanna")
     *  - KM-Stil: "Luis Alting van Geusau" -> ("van Geusau", "Luis Alting")
     */
    private fun splitName(rawName: String): Pair<String, String>? {
        if (rawName.contains(',')) {
            // EasyWk: "Nachname, Vorname"
            val p = rawName.split(',', limit = 2)
            return p[0].trim() to p[1].trim()
        }
        // KM-Stil: "Vorname Nachname" – Nachname hinten, inkl. Präfixe
        val tokens = rawName.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size < 2) return null
        var start = tokens.size - 1
        while (start > 1 && namensPraefixe.contains(tokens[start - 1].lowercase())) start--
        val nachname = tokens.subList(start, tokens.size).joinToString(" ")
        val vorname = tokens.subList(0, start).joinToString(" ")
        return nachname to vorname
    }

    /**
     * Analysiert eine Meldeliste in EINEM Durchlauf:
     * Veranstaltungsname, Vereine und Teilnehmer.
     */
    suspend fun analyze(input: java.io.InputStream): MeldelistAnalysis =
        withContext(Dispatchers.IO) {
            val lines = readLines(input)

            // Veranstaltungsname: generischer Titelblock des Deckblatts,
            // Fallback: erste "nicht-generische" Zeile der ersten Seite
            val eventName = extractEventName(lines)
                ?: lines.take(15).firstOrNull { line ->
                    val l = line.lowercase()
                    line.isNotBlank() && genericLines.none { l.contains(it) } && line.length > 5
                }?.trim()

            // Staffel-Zeilen rausfiltern: kein Jahrgang, Namen sind "1. Mannschaft (X)"
            val datenZeilen = lines.asSequence()
                .filterNot { staffelRegex.containsMatchIn(it) }

            // Vereine
            val clubRegex = Regex(
                """^Bahn\s+\d+\s+.+?\s+\d{4}$akSuffix\s+(.+?)\s+\d\d:\d\d,\d{2}"""
            )
            val clubSortKey: (String) -> String = {
                it.lowercase().replace(Regex("[^a-zäöüß0-9]"), "")
            }
            val clubs = datenZeilen
                .mapNotNull { clubRegex.find(it)?.groupValues?.get(1)?.trim() }
                .distinct()
                .sortedBy(clubSortKey)
                .toList()

            // Teilnehmer: Komma optional, Jahrgang mit Altersklassen-Suffix
            val participantRegex = Regex(
                """^Bahn\s+\d+\s+(.+?)\s+(\d{4})$akSuffix\s+(.+?)\s+\d\d:\d\d,\d{2}"""
            )
            val participants = datenZeilen
                .mapNotNull { line ->
                    participantRegex.find(line)?.let { m ->
                        val (nachname, vorname) = splitName(m.groupValues[1].trim())
                            ?: return@mapNotNull null
                        Participant(nachname, vorname, m.groupValues[3].trim())
                    }
                }
                .distinctBy { "${it.nachname}|${it.vorname}|${it.verein}" }
                .toList()

            MeldelistAnalysis(eventName, clubs, participants)
        }

    /**
     * Die Personensuche: alle Meldungen eines Schwimmers, optional nach
     * Verein gefiltert. Über [onHeatEntry] werden zusätzlich ALLE
     * Meldezeiten eines Laufs gemeldet (für die Medaillenchance-Berechnung).
     */
    suspend fun findSwimmer(
        input: java.io.InputStream,
        nachname: String,
        vorname: String,
        verein: String? = null,
        onHeatEntry: ((wettkampf: Int, lauf: Int, jahrgang: Int, zeitMs: Long, name: String) -> Unit)? = null
    ): List<MeldeEntry> = withContext(Dispatchers.IO) {
        val searchName = "$nachname, $vorname"
        android.util.Log.d("SEARCH_DEBUG", "searchName='$searchName', verein='$verein'")
        val entries = mutableListOf<MeldeEntry>()
        var wk = 0; var distanz = 0; var art = ""; var lauf = 0
        var aktuellesTag = 0               // aktueller Tag aus Kontext

        readLines(input).forEach { line ->

            // Staffel-Zeilen überspringen: kein Schwimmer, kein Jahrgang
            if (staffelRegex.containsMatchIn(line)) return@forEach

            val wkMatch = wettkampfRegex.find(line)

            // Tag-Kontext aus der Zeile extrahieren (z.B. "Tag 1" / "Samstag" im Header)
            if (tagRegex.containsMatchIn(line)) {
                val erkanntesTag = bestimmeTagZeile(line)
                if (erkanntesTag > 0) {
                    aktuellesTag = erkanntesTag
                }
            }

            when {
                wkMatch != null -> {
                    wk = wkMatch.groupValues[1].toInt()
                    distanz = wkMatch.groupValues[2].toInt()
                    art = styleMap[wkMatch.groupValues[3]] ?: "?"
                    lauf = 0
                }
                laufRegex.find(line) != null -> {
                    lauf = laufRegex.find(line)!!.groupValues[1].toInt()
                }
                else -> bahnRegex.find(line)?.let { m ->
                    val name = m.groupValues[2].trim()
                    val club = m.groupValues[4].trim()
                    val jahrgang = m.groupValues[3].toInt()
                    val zeit = m.groupValues[5]

                    val tag = aktuellesTag

                    // Meldezeit an die Heat-Sammlung melden (für Medaillenchance)
                    if (zeit.isNotBlank()) {
                        MedalEstimator.parseZeitMs(zeit)?.let { ms ->
                            onHeatEntry?.invoke(wk, lauf, jahrgang, ms, name)
                        }
                    }

                    if (isSameName(name, searchName) &&
                        (verein == null || club.contains(verein, ignoreCase = true))
                    ) {
                        entries.add(
                            MeldeEntry(
                                name = name,
                                wettkampf = wk,
                                lauf = lauf,
                                bahn = m.groupValues[1].toInt(),
                                distanz = distanz,
                                schwimmart = art,
                                zeit = zeit.ifBlank { "" },
                                verein = club,
                                jahrgang = jahrgang,
                                tag = tag
                            )
                        )
                    }
                }
            }
        }
        entries
    }

    /**
     * Reihenfolge-unabhängiger Namensvergleich: Die Tokens werden sortiert,
     * deshalb ist es egal, ob "Nachname, Vorname" (EasyWk) oder
     * "Vorname Nachname" (KM) im PDF steht bzw. wie der Nutzer tippt.
     * Achtung: Bei Vertauschungen wie "Maria Anna" vs. "Anna Maria" kann das
     * theoretisch fälschlich matchen – praktisch unkritisch bei konkreten Suchen.
     */
    private fun isSameName(found: String, searched: String): Boolean {
        fun norm(s: String) = s.replace(",", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .sorted()
            .joinToString(" ")
        return norm(found) == norm(searched)
    }
}
