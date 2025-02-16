package com.github.inindev.teslaapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// tesla rest calls
// https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands
// https://developer.tesla.com/docs/fleet-api/getting-started/best-practices

class TeslaFleetApi(private val oauth2Client: OAuth2Client) {
    internal var vehicleId: String = ""
        set

    internal var baseUrl: String = ""
        set

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#door-lock
    suspend fun lockDoors(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/door_lock"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#door-unlock
    suspend fun unlockDoors(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/door_unlock"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#flash-lights
    suspend fun flashLights(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/flash_lights"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#honk-horn
    suspend fun honkHorn(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/honk_horn"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#actuate-trunk
    suspend fun rearTrunk(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/actuate_trunk",
        body = "{\"which_trunk\": \"rear\"}"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#actuate-trunk
    suspend fun frontTrunk(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/actuate_trunk",
        body = "{\"which_trunk\": \"front\"}"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#auto-conditioning-stop
    suspend fun climateOff(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/auto_conditioning_stop"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#auto-conditioning-start
    suspend fun climateOn(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/auto_conditioning_start"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#charge-port-door-close
    suspend fun chargeClose(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/charge_port_door_close"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands#charge-port-door-open
    suspend fun chargeOpen(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/command/charge_port_door_open"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-endpoints#wake-up
    suspend fun wake(): RestResult = executeRequest(
        method = "POST",
        url = "$baseUrl/api/1/vehicles/$vehicleId/wake_up"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-endpoints#vehicle
    suspend fun vehicle(): RestResult = executeRequest(
        method = "GET",
        url = "$baseUrl/api/1/vehicles/$vehicleId"
    )

    // https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-endpoints#vehicle-data
    suspend fun vehicleData(): RestResult = executeRequest(
        method = "GET",
        url = "$baseUrl/api/1/vehicles/$vehicleId/vehicle_data"
    )

    // process the request
    private val client = OkHttpClient()
    private suspend fun executeRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): RestResult = withContext(Dispatchers.IO) {
        val tag = "TeslaFleetApi"

        // default headers
        val defaultHeaders = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${oauth2Client.getAccessToken()}"
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
            RestResult.Success(responseBody)
        } else {
            Log.d(tag, "http failure code: ${response.code}")
            RestResult.Failure(response.code)
        }
    }
}

sealed class RestResult {
    data class Success(val data: String) : RestResult()
    data class Failure(val errorCode: Int?) : RestResult()
}
