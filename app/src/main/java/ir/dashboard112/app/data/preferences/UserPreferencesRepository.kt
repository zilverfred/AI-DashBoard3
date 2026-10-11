package ir.dashboard112.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore by preferencesDataStore(name = "dashboard112_settings")

@Serializable
enum class ThemeModePreference(val labelFa: String) {
    SYSTEM("هماهنگ با سیستم"),
    LIGHT("حالت روشن"),
    DARK("حالت تاریک")
}

@Serializable
data class UserPreferencesState(
    val themeMode: ThemeModePreference = ThemeModePreference.LIGHT,
    val primaryColorKey: String = "BLUE", // BLUE, EMERALD, INDIGO, AMBER, ROSE
    val fontScale: Float = 1.0f,          // 0.85f .. 1.25f
    val currencyUnit: String = "تومان",
    val distanceUnit: String = "کیلومتر",
    val weeklyWorkoutTarget: Int = 4,
    val notificationsEnabled: Boolean = true,
    val reducedMotion: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PRIMARY_COLOR = stringPreferencesKey("primary_color")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val CURRENCY_UNIT = stringPreferencesKey("currency_unit")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val WEEKLY_WORKOUT_TARGET = intPreferencesKey("weekly_workout_target")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        UserPreferencesState(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeModePreference.valueOf(it) }.getOrNull()
            } ?: ThemeModePreference.LIGHT,
            primaryColorKey = prefs[Keys.PRIMARY_COLOR] ?: "BLUE",
            fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.25f),
            currencyUnit = prefs[Keys.CURRENCY_UNIT] ?: "تومان",
            distanceUnit = prefs[Keys.DISTANCE_UNIT] ?: "کیلومتر",
            weeklyWorkoutTarget = (prefs[Keys.WEEKLY_WORKOUT_TARGET] ?: 4).coerceIn(1, 14),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false
        )
    }

    suspend fun updateThemeMode(mode: ThemeModePreference) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updatePrimaryColor(colorKey: String) {
        context.dataStore.edit { it[Keys.PRIMARY_COLOR] = colorKey }
    }

    suspend fun updateFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.25f) }
    }

    suspend fun updateCurrencyUnit(unit: String) {
        context.dataStore.edit { it[Keys.CURRENCY_UNIT] = unit }
    }

    suspend fun updateWeeklyWorkoutTarget(target: Int) {
        context.dataStore.edit { it[Keys.WEEKLY_WORKOUT_TARGET] = target.coerceIn(1, 14) }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateReducedMotion(reduced: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = reduced }
    }
}
