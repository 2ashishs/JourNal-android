package ash.app.journal.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ash.app.journal.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "journal_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferenceKeys {
        val SNOOZE_DURATION_MILLIS = longPreferencesKey("pref_snooze_duration_millis")
        val PIN_HASH = stringPreferencesKey("pref_pin_hash")
        val IS_PIN_ENABLED = booleanPreferencesKey("pref_is_pin_enabled")
    }

    // --- Snooze Preferences ---

    val snoozeDurationFlow: Flow<SnoozeDuration> = context.dataStore.data.map { prefs ->
        val durationMillis = prefs[PreferenceKeys.SNOOZE_DURATION_MILLIS] ?: SnoozeDuration.DEFAULT.durationMillis
        SnoozeDuration.fromMillis(durationMillis)
    }

    suspend fun getSnoozeDurationMillis(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[PreferenceKeys.SNOOZE_DURATION_MILLIS] ?: SnoozeDuration.DEFAULT.durationMillis
    }

    suspend fun setSnoozeDuration(duration: SnoozeDuration) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.SNOOZE_DURATION_MILLIS] = duration.durationMillis
        }
    }

    // --- Security / PIN Preferences ---

    val isPinSetFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val isEnabled = prefs[PreferenceKeys.IS_PIN_ENABLED] ?: false
        val hash = prefs[PreferenceKeys.PIN_HASH]
        isEnabled && !hash.isNullOrBlank()
    }

    suspend fun setPin(newPin: String) {
        val hash = SecurityUtils.hashPin(newPin)
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.PIN_HASH] = hash
            prefs[PreferenceKeys.IS_PIN_ENABLED] = true
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.PIN_HASH)
            prefs[PreferenceKeys.IS_PIN_ENABLED] = false
        }
    }

    suspend fun validatePin(enteredPin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[PreferenceKeys.PIN_HASH]
        return SecurityUtils.verifyPin(enteredPin, storedHash)
    }
}