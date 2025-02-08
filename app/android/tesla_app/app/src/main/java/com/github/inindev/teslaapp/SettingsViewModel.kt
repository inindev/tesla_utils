package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val secureStorage: SecureStorage) : ViewModel() {
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    data class Settings(
        val clientId: String = "",
        val clientSecret: String = "",
        val vin: String = "",
        val baseUrl: String = ""
    )

    init {
        loadSettings()
    }

    // settings management
    internal fun loadSettings() {
        viewModelScope.launch {
            _settings.value = Settings(
                clientId = secureStorage.retrieveClientId(),
                clientSecret = secureStorage.retrieveClientSecret(),
                vin = secureStorage.retrieveVin(),
                baseUrl = secureStorage.retrieveBaseUrl()
            )
        }
    }

    private fun saveSettings(settings: Settings) {
        secureStorage.storeClientId(settings.clientId)
        secureStorage.storeClientSecret(settings.clientSecret)
        secureStorage.storeVin(settings.vin)
        secureStorage.storeBaseUrl(settings.baseUrl)
    }

    private fun updateSetting(update: (Settings) -> Settings) {
        viewModelScope.launch {
            val newSettings = update(_settings.value)
            _settings.value = newSettings
            saveSettings(newSettings)
        }
    }

    // update methods
    fun updateClientId(newClientId: String) {
        updateSetting { it.copy(clientId = newClientId) }
    }

    fun updateClientSecret(newClientSecret: String) {
        updateSetting { it.copy(clientSecret = newClientSecret) }
    }

    fun updateVin(newVin: String) {
        updateSetting { it.copy(vin = newVin) }
    }

    fun updateBaseUrl(newBaseUrl: String) {
        updateSetting { it.copy(baseUrl = newBaseUrl) }
    }
}
