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
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()
    private val fleetApi: TeslaFleetApi

    init {
        fleetApi = TeslaFleetApi(oauth2Client)

        val vin = secureStorage.retrieveVin()
        fleetApi.updateVehicleId(vin)

        val baseUrl = secureStorage.retrieveBaseUrl()
        fleetApi.updateBaseUrl(baseUrl)
    }

    fun setIsAuthenticating(value: Boolean) {
        viewModelScope.launch {
            _isAuthenticating.value = value
        }
    }

    private val _statusText = MutableStateFlow("Status: Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun updateStatusText(newStatus: String) {
        viewModelScope.launch {
            _statusText.value = newStatus
        }
    }


    fun lockDoors() {
        viewModelScope.launch {
            updateStatusText("Locking doors...")
            fleetApi.lockDoors { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Doors locked successfully")
                    is RestResult.Failure -> updateStatusText("Failed to lock doors: ${result.errorMessage}")
                }
            }
        }
    }

    fun unlockDoors() {
        viewModelScope.launch {
            updateStatusText("Unlocking doors...")
            fleetApi.unlockDoors { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Doors unlocked successfully")
                    is RestResult.Failure -> updateStatusText("Failed to unlock doors: ${result.errorMessage}")
                }
            }
        }
    }

    fun flashLights() {
        viewModelScope.launch {
            updateStatusText("Flashing lights...")
            fleetApi.flashLights { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Lights flashed successfully")
                    is RestResult.Failure -> updateStatusText("Failed to flash lights: ${result.errorMessage}")
                }
            }
        }
    }

    fun honkHorn() {
        viewModelScope.launch {
            updateStatusText("Honking horn...")
            fleetApi.honkHorn { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Horn honked successfully")
                    is RestResult.Failure -> updateStatusText("Failed to honk horn: ${result.errorMessage}")
                }
            }
        }
    }

    fun rearTrunk() {
        viewModelScope.launch {
            updateStatusText("Opening rear trunk...")
            fleetApi.rearTrunk { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Rear trunk opened successfully")
                    is RestResult.Failure -> updateStatusText("Failed to open rear trunk: ${result.errorMessage}")
                }
            }
        }
    }

    fun frontTrunk() {
        viewModelScope.launch {
            updateStatusText("Opening front trunk...")
            fleetApi.frontTrunk { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Front trunk opened successfully")
                    is RestResult.Failure -> updateStatusText("Failed to open front trunk: ${result.errorMessage}")
                }
            }
        }
    }

    fun climateOn() {
        viewModelScope.launch {
            updateStatusText("Turning climate on...")
            fleetApi.climateOn { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Climate activated successfully")
                    is RestResult.Failure -> updateStatusText("Failed to activate climate: ${result.errorMessage}")
                }
            }
        }
    }

    fun chargeClose() {
        viewModelScope.launch {
            updateStatusText("Closing charger door...")
            fleetApi.chargeClose { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Charger door closed successfully")
                    is RestResult.Failure -> updateStatusText("Failed to close charger door: ${result.errorMessage}")
                }
            }
        }
    }

    fun wake() {
        viewModelScope.launch {
            updateStatusText("Sending wake...")
            fleetApi.wake { result ->
                when (result) {
                    is RestResult.Success -> updateStatusText("Wakeup successful")
                    is RestResult.Failure -> updateStatusText("Failed to wake: ${result.errorMessage}")
                }
            }
        }
    }
}

