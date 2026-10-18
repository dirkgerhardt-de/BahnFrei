package de.dirkgerhardt.bahnfrei.data

import androidx.annotation.StringRes
import de.dirkgerhardt.bahnfrei.R

/** Medaillen-Typ für die Anzeige (reine Enumeration, Texte aus strings.xml). */
enum class Medaille(@StringRes val nameRes: Int) {
    GOLD(R.string.medaille_gold),
    SILBER(R.string.medaille_silber),
    BRONZE(R.string.medaille_bronze);

    val emoji get() = when(this) {
        GOLD -> "🥇"
        SILBER -> "🥈"
        BRONZE -> "🥉"
    }
}

