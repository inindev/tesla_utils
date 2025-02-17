package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val secureStorage: SecureStorage,
    private val oauth2Client: OAuth2Client
) : ViewModel() {
    private val fleetApi: TeslaFleetApi = TeslaFleetApi(oauth2Client)

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _statusText = MutableStateFlow("Status: Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _jsonContent = MutableStateFlow("")
    val jsonContent: StateFlow<String> = _jsonContent.asStateFlow()

    init {
        val vin = secureStorage.retrieveVin()
        val baseUrl = secureStorage.retrieveBaseUrl()
        fleetApi.apply {
            this.vehicleId = vin
            this.baseUrl = baseUrl
        }
    }

    fun setIsAuthenticating(value: Boolean) {
        viewModelScope.launch { _isAuthenticating.value = value }
    }

    fun updateStatusText(newStatus: String) {
        viewModelScope.launch { _statusText.value = newStatus }
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

    fun rearTrunk() {
        updateStatusText("Opening rear trunk...")
        execApiCmd(
            operation = { fleetApi.rearTrunk() },
            onSuccess = { updateStatusText("Rear trunk opened successfully") },
            onFailure = { updateStatusText("Failed to open rear trunk: $it") }
        )
    }

    fun frontTrunk() {
        updateStatusText("Opening front trunk...")
        execApiCmd(
            operation = { fleetApi.frontTrunk() },
            onSuccess = { updateStatusText("Front trunk opened successfully") },
            onFailure = { updateStatusText("Failed to open front trunk: $it") }
        )
    }

    fun climateOn() {
        updateStatusText("Turning climate on...")
        execApiCmd(
            operation = { fleetApi.climateOn() },
            onSuccess = { updateStatusText("Climate activated successfully") },
            onFailure = { updateStatusText("Failed to activate climate: $it") }
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
        updateStatusText("Fetching vehicle...")
        execApiCmd(
            operation = { fleetApi.vehicle() },
            onSuccess = { data ->
                _jsonContent.value = prettyPrint(data)
                updateStatusText("Vehicle fetch successful")
            },
            onFailure = { updateStatusText("Failed to fetch vehicle: $it") },
            false
        )
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
                        onFailure("HTTP ${result.statusCode}")
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
