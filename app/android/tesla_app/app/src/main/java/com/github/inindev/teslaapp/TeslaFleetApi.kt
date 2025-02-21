package com.github.inindev.teslaapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// tesla rest calls
// https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands
// https://developer.tesla.com/docs/fleet-api/getting-started/best-practices

class TeslaFleetApi(
    private val settingsRepository: SettingsRepository,
    private val oauth2Client: OAuth2Client
) {
    private val tag = "TeslaFleetApi"

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#actuate-trunk
    suspend fun frontTrunk(): HttpResult = executeRequest(
        method   = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/actuate_trunk",
        body     = "{\"which_trunk\": \"front\"}"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#actuate-trunk
    suspend fun rearTrunk(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/actuate_trunk",
        body = "{\"which_trunk\": \"rear\"}"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#auto-conditioning-start
    suspend fun climateOn(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/auto_conditioning_start"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#auto-conditioning-stop
    suspend fun climateOff(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/auto_conditioning_stop"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#charge-port-door-close
    suspend fun chargeClose(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/charge_port_door_close"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#charge-port-door-open
    suspend fun chargeOpen(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/charge_port_door_open"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#door-lock
    suspend fun lockDoors(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/door_lock"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#door-unlock
    suspend fun unlockDoors(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/door_unlock"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#flash-lights
    suspend fun flashLights(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/flash_lights"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#honk-horn
    suspend fun honkHorn(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/honk_horn"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#window-control
    suspend fun ventWindows(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/command/window_control",
        body = "{\"command\": \"vent\"}"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-endpoints#wake-up
    suspend fun wakeUp(): HttpResult = executeRequest(
        method = "POST",
        endpoint = "/api/1/vehicles/{vehicleId}/wake_up"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-endpoints#vehicle
    suspend fun vehicle(): HttpResult = executeRequest(
        method = "GET",
        endpoint = "/api/1/vehicles/{vehicleId}"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-endpoints#vehicle-data
    suspend fun vehicleData(): HttpResult = executeRequest(
        method = "GET",
        endpoint = "/api/1/vehicles/{vehicleId}/vehicle_data"
    )

    // https://developer.tesla.com/docs/fleet-api/getting-started/best-practices#ensure-connectivity-state-before-interacting-with-a-device
    suspend fun waitForVehicleOnline(maxRetries: Int = 10): HttpResult {
        for (retryCount in 0 until maxRetries) {
            try {
                val vehicleStatus = vehicle()
                if (vehicleStatus is HttpResult.Failure) {
                    Log.d(tag, "Failed to retrieve vehicle status: HTTP ${vehicleStatus.statusCode}")
                    return vehicleStatus
                }

                val state = JSONObject((vehicleStatus as HttpResult.Success).data)
                    .getJSONObject("response")
                    .getString("state")

                if (state == "online") {
                    Log.d(tag, "Vehicle came online after ${retryCount + 1} attempts")
                    return HttpResult.Success(200)
                }

                if (retryCount == 0) {
                    val wakeResult = wakeUp()
                    if (wakeResult is HttpResult.Failure) {
                        Log.d(tag, "Failed to wake up the vehicle: HTTP ${wakeResult.statusCode}")
                        return wakeResult
                    }
                    delay(8000L) // start checking after 8+1 sec
                }

                delay(1000L)
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse vehicle state: ${e.message}")
                return HttpResult.Failure(500)
            }

            Log.d(tag, "Vehicle not online yet. Retry attempt ${retryCount + 1}/${maxRetries}")
        }

        Log.d(tag, "Max retries reached. Vehicle did not come online")
        return HttpResult.Failure(408)
    }

    // process the request
    private val client = OkHttpClient()
    private suspend fun executeRequest(
        method: String,
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): HttpResult = withContext(Dispatchers.IO) {
        val accessToken = oauth2Client.getAccessToken()
        if (accessToken == null) {
            Log.e(tag, "No valid token available for API call")
            return@withContext HttpResult.Failure(401) // unauthorized
        }

        // substitute {vehicleId} with the actual vehicleId
        val settings = settingsRepository.loadSettings()
        val baseUrl = settings.baseUrl
        val vehicleId = settings.vin
        val url = "$baseUrl${endpoint.replace("{vehicleId}", vehicleId)}"

        // default headers
        val defaultHeaders = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $accessToken"
        )

        // merge headers: allow provided headers to override defaults
        val fullHeaders = defaultHeaders.toMutableMap().apply {
            headers.forEach { (key, value) ->
                this[key] = value
            }
        }

        Log.d(tag, "Sending request: $method to $url")
        Log.d(tag, "Headers: $fullHeaders")
        if (body != null) Log.d(tag, "Body: $body")

        val request = Request.Builder()
            .url(url)
            .apply {
                fullHeaders.forEach { (k, v) -> addHeader(k, v) }
                when (method.uppercase()) {
                    "GET" -> get()
                    "POST" -> post(
                        (body ?: "").toRequestBody("application/json".toMediaType())
                    )
                    else -> throw IllegalArgumentException("Unsupported method: $method")
                }
            }
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            Log.d(tag, "http success code: ${response.code} body: $responseBody")
            HttpResult.Success(response.code, responseBody)
        } else {
            Log.d(tag, "http failure code: ${response.code}")
            HttpResult.Failure(response.code)
        }
    }
}

sealed class HttpResult {
    data class Success(val statusCode: Int, val data: String = "") : HttpResult()
    data class Failure(val statusCode: Int) : HttpResult()
}
