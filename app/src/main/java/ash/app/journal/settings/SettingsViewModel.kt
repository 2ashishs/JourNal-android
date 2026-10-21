package ash.app.journal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ash.app.journal.settings.SettingsDataStore
import ash.app.journal.settings.SnoozeDuration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val snoozeDuration: StateFlow<SnoozeDuration> = settingsDataStore.snoozeDurationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SnoozeDuration.DEFAULT
        )

    val isPinSet: StateFlow<Boolean> = settingsDataStore.isPinSetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onSnoozeDurationSelected(duration: SnoozeDuration) {
        viewModelScope.launch {
            settingsDataStore.setSnoozeDuration(duration)
        }
    }

    fun onSetPin(pin: String) {
        viewModelScope.launch {
            settingsDataStore.setPin(pin)
        }
    }

    fun onRemovePin() {
        viewModelScope.launch {
            settingsDataStore.disablePin()
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return settingsDataStore.validatePin(pin)
    }
}
