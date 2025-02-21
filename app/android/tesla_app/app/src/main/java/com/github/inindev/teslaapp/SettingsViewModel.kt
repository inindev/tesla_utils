package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val settingsValidator = SettingsValidator()
    private val _settingsState = MutableStateFlow(Settings())
    val settingsState: StateFlow<Settings> = _settingsState.asStateFlow()

    data class Settings(
        val vin: String = "",
        val baseUrl: String = "",
        val clientId: String = "",
        val clientSecret: String = ""
    )

    private val _vinValidationState = MutableStateFlow(SettingsValidator.ValidationState.EMPTY)
    val vinValidationState: StateFlow<SettingsValidator.ValidationState> = _vinValidationState

    private val _baseUrlValidationState = MutableStateFlow(SettingsValidator.ValidationState.EMPTY)
    val baseUrlValidationState: StateFlow<SettingsValidator.ValidationState> = _baseUrlValidationState

    private val _clientIdValidationState = MutableStateFlow(SettingsValidator.ValidationState.EMPTY)
    val clientIdValidationState: StateFlow<SettingsValidator.ValidationState> = _clientIdValidationState.asStateFlow()

    private val _clientSecretValidationState = MutableStateFlow(SettingsValidator.ValidationState.EMPTY)
    val clientSecretValidationState: StateFlow<SettingsValidator.ValidationState> = _clientSecretValidationState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            val loadedSettings = repository.loadSettings()
            _settingsState.value = loadedSettings
            validateAllFields(loadedSettings)
        }
    }

    private fun saveSettings(settings: Settings) {
        repository.saveSettings(settings)
        validateAllFields(settings)
    }

    private fun validateAllFields(settings: Settings) {
        _vinValidationState.value = settingsValidator.isValidVin(settings.vin)
        _baseUrlValidationState.value = settingsValidator.isValidBaseUrl(settings.baseUrl)
        _clientIdValidationState.value = settingsValidator.isValidUuid(settings.clientId)
        _clientSecretValidationState.value = settingsValidator.isValidClientSecret(settings.clientSecret)
    }

    private fun updateSetting(update: (Settings) -> Settings) {
        viewModelScope.launch {
            val newSettings = update(_settingsState.value)
            _settingsState.value = newSettings
            saveSettings(newSettings)
        }
    }

    fun updateVin(newVin: String) = updateSetting { it.copy(vin = newVin) }
    fun updateBaseUrl(newBaseUrl: String) = updateSetting { it.copy(baseUrl = newBaseUrl) }
    fun updateClientId(newClientId: String) = updateSetting { it.copy(clientId = newClientId) }
    fun updateClientSecret(newClientSecret: String) = updateSetting { it.copy(clientSecret = newClientSecret) }
}
