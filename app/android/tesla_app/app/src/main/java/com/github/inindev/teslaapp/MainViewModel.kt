package com.github.inindev.teslaapp

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val oauth2Client: OAuth2Client
) : ViewModel() {
    private val directApi = TeslaDirectFleetApi(oauth2Client)
    private var proxyApi: TeslaProxyFleetApi? = null // initialized only when a vehicle is selected

    private val _settingsValid = MutableStateFlow(false)
    val settingsValid: StateFlow<Boolean> = _settingsValid.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _statusText = MutableStateFlow("Status: Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _jsonContent = MutableStateFlow("")
    val jsonContent: StateFlow<String> = _jsonContent.asStateFlow()

    // vehicle data class
    data class Vehicle(
        val id: Long,
        val vin: String,
        val displayName: String
    )

    init {
        fetchVehicles()
    }

    // update selected vehicle and create proxy api
    fun selectVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            _selectedVehicle.value = vehicle
            val settings = settingsRepository.loadSettings()
            proxyApi = TeslaProxyFleetApi(settings.proxyUrl, vehicle.vin, oauth2Client)
            updateStatusText("Selected vehicle: ${vehicle.displayName}: ${vehicle.vin}")
        }
    }

    // update proxy url and recreate proxy api if a vehicle is selected
    fun updateProxyUrl(proxyUrl: String) {
        viewModelScope.launch {
            val currentVehicle = _selectedVehicle.value
            if (currentVehicle != null) {
                proxyApi = TeslaProxyFleetApi(proxyUrl, currentVehicle.vin, oauth2Client)
                updateStatusText("Proxy URL updated for vehicle: ${currentVehicle.displayName}")
            }
        }
    }

    fun frontTrunk() {
        execApiCmd(
            preFlight = { updateStatusText("Opening front trunk...") },
            operation = { proxyApi!!.frontTrunk() },
            onSuccess = { updateStatusText("Front trunk opened successfully") },
            onFailure = { updateStatusText("Failed to open front trunk: $it") }
        )
    }

    fun rearTrunk() {
        execApiCmd(
            preFlight = { updateStatusText("Opening rear trunk...") },
            operation = { proxyApi!!.rearTrunk() },
            onSuccess = { updateStatusText("Rear trunk opened successfully") },
            onFailure = { updateStatusText("Failed to open rear trunk: $it") }
        )
    }

    fun climateOn() {
        execApiCmd(
            preFlight = { updateStatusText("Turning climate on...") },
            operation = { proxyApi!!.climateOn() },
            onSuccess = { updateStatusText("Climate turned on successfully") },
            onFailure = { updateStatusText("Failed to turn on climate: $it") }
        )
    }

    fun climateOff() {
        execApiCmd(
            preFlight = { updateStatusText("Turning climate off...") },
            operation = { proxyApi!!.climateOff() },
            onSuccess = { updateStatusText("Climate turned off successfully") },
            onFailure = { updateStatusText("Failed to turn off climate: $it") }
        )
    }

    fun chargeClose() {
        execApiCmd(
            preFlight = { updateStatusText("Closing charger door...") },
            operation = { proxyApi!!.chargeClose() },
            onSuccess = { updateStatusText("Charger door closed successfully") },
            onFailure = { updateStatusText("Failed to close charger door: $it") }
        )
    }

    fun chargeOpen() {
        execApiCmd(
            preFlight = { updateStatusText("Opening charger door...") },
            operation = { proxyApi!!.chargeOpen() },
            onSuccess = { updateStatusText("Charger door opened successfully") },
            onFailure = { updateStatusText("Failed to open charger door: $it") }
        )
    }

    fun lockDoors() {
        execApiCmd(
            preFlight = { updateStatusText("Locking doors...") },
            operation = { proxyApi!!.lockDoors() },
            onSuccess = { updateStatusText("Doors locked successfully") },
            onFailure = { updateStatusText("Failed to lock doors: $it") }
        )
    }

    fun unlockDoors() {
        execApiCmd(
            preFlight = { updateStatusText("Unlocking doors...") },
            operation = { proxyApi!!.unlockDoors() },
            onSuccess = { updateStatusText("Doors unlocked successfully") },
            onFailure = { updateStatusText("Failed to unlock doors: $it") }
        )
    }

    fun flashLights() {
        execApiCmd(
            preFlight = { updateStatusText("Flashing lights...") },
            operation = { proxyApi!!.flashLights() },
            onSuccess = { updateStatusText("Lights flashed successfully") },
            onFailure = { updateStatusText("Failed to flash lights: $it") }
        )
    }

    fun honkHorn() {
        execApiCmd(
            preFlight = { updateStatusText("Honking horn...") },
            operation = { proxyApi!!.honkHorn() },
            onSuccess = { updateStatusText("Horn honked successfully") },
            onFailure = { updateStatusText("Failed to honk horn: $it") }
        )
    }

    fun ventWindows() {
        execApiCmd(
            preFlight = { updateStatusText("Venting windows...") },
            operation = { proxyApi!!.ventWindows() },
            onSuccess = { updateStatusText("Windows vented successfully") },
            onFailure = { updateStatusText("Failed to vent windows: $it") }
        )
    }

    fun wakeUp() {
        execApiCmd(
            preFlight = { updateStatusText("Sending wake up...") },
            operation = { proxyApi!!.wakeUp() },
            onSuccess = { updateStatusText("Wake up successful") },
            onFailure = { updateStatusText("Failed to wake up: $it") },
            requiresOnlineVehicle = false
        )
    }

    fun vehicle() {
        execApiCmd(
            preFlight = {
                _jsonContent.value = ""
                updateStatusText("Fetching vehicle info...")
            },
            operation = { proxyApi!!.vehicle() },
            onSuccess = { data ->
                _jsonContent.value = prettyPrint(data)
                updateStatusText("Vehicle info fetch successful")
            },
            onFailure = { updateStatusText("Failed to fetch vehicle info: $it") },
            requiresOnlineVehicle = false
        )
    }

    fun vehicleData() {
        execApiCmd(
            preFlight = {
                _jsonContent.value = ""
                updateStatusText("Fetching vehicle infoEx...")
            },
            operation = { proxyApi!!.vehicleData() },
            onSuccess = { data ->
                _jsonContent.value = prettyPrint(data)
                updateStatusText("Vehicle infoEx fetch successful")
            },
            onFailure = { updateStatusText("Failed to fetch vehicle infoEx: $it") }
        )
    }

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

    // execute api command with pre-action
    private fun execApiCmd(
        preFlight: () -> Unit = {},
        operation: suspend () -> HttpResult,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        requiresOnlineVehicle: Boolean = true
    ) {
        if (proxyApi == null) {
            updateStatusText("No vehicle selected")
            return
        }
        preFlight()
        viewModelScope.launch {
            try {
                if (requiresOnlineVehicle) {
                    val onlineStatus = proxyApi!!.waitForVehicleOnline()
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
                                updateStatusText("Token unavailable. Please re-authenticate in settings.")
                                _snackbarMessage.value = "Authentication failed. Please re-authenticate."
                            }
                            404 -> updateStatusText("Vehicle not found. Check selected vehicle.")
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

    // fetch vehicle list from direct api, independent of execApiCmd
    fun fetchVehicles() {
        viewModelScope.launch {
            updateStatusText("Fetching vehicle list...")
            try {
                when (val result = directApi.listVehicles()) {
                    is HttpResult.Success -> {
                        val json = JSONObject(result.data).getJSONArray("response")
                        val vehicleList = (0 until json.length()).map { i ->
                            val vehicle = json.getJSONObject(i)
                            Vehicle(
                                id = vehicle.getLong("id"),
                                vin = vehicle.getString("vin"),
                                displayName = vehicle.getString("display_name")
                            )
                        }
                        _vehicles.value = vehicleList
                        vehicleList.firstOrNull()?.let { selectVehicle(it) }
                        updateStatusText("Vehicle list fetched successfully")
                    }
                    is HttpResult.Failure -> {
                        when (result.statusCode) {
                            401 -> {
                                updateStatusText("Token unavailable. please re-authenticate in settings.")
                                _snackbarMessage.value = "Authentication failed. please re-authenticate."
                            }
                            404 -> updateStatusText("No vehicles found")
                            else -> updateStatusText("Failed to fetch vehicles: HTTP ${result.statusCode}")
                        }
                    }
                }
            } catch (e: Exception) {
                updateStatusText("unexpected error fetching vehicles: ${e.message ?: "error occurred"}")
            }
        }
    }

    // format json string for display
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
