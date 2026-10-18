package de.dirkgerhardt.bahnfrei.data

import android.content.res.Resources
import de.dirkgerhardt.bahnfrei.R
import java.util.Random

object MedalEstimator {

    const val MEDAILLE_SCHWELLE = 0.50

    /** Anzahl der Einträge in R.array.motivationssprueche (strings.xml). */
    const val MOTIVATIONSSPRUECHE_ANZAHL = 15

    /** Index des Motivationsspruchs; Texte werden in der UI aus strings.xml geladen. */
    fun motivationsSpruchIndex(salt: Int = 0): Int {
        val n = MOTIVATIONSSPRUECHE_ANZAHL
        return ((salt % n) + n) % n
    }

    fun parseZeitMs(zeit: String): Long? {
        val m = Regex("""^(\d{2}):(\d{2}),(\d{2})$""").find(zeit.trim()) ?: return null
        val (min, sek, hundert) = m.destructured
        return (min.toLong() * 60_000) + (sek.toLong() * 1000) + (hundert.toLong() * 10)
    }

    fun medaillenChancen(
        eigeneZeitMs: Long,
        eigeneJahrgang: Int,
        alleLaufe: Map<Int, List<RivalenZeit>>,
        relSigma: Double = 0.02,
        runs: Int = 5_000,
        rng: Random = Random(42)
    ): MedaillenChancen {
        val rivalenZeiten = alleLaufe.values.flatten()
            .filter { it.jahrgang == eigeneJahrgang }
            .map { it.zeitMs }
        if (rivalenZeiten.isEmpty()) {
            return MedaillenChancen(1.0, 0.0, 0.0)
        }
        var gold = 0
        var silber = 0
        var bronze = 0
        repeat(runs) {
            val own = rng.nextGaussian() * relSigma * eigeneZeitMs + eigeneZeitMs
            var rank = 1
            for (t in rivalenZeiten) {
                val rival = rng.nextGaussian() * relSigma * t + t
                if (rival < own) rank++
            }
            when (rank) {
                1 -> gold++
                2 -> silber++
                3 -> bronze++
            }
        }
        return MedaillenChancen(
            gold.toDouble() / runs,
            silber.toDouble() / runs,
            bronze.toDouble() / runs
        )
    }

    fun besteMedaille(chancen: MedaillenChancen): Medaille? {
        if (chancen.gesamt <= MEDAILLE_SCHWELLE) return null
        val optionen = listOf(
            chancen.gold to Medaille.GOLD,
            chancen.silber to Medaille.SILBER,
            chancen.bronze to Medaille.BRONZE
        )
        return optionen
            .filter { it.first >= chancen.gesamt * 0.30 }
            .ifEmpty { optionen }
            .maxBy { it.first }
            .second
    }

    /**
     * Die nächstbessere Medaille als Perspektive.
     * SCHWELLENLOGIK: Zeige nur, wenn echte Chance > 0% UND
     * (absolute Schwelle ≥ 20% ODER relativ ≥ 50% der Primär-Chance).
     */
    fun zweiteMedaille(
        chancen: MedaillenChancen,
        beste: Medaille?,
        schwelle: Double = 0.20
    ): Medaille? {
        if (beste == null) return null
        return listOf(
            Medaille.GOLD to chancen.gold,
            Medaille.SILBER to chancen.silber,
            Medaille.BRONZE to chancen.bronze
        )
            .filter {
                it.first.ordinal < beste.ordinal &&
                        it.second > 0.0 &&
                        (it.second >= schwelle || it.second >= 0.5)
            }
            .maxByOrNull { it.second }
            ?.first
    }

    /** Persönlicher Wertungstext; alle Texte kommen aus strings.xml (über [res]). */
    fun personalWertung(
        res: Resources,
        gesamt: Double,
        medaille: Medaille?,
        salt: Int = 0
    ): String {
        val arrayId = when {
            gesamt < 0.10 -> R.array.wertung_unter10
            gesamt < 0.20 -> R.array.wertung_unter20
            gesamt < 0.30 -> R.array.wertung_unter30
            gesamt < 0.40 -> R.array.wertung_unter40
            medaille == null -> R.array.wertung_ohne_medaille
            gesamt < 0.60 -> R.array.wertung_unter60
            gesamt < 0.70 -> R.array.wertung_unter70
            gesamt < 0.80 -> R.array.wertung_unter80
            gesamt < 0.90 -> R.array.wertung_unter90
            else -> return res.getString(
                R.string.wertung_zum_greifen_nah,
                res.getString(medaille.nameRes)
            )
        }
        val varianten = res.getStringArray(arrayId)
        return varianten[((salt % varianten.size) + varianten.size) % varianten.size]
    }

    fun chancenInfoBerechnen(
        eigeneEntries: List<MeldeEntry>,
        feld: Map<Int, List<RivalenZeit>>,
        res: Resources,
        runs: Int = 5_000
    ): Map<Int, ChancenInfo> {
        val result = HashMap<Int, ChancenInfo>()
        for (entry in eigeneEntries) {
            if (result.containsKey(entry.wettkampf)) continue
            val zeitMs = entry.zeit?.let { parseZeitMs(it) } ?: continue
            val chancen = medaillenChancen(
                zeitMs, entry.jahrgang,
                mapOf(entry.wettkampf to (feld[entry.wettkampf] ?: emptyList())),
                runs = runs
            )
            val medaille = besteMedaille(chancen)
            result[entry.wettkampf] = ChancenInfo(
                chancen = chancen,
                medaille = medaille,
                text = personalWertung(res, chancen.gesamt, medaille, entry.wettkampf),
                motivationsSpruchIndex = if (chancen.gesamt <= 0.0)
                    motivationsSpruchIndex(entry.wettkampf) else null
            )
        }
        return result
    }
}

