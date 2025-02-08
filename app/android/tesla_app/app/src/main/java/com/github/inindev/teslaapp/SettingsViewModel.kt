package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    data class AppSettings(
        var clientId: String = "",
        var clientSecret: String = "",
        var vin: String = "",
        var baseUrl: String = "",
    )

    fun updateClientId(newClientId: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(clientId = newClientId)
            saveSettings()
        }
    }

    fun updateClientSecret(newClientSecret: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(clientSecret = newClientSecret)
            saveSettings()
        }
    }

    fun updateVin(newVin: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(vin = newVin)
            saveSettings()
        }
    }

    fun updateBaseUrl(newBaseUrl: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(baseUrl = newBaseUrl)
            saveSettings()
        }
    }

    // function to load settings
    fun loadSettings() {
        viewModelScope.launch {
            // load settings from encrypted persistent storage
        }
    }

    // function to save settings
    private fun saveSettings() {
        // save settings to encrypted persistent storage
    }
}
