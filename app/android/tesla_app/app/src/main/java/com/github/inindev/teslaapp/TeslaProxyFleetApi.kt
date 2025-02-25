package com.github.inindev.teslaapp

import android.util.Log
import kotlinx.coroutines.delay
import org.json.JSONObject

// handles vehicle-specific calls via the tesla http proxy
class TeslaProxyFleetApi(
    private val proxyUrl: String,
    private val vin: String,
    oauth2Client: OAuth2Client
) : TeslaFleetApiBase(oauth2Client) {

    override fun getBaseUrl(): String = proxyUrl

    // vehicle-specific commands
    suspend fun frontTrunk(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/actuate_trunk",
        body = "{\"which_trunk\": \"front\"}"
    )

    suspend fun rearTrunk(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/actuate_trunk",
        body = "{\"which_trunk\": \"rear\"}"
    )

    suspend fun climateOn(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/auto_conditioning_start"
    )

    suspend fun climateOff(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/auto_conditioning_stop"
    )

    suspend fun chargeClose(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/charge_port_door_close"
    )

    suspend fun chargeOpen(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/charge_port_door_open"
    )

    suspend fun lockDoors(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/door_lock"
    )

    suspend fun unlockDoors(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/door_unlock"
    )

    suspend fun flashLights(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/flash_lights"
    )

    suspend fun honkHorn(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/honk_horn"
    )

    suspend fun ventWindows(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/command/window_control",
        body = "{\"command\": \"vent\"}"
    )

    suspend fun wakeUp(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/$vin/wake_up"
    )

    suspend fun vehicle(): HttpResult = executeRequest(
        method = "GET",
        endpoint = "/api/1/vehicles/$vin"
    )

    suspend fun vehicleData(): HttpResult = executeRequest(
        method = "GET",
        endpoint = "/api/1/vehicles/$vin/vehicle_data"
    )

    suspend fun waitForVehicleOnline(maxRetries: Int = 10): HttpResult {
        for (retryCount in 0 until maxRetries) {
            try {
                val vehicleStatus = vehicle()
                if (vehicleStatus is HttpResult.Failure) {
                    Log.d("TeslaFleetApi", "Failed to retrieve vehicle status: HTTP ${vehicleStatus.statusCode}")
                    return vehicleStatus
                }

                val state = JSONObject((vehicleStatus as HttpResult.Success).data)
                    .getJSONObject("response")
                    .getString("state")

                if (state == "online") {
                    Log.d("TeslaFleetApi", "Vehicle came online after ${retryCount + 1} attempts")
                    return HttpResult.Success(200)
                }

                if (retryCount == 0) {
                    val wakeResult = wakeUp()
                    if (wakeResult is HttpResult.Failure) {
                        Log.d("TeslaFleetApi", "Failed to wake up the vehicle: HTTP ${wakeResult.statusCode}")
                        return wakeResult
                    }
                    delay(8000L) // start checking after 8+1 sec
                }

                delay(1000L)
            } catch (e: Exception) {
                Log.e("TeslaFleetApi", "Failed to parse vehicle state: ${e.message}")
                return HttpResult.Failure(500)
            }

            Log.d("TeslaFleetApi", "Vehicle not online yet. Retry attempt ${retryCount + 1}/$maxRetries")
        }

        Log.d("TeslaFleetApi", "Max retries reached. Vehicle did not come online.")
        return HttpResult.Failure(408)
    }
}
