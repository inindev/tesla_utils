package com.github.inindev.teslaapp

// handles calls to the direct tesla fleet api endpoint
class TeslaDirectFleetApi(oauth2Client: OAuth2Client) : TeslaFleetApiBase(oauth2Client) {
    private val baseUrl = "https://fleet-api.prd.na.vn.cloud.tesla.com"

    override fun getBaseUrl(): String = baseUrl

    suspend fun listVehicles(): HttpResult = executeRequest(
        method = "GET",
        endpoint = "/api/1/vehicles"
    )
}
