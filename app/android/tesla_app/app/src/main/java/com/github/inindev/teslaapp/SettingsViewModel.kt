package com.github.inindev.teslaapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val secureStorage: SecureStorage) : ViewModel() {
    private val _settingsState = MutableStateFlow(Settings())
    val settingsState: StateFlow<Settings> = _settingsState.asStateFlow()

    data class Settings(
        val vin: String = "",
        val baseUrl: String = "",
        val clientId: String = "",
        val clientSecret: String = ""
    )

    private val _vinValidationState = MutableStateFlow(ValidationState.EMPTY)
    val vinValidationState: StateFlow<ValidationState> = _vinValidationState

    private val _baseUrlValidationState = MutableStateFlow(ValidationState.EMPTY)
    val baseUrlValidationState: StateFlow<ValidationState> = _baseUrlValidationState

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
                vin = secureStorage.retrieveVin(),
                baseUrl = secureStorage.retrieveBaseUrl(),
                clientId = secureStorage.retrieveClientId(),
                clientSecret = secureStorage.retrieveClientSecret()
            )
            _settingsState.value = loadedSettings
            _clientIdValidationState.value = isValidUuid(loadedSettings.clientId)
            _clientSecretValidationState.value = isValidClientSecret(loadedSettings.clientSecret)
        }
    }

    private fun saveSettings(settings: Settings) {
        secureStorage.storeVin(settings.vin)
        secureStorage.storeBaseUrl(settings.baseUrl)
        secureStorage.storeClientId(settings.clientId)
        secureStorage.storeClientSecret(settings.clientSecret)
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
    fun updateVin(newVin: String) {
        updateSetting { it.copy(vin = newVin) }
        _vinValidationState.value = isValidVin(newVin)
    }

    fun updateBaseUrl(newBaseUrl: String) {
        updateSetting { it.copy(baseUrl = newBaseUrl) }
        _baseUrlValidationState.value = isValidBaseUrl(newBaseUrl)
    }

    fun updateClientId(newClientId: String) {
        updateSetting { it.copy(clientId = newClientId) }
        _clientIdValidationState.value = isValidUuid(newClientId)
    }

    fun updateClientSecret(newClientSecret: String) {
        updateSetting { it.copy(clientSecret = newClientSecret) }
        _clientSecretValidationState.value = isValidClientSecret(newClientSecret)
    }

    enum class ValidationState {
        EMPTY,
        INVALID,
        VALID_BUT_INCOMPLETE,
        VALID
    }

    private fun isValidVin(vin: String): ValidationState {
        Log.d("VINValidation", "VIN being validated: $vin")

        if (vin.isEmpty()) {
            Log.d("VINValidation", "VIN is empty")
            return ValidationState.EMPTY
        }

        val validChars = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"
        if (!vin.all { it in validChars }) {
            Log.d("VINValidation", "VIN contains invalid characters")
            return ValidationState.INVALID
        }

        if (vin.length < 17) {
            Log.d("VINValidation", "VIN length less than 17: ${vin.length}")
            return ValidationState.VALID_BUT_INCOMPLETE
        }

        // convert character to its numeric value for check digit calculation
        fun transliterate(c: Char): Int {
            val lookup = "0123456789.ABCDEFGH..JKLMN.P.R..STUVWXYZ"
            val value = lookup.indexOf(c) % 10
            Log.d("VINValidation", "Character $c transliterated to value: $value")
            return value
        }

        // calculate the check digit
        val weights = "8765432X098765432" // 'X' represents 10
        val map = "0123456789X"
        var sum = 0
        for (i in 0 until 17) {
            val value = transliterate(vin[i])
            val weight = if (weights[i] == 'X') 10 else weights[i] - '0'
            Log.d("VINValidation", "Index $i: Value = $value, Weight = $weight")
            sum += value * weight
        }
        Log.d("VINValidation", "Sum calculated for check digit: $sum")

        // the check digit (9th character) is calculated
        val calculatedCheckDigit = map[sum % 11].also {
            Log.d("VINValidation", "Check digit calculated as $it")
        }
        // compare calculated check digit with the one in the VIN
        if (calculatedCheckDigit != vin[8]) {
            Log.d("VINValidation", "VIN is INVALID. Calculated check digit: $calculatedCheckDigit, Actual: ${vin[8]}")
            ValidationState.INVALID
        }

        Log.d("VINValidation", "VIN is VALID")
        return ValidationState.VALID
    }

    private fun isValidBaseUrl(url: String): ValidationState {
        if (url.isEmpty()) return ValidationState.EMPTY
        if (!url.startsWith("https://")) return ValidationState.INVALID
        val hostRegex = Regex("^https://([a-zA-Z0-9.-]+)(/.*)?$")
        return if (hostRegex.matches(url)) ValidationState.VALID else ValidationState.INVALID
    }

    private fun isValidUuid(uuid: String): ValidationState {
        if (uuid.isEmpty()) return ValidationState.EMPTY

        val tokens = uuid.split("-")
        if (tokens.size > 5) return ValidationState.INVALID

        val expectedLengths = intArrayOf(8, 4, 4, 4, 12)
        val hexRegex = Regex("^[0-9a-f]*$", RegexOption.IGNORE_CASE)

        var prevTokenFull = true
        for (i in tokens.indices) {
            if (!prevTokenFull) return ValidationState.INVALID

            if (tokens[i].length > expectedLengths[i] || !hexRegex.matches(tokens[i])) {
                return ValidationState.INVALID
            }

            prevTokenFull = (tokens[i].length == expectedLengths[i])
        }

        return if (uuid.length == 36) ValidationState.VALID else ValidationState.VALID_BUT_INCOMPLETE
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

    fun validateSettings(): Boolean {
        val currentSettings = _settingsState.value
        val vinValid = isValidVin(currentSettings.vin)
        val baseUrlValid = isValidBaseUrl(currentSettings.baseUrl)
        val clientIdValid = isValidUuid(currentSettings.clientId)
        val clientSecretValid = isValidClientSecret(currentSettings.clientSecret)

        // ValidationState.VALID is the only state we consider as valid
        return vinValid == ValidationState.VALID &&
                baseUrlValid == ValidationState.VALID &&
                clientIdValid == ValidationState.VALID &&
                clientSecretValid == ValidationState.VALID
    }
}
