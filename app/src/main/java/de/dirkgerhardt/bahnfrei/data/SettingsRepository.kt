package de.dirkgerhardt.bahnfrei.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AnzeigeModus {
    /** Einschätzung plus wahrscheinlichste Medaille (Standard) */
    MEDAILLENCHANCEN,
    /** Exakte Prozentwerte für Gold, Silber, Bronze und Gesamt */
    PRO
}

enum class ThemeModus {
    SYSTEM,
    HELL,
    DUNKEL
}

enum class Sprache {
    DEUTSCH,
    ENGLISH
}

data class AppSettings(
    val sprache: Sprache = Sprache.DEUTSCH,
    val themeModus: ThemeModus = ThemeModus.SYSTEM,
    /** Material 3 Dynamic Color (Android 12+) */
    val dynamicColor: Boolean = true,
    val modus: AnzeigeModus = AnzeigeModus.MEDAILLENCHANCEN,
    /**
     * ZENTRALE EINSTELLUNG: Mindest-Wahrscheinlichkeit (0.0–1.0) der BESSEREN
     * Medaille, ab der sie als Perspektive angezeigt wird.
     * Standard 0.20 = 20 % (empirische Schwelle).
     */
    val perspektiveSchwelle: Double = 0.20,
    /**
     * Motivation bei <50 % Gesamtchance:
     * true  -> Motivationstext anzeigen ("Geheimtipp", Sprüche etc.)
     * false -> nichts anzeigen
     */
    val motivationAnzeigen: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        _settings.value = AppSettings(
            sprache = leseSprache(),
            themeModus = leseThemeModus(),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
            modus = leseModus(),
            perspektiveSchwelle = lesePerspektiveSchwelle(),
            motivationAnzeigen = leseMotivationAnzeigen()
        )
    }

    private fun leseSprache(): Sprache = leseSpracheAus(prefs)

    fun setSprache(neu: Sprache) {
        prefs.edit().putString(KEY_SPRACHE, neu.name).apply()
        _settings.value = _settings.value.copy(sprache = neu)
    }

    private fun leseThemeModus(): ThemeModus {
        val gespeichert = prefs.getString(KEY_THEME, null) ?: return ThemeModus.SYSTEM
        return runCatching { ThemeModus.valueOf(gespeichert) }.getOrDefault(ThemeModus.SYSTEM)
    }

    fun setThemeModus(neu: ThemeModus) {
        prefs.edit().putString(KEY_THEME, neu.name).apply()
        _settings.value = _settings.value.copy(themeModus = neu)
    }

    fun setDynamicColor(neu: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, neu).apply()
        _settings.value = _settings.value.copy(dynamicColor = neu)
    }

    private fun leseModus(): AnzeigeModus {
        val gespeichert = prefs.getString(KEY_MODUS, null)
        if (gespeichert != null) {
            return runCatching { AnzeigeModus.valueOf(gespeichert) }
                .getOrDefault(AnzeigeModus.MEDAILLENCHANCEN)
            // Alter "MOTIVATION"-Wert crasht hier nicht, sondern fällt
            // auf MEDAILLENCHANCEN zurück – genau das Gewünschte.
        }
        // Migration von den alten Boolean-Keys (erste Version der Settings)
        val altesPro = prefs.getBoolean(KEY_ALT_PRO, false)
        if (prefs.contains(KEY_ALT_PRO) && altesPro) return AnzeigeModus.PRO
        val alteChancen = prefs.getBoolean(KEY_ALT_CHANCEN, true)
        if (prefs.contains(KEY_ALT_CHANCEN) && !alteChancen) return AnzeigeModus.MEDAILLENCHANCEN
        return AnzeigeModus.MEDAILLENCHANCEN
    }

    private fun lesePerspektiveSchwelle(): Double =
        prefs.getFloat(KEY_PERSPEKTIVE, 0.20f).toDouble().coerceIn(0.0, 1.0)

    private fun leseMotivationAnzeigen(): Boolean =
        prefs.getBoolean(KEY_MOTIVATION, true)

    fun setModus(neu: AnzeigeModus) {
        prefs.edit().putString(KEY_MODUS, neu.name).apply()
        _settings.value = _settings.value.copy(modus = neu)
    }

    fun setPerspektiveSchwelle(neu: Double) {
        val gerastet = (kotlin.math.round(neu * 20) / 20.0).coerceIn(0.0, 1.0)
        prefs.edit().putFloat(KEY_PERSPEKTIVE, gerastet.toFloat()).apply()
        _settings.value = _settings.value.copy(perspektiveSchwelle = gerastet)
    }

    fun setMotivationAnzeigen(neu: Boolean) {
        prefs.edit().putBoolean(KEY_MOTIVATION, neu).apply()
        _settings.value = _settings.value.copy(motivationAnzeigen = neu)
    }

    companion object {
        private const val PREFS_NAME = "easymeldelist_settings"
        private const val KEY_SPRACHE = "sprache"
        private const val KEY_THEME = "theme_modus"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_MODUS = "anzeige_modus"
        private const val KEY_PERSPEKTIVE = "perspektive_schwelle"
        private const val KEY_MOTIVATION = "motivation_anzeigen"
        // Alte Keys der Boolean-Version, nur für Migration relevant
        private const val KEY_ALT_PRO = "pro_modus"
        private const val KEY_ALT_CHANCEN = "medaillen_chancen_anzeigen"

        /** Gespeicherte Sprache, z. B. für Locale-Setup vor der Activity-Erstellung */
        fun gespeicherteSprache(context: Context): Sprache =
            leseSpracheAus(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

        private fun leseSpracheAus(prefs: SharedPreferences): Sprache {
            val gespeichert = prefs.getString(KEY_SPRACHE, null) ?: return Sprache.DEUTSCH
            return runCatching { Sprache.valueOf(gespeichert) }.getOrDefault(Sprache.DEUTSCH)
        }

        private val _settings = MutableStateFlow(AppSettings())
        val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    }
}