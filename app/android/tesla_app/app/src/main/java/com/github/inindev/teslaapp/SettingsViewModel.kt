package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

class SettingsViewModel(private val secureStorage: SecureStorage) : ViewModel() {
    private val _settingsState = MutableStateFlow(Settings())
    val settingsState: StateFlow<Settings> = _settingsState.asStateFlow()

    data class Settings(
        val clientId: String = "",
        val clientSecret: String = "",
        val vin: String = "",
        val baseUrl: String = ""
    )

    private val _clientIdValidationState = MutableStateFlow(ValidationState.EMPTY)
    val clientIdValidationState: StateFlow<ValidationState> = _clientIdValidationState.asStateFlow()

    private val _clientSecretValidationState = MutableStateFlow(ValidationState.EMPTY)
    val clientSecretValidationState: StateFlow<ValidationState> = _clientSecretValidationState.asStateFlow()

    init {
        loadSettings()
    }

    // settings management
    internal fun loadSettings() {
        viewModelScope.launch {
            val loadedSettings = Settings(
                clientId = secureStorage.retrieveClientId(),
                clientSecret = secureStorage.retrieveClientSecret(),
                vin = secureStorage.retrieveVin(),
                baseUrl = secureStorage.retrieveBaseUrl()
            )
            _settingsState.value = loadedSettings
            _clientIdValidationState.value = isValidUuid(loadedSettings.clientId)
            _clientSecretValidationState.value = isValidClientSecret(loadedSettings.clientSecret)
        }
    }

    private fun saveSettings(settings: Settings) {
        secureStorage.storeClientId(settings.clientId)
        secureStorage.storeClientSecret(settings.clientSecret)
        secureStorage.storeVin(settings.vin)
        secureStorage.storeBaseUrl(settings.baseUrl)
        _clientIdValidationState.value = isValidUuid(settings.clientId)
        _clientSecretValidationState.value = isValidClientSecret(settings.clientSecret)
    }

    private fun updateSetting(update: (Settings) -> Settings) {
        viewModelScope.launch {
            val newSettings = update(_settingsState.value)
            _settingsState.value = newSettings
            saveSettings(newSettings)
        }
    }

    // update methods
    fun updateClientId(newClientId: String) {
        updateSetting { it.copy(clientId = newClientId) }
        _clientIdValidationState.value = isValidUuid(newClientId)
    }

    fun updateClientSecret(newClientSecret: String) {
        updateSetting { it.copy(clientSecret = newClientSecret) }
        _clientSecretValidationState.value = isValidClientSecret(newClientSecret)
    }

    fun updateVin(newVin: String) {
        updateSetting { it.copy(vin = newVin) }
    }

    fun updateBaseUrl(newBaseUrl: String) {
        updateSetting { it.copy(baseUrl = newBaseUrl) }
    }

    enum class ValidationState {
        EMPTY,
        INVALID,
        VALID_BUT_INCOMPLETE,
        VALID
    }

    private fun isValidUuid(uuid: String): ValidationState {
        if (uuid.isEmpty()) return ValidationState.EMPTY

        // check for fully valid uuid
        if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex())) return ValidationState.VALID

        // check overall length and character set for partial validation
        if (uuid.length > 36 || !uuid.matches("^[0-9a-fA-F-]+$".toRegex())) return ValidationState.INVALID

        // layered validation based on the length of the string for partial validation
        when (uuid.length) {
            in 1..8 -> if (uuid.matches("^[0-9a-fA-F]{1,8}$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            9 -> if (uuid.matches("^[0-9a-fA-F]{8}-$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            in 10..13 -> if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{1,${uuid.length - 9}}$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            14 -> if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            in 15..18 -> if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{1,${uuid.length - 14}}$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            19 -> if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            in 20..23 -> if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{1,${uuid.length - 19}}$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            24 -> if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
            in 25..36 -> if (uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{1,${min(uuid.length - 24, 12)}}$".toRegex())) return ValidationState.VALID_BUT_INCOMPLETE
        }

        return ValidationState.INVALID
    }

    private fun isValidClientSecret(secret: String): ValidationState {
        if (secret.isEmpty()) return ValidationState.EMPTY

        // define the expected sequence
        val expectedSequence = "ta-secret."

        // check if the input matches the start of the expected sequence
        if (secret.length <= expectedSequence.length && expectedSequence.startsWith(secret)) {
            return ValidationState.VALID_BUT_INCOMPLETE // partial match
        }

        if (secret.length < 26) return ValidationState.VALID_BUT_INCOMPLETE
        if (secret.length == 26) return ValidationState.VALID

        // if it doesn't match any of the above, it's invalid
        return ValidationState.INVALID
    }
}
