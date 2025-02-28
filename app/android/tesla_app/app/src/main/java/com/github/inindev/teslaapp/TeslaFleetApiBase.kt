package com.github.inindev.teslaapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

abstract class TeslaFleetApiBase(private val oauth2Client: OAuth2Client) {
    private val tag = "TeslaFleetApi"
    private val client = OkHttpClient()

    // abstract method for subclasses to define their base url
    protected abstract fun getBaseUrl(): String

    // execute the http request
    protected suspend fun executeRequest(
        method: String,
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): HttpResult = withContext(Dispatchers.IO) {
        val accessToken = oauth2Client.getAccessToken()
        if (accessToken == null) {
            Log.e(tag, "no valid token available for api call")
            return@withContext HttpResult.Failure(401) // unauthorized
        }

        val url = "${getBaseUrl()}$endpoint"
        val defaultHeaders = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $accessToken"
        )

        val fullHeaders = defaultHeaders.toMutableMap().apply {
            headers.forEach { (key, value) -> this[key] = value }
        }

        Log.d(tag, "sending request: $method to $url")
        Log.d(tag, "headers: $fullHeaders")
        if (body != null) Log.d(tag, "body: $body")

        val request = Request.Builder()
            .url(url)
            .apply {
                fullHeaders.forEach { (k, v) -> addHeader(k, v) }
                when (method.uppercase()) {
                    "GET" -> get()
                    "POST" -> post((body ?: "").toRequestBody("application/json".toMediaType()))
                    else -> throw IllegalArgumentException("unsupported method: $method")
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
