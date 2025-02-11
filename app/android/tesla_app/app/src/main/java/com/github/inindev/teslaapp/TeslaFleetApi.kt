package com.github.inindev.teslaapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

// https://developer.tesla.com/docs/fleet-api/endpoints/vehicle-commands

class TeslaFleetApi(private val oauth2Client: OAuth2Client) {
    private var vehicleId: String = ""
    private var baseUrl: String = ""

    fun updateVehicleId(newVehicleId: String) {
        this.vehicleId = newVehicleId
    }

    fun updateBaseUrl(newBaseUrl: String) {
        this.baseUrl = newBaseUrl
    }

    suspend fun lockDoors(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/door_lock",
            callback = callback
        )
    }

    suspend fun unlockDoors(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/door_unlock",
            callback = callback
        )
    }

    suspend fun flashLights(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/flash_lights",
            callback = callback
        )
    }

    suspend fun honkHorn(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/honk_horn",
            callback = callback
        )
    }

    suspend fun rearTrunk(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/actuate_trunk",
            body = "{\"which_trunk\": \"rear\"}",
            callback = callback
        )
    }

    suspend fun frontTrunk(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/actuate_trunk",
            body = "{\"which_trunk\": \"front\"}",
            callback = callback
        )
    }

    suspend fun climateOff(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/auto_conditioning_stop",
            callback = callback
        )
    }

    suspend fun climateOn(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/auto_conditioning_start",
            callback = callback
        )
    }

    suspend fun chargeClose(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/charge_port_door_close",
            callback = callback
        )
    }

    suspend fun chargeOpen(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/command/charge_port_door_open",
            callback = callback
        )
    }

    suspend fun wake(callback: (RestResult) -> Unit) {
        executeRequest(
            method = "POST",
            url = "$baseUrl/api/1/vehicles/$vehicleId/wake_up",
            callback = callback
        )
    }

    private suspend fun executeRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        callback: (RestResult) -> Unit
    ) = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val tag = "TeslaFleetApi"

        // default headers
        val defaultHeaders = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${oauth2Client.getAccessToken()}"
        )

        // merge headers: allow provided headers to override defaults
        val fullHeaders = defaultHeaders.toMutableMap().apply {
            headers.forEach { (key, value) ->
                this[key] = value // This will overwrite if the key exists, or add if new
            }
        }

        Log.d(tag, "Sending request: $method to $url")
        Log.d(tag, "Headers: $fullHeaders")
        if (body != null) Log.d(tag, "Body: $body")

        try {
            val request = Request.Builder()
                .url(url)
                .apply {
                    fullHeaders.forEach { (k, v) -> addHeader(k, v) }
                    when (method) {
                        "POST" -> post(
                            (body ?: "").toRequestBody("application/json".toMediaType())
                        )
                        "GET" -> get()
                    }
                }
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            Log.d(tag, "Response received with code: ${response.code}")
            Log.d(tag, "Response body: $responseBody")

            if (response.isSuccessful) {
                Log.d(tag, "Request was successful. Data: $responseBody")
                callback(RestResult.Success(responseBody))
            } else {
                Log.e(tag, "Request failed with HTTP code: ${response.code}")
                callback(RestResult.Failure("Unexpected code ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Request failed with exception: ${e.message}", e)
            callback(RestResult.Failure(e.message ?: "An unknown error occurred"))
        }
    }
}

sealed class RestResult {
    data class Success(val data: String) : RestResult()
    data class Failure(val errorMessage: String) : RestResult()
}
