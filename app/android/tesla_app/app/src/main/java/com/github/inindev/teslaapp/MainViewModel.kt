package com.github.inindev.teslaapp

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val oauth2Client: OAuth2Client,
) : ViewModel() {
    private val fleetApi: TeslaFleetApi = TeslaFleetApi(settingsRepository, oauth2Client)

    private val _settingsValid = MutableStateFlow(false)
    val settingsValid: StateFlow<Boolean> = _settingsValid.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _statusText = MutableStateFlow("Status: Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _jsonContent = MutableStateFlow("")
    val jsonContent: StateFlow<String> = _jsonContent.asStateFlow()

    fun setIsAuthenticating(value: Boolean) {
        viewModelScope.launch { _isAuthenticating.value = value }
    }

    fun updateSettingsValid(isValid: Boolean) {
        viewModelScope.launch { _settingsValid.value = isValid }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun updateStatusText(newStatus: String) {
        viewModelScope.launch { _statusText.value = newStatus }
    }

    fun frontTrunk() {
        updateStatusText("Opening front trunk...")
        execApiCmd(
            operation = { fleetApi.frontTrunk() },
            onSuccess = { updateStatusText("Front trunk opened successfully") },
            onFailure = { updateStatusText("Failed to open front trunk: $it") }
        )
    }

    fun rearTrunk() {
        updateStatusText("Opening rear trunk...")
        execApiCmd(
            operation = { fleetApi.rearTrunk() },
            onSuccess = { updateStatusText("Rear trunk opened successfully") },
            onFailure = { updateStatusText("Failed to open rear trunk: $it") }
        )
    }

    fun climateOn() {
        updateStatusText("Turning climate on...")
        execApiCmd(
            operation = { fleetApi.climateOn() },
            onSuccess = { updateStatusText("Climate turned on successfully") },
            onFailure = { updateStatusText("Failed to turn on climate: $it") }
        )
    }

    fun climateOff() {
        updateStatusText("Turning climate off...")
        execApiCmd(
            operation = { fleetApi.climateOff() },
            onSuccess = { updateStatusText("Climate turned off successfully") },
            onFailure = { updateStatusText("Failed to turn off climate: $it") }
        )
    }

    fun chargeClose() {
        updateStatusText("Closing charger door...")
        execApiCmd(
            operation = { fleetApi.chargeClose() },
            onSuccess = { updateStatusText("Charger door closed successfully") },
            onFailure = { updateStatusText("Failed to close charger door: $it") }
        )
    }

    fun chargeOpen() {
        updateStatusText("Opening charger door...")
        execApiCmd(
            operation = { fleetApi.chargeOpen() },
            onSuccess = { updateStatusText("Charger door opened successfully") },
            onFailure = { updateStatusText("Failed to open charger door: $it") }
        )
    }

    fun lockDoors() {
        updateStatusText("Locking doors...")
        execApiCmd(
            operation = { fleetApi.lockDoors() },
            onSuccess = { updateStatusText("Doors locked successfully") },
            onFailure = { updateStatusText("Failed to lock doors: $it") }
        )
    }

    fun unlockDoors() {
        updateStatusText("Unlocking doors...")
        execApiCmd(
            operation = { fleetApi.unlockDoors() },
            onSuccess = { updateStatusText("Doors unlocked successfully") },
            onFailure = { updateStatusText("Failed to unlock doors: $it") }
        )
    }

    fun flashLights() {
        updateStatusText("Flashing lights...")
        execApiCmd(
            operation = { fleetApi.flashLights() },
            onSuccess = { updateStatusText("Lights flashed successfully") },
            onFailure = { updateStatusText("Failed to flash lights: $it") }
        )
    }

    fun honkHorn() {
        updateStatusText("Honking horn...")
        execApiCmd(
            operation = { fleetApi.honkHorn() },
            onSuccess = { updateStatusText("Horn honked successfully") },
            onFailure = { updateStatusText("Failed to honk horn: $it") }
        )
    }

    fun ventWindows() {
        updateStatusText("Venting windows...")
        execApiCmd(
            operation = { fleetApi.ventWindows() },
            onSuccess = { updateStatusText("Windows vented successfully") },
            onFailure = { updateStatusText("Failed to vent windows: $it") }
        )
    }

    fun wakeUp() {
        updateStatusText("Sending wake up...")
        execApiCmd(
            operation = { fleetApi.wakeUp() },
            onSuccess = { updateStatusText("Wake up successful") },
            onFailure = { updateStatusText("Failed to wake up: $it") },
            false
        )
    }

    fun vehicle() {
        _jsonContent.value = ""
        updateStatusText("Fetching vehicle info...")
        execApiCmd(
            operation = { fleetApi.vehicle() },
            onSuccess = { data ->
                _jsonContent.value = prettyPrint(data)
                updateStatusText("Vehicle info fetch successful")
            },
            onFailure = { updateStatusText("Failed to fetch vehicle info: $it") },
            false
        )
    }

    fun vehicleData() {
        _jsonContent.value = ""
        updateStatusText("Fetching vehicle infoEx...")
        execApiCmd(
            operation = { fleetApi.vehicleData() },
            onSuccess = { data ->
                _jsonContent.value = prettyPrint(data)
                updateStatusText("Vehicle infoEx fetch successful")
            },
            onFailure = { updateStatusText("Failed to fetch vehicle infoEx: $it") }
        )
    }

    fun initiateAuthFlow(context: Context) {
        viewModelScope.launch {
            setIsAuthenticating(true)
            try {
                updateStatusText("Authentication process started...")
                val authUri = oauth2Client.initiateAuthFlow()
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(context, authUri)
            } catch (e: Exception) {
                updateStatusText("Authentication failed: ${e.message}")
                setIsAuthenticating(false)
            }
        }
    }

    private fun execApiCmd(
        operation: suspend () -> HttpResult,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        requiresOnlineVehicle: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                if (requiresOnlineVehicle) {
                    val onlineStatus = fleetApi.waitForVehicleOnline()
                    if (onlineStatus is HttpResult.Failure) {
                        onFailure("Failed to ensure vehicle is online: HTTP ${onlineStatus.statusCode}")
                        return@launch
                    }
                }

                when (val result = operation()) {
                    is HttpResult.Success -> {
                        onSuccess(result.data)
                    }
                    is HttpResult.Failure -> {
                        when (result.statusCode) {
                            401 -> {
                                updateStatusText("Token unavailable. Please re-authenticate in Settings.")
                                _snackbarMessage.value = "Authentication failed. Please re-authenticate."
                            }
                            404 -> updateStatusText("Vehicle not found. Check VIN in Settings.")
                            408 -> updateStatusText("Vehicle offline. Try waking it up.")
                            else -> onFailure("HTTP ${result.statusCode}")
                        }
                    }
                }
            } catch (e: Exception) {
                onFailure("Unexpected error: ${e.message ?: "Error occurred"}")
            }
        }
    }

    private fun prettyPrint(jsonString: String): String {
        var indentLevel = 0
        val result = StringBuilder()
        for (i in jsonString.indices) {
            when (val char = jsonString[i]) {
                '{', '[' -> {
                    result.append(char).append('\n')
                    indentLevel++
                    repeat(indentLevel) { result.append("   ") }
                }
                '}', ']' -> {
                    result.append('\n')
                    indentLevel--
                    repeat(indentLevel) { result.append("   ") }
                    result.append(char)
                }
                ',' -> {
                    result.append(char).append('\n')
                    repeat(indentLevel) { result.append("   ") }
                }
                ':' -> result.append(char).append(' ')
                else -> result.append(char)
            }
        }
        return result.toString()
    }
}
