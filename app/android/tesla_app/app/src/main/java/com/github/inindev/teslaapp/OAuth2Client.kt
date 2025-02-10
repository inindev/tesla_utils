package com.github.inindev.teslaapp

import android.net.Uri
import android.util.Base64
import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.math.floor

// 1. Client -> Authorization Server: Redirect user to login and approve access.
// 2. User -> Authorization Server: Authenticate and authorize.
// 3. Authorization Server -> Client: Redirect back with Authorization Code.
// 4. Client -> Authorization Server: POST request with code to get tokens.
// 5. Authorization Server -> Client: Response with Access Token (and Refresh Token).
// 6. Client -> Resource Server: Use Access Token to access protected resources.

class OAuth2Client(private val secureStorage: SecureStorage) {
    companion object {
        const val TAG = "OAuth2Client"
        const val AUTH_EP = "https://auth.tesla.com/oauth2/v3/authorize"
        const val TOKEN_EP = "https://auth.tesla.com/oauth2/v3/token"
        const val AUDIENCE = "https://fleet-api.prd.na.vn.cloud.tesla.com"
        const val CALLBACK_URI = "tesla-app://auth/callback"  // see AndroidManifest.xml
        const val SCOPE = "openid user_data vehicle_device_data vehicle_cmds vehicle_charging_cmds energy_device_data energy_cmds offline_access"
        const val LOCALE = "en-US"
    }

    /**
     * Generates the authentication URL for the OAuth2 flow.
     * 
     * Constructs the URI for initiating user authorization with:
     * - Client ID from secure storage.
     * - A unique state for CSRF protection.
     * 
     * @return The constructed URI for user authentication.
     * @throws Exception If client ID retrieval fails or URI construction errors occur.
     */
    // Step 1: User Authorization
    // https://developer.tesla.com/docs/fleet-api/authentication/third-party-tokens#step-1-user-authorization
    fun initiateAuthFlow(): Uri {
        val clientId = secureStorage.retrieveClientId()
        if (clientId.isBlank()) {
            Log.d(TAG, "Client ID is missing")
            throw Exception("Client ID is missing")
        }

        // generate a unique state for csrf protection
        val state = generateState()
        secureStorage.storeState(state)

        val builder = Uri.parse(AUTH_EP).buildUpon()
        builder.appendQueryParameter("client_id", clientId)
               .appendQueryParameter("locale", LOCALE)
               .appendQueryParameter("prompt", "login")
               .appendQueryParameter("redirect_uri", CALLBACK_URI)
               .appendQueryParameter("response_type", "code")
               .appendQueryParameter("scope", SCOPE)
               .appendQueryParameter("state", state)

        val authUri = builder.build()
        Log.d(TAG, "Generated Auth URI: $authUri")

        return authUri
    }

    /**
     * Exchanges the authorization code for access and refresh tokens.
     * 
     * This method:
     * - Extracts 'code' and 'state' from the callback URI.
     * - Validates the 'state' to prevent CSRF.
     * - Retrieves client credentials from secure storage.
     * - Sends a POST request to exchange the code for tokens.
     * - Stores new tokens securely if successful.
     *
     * @param uri The URI containing the authorization code and state from the OAuth callback.
     * @return An [AuthResult] indicating success or failure of the token exchange.
     */
    // Step 2: Callback
    // https://developer.tesla.com/docs/fleet-api/authentication/third-party-tokens#step-2-callback
    // Step 3: Code Exchange
    // https://developer.tesla.com/docs/fleet-api/authentication/third-party-tokens#step-3-code-exchange
    fun exchangeCodeForTokens(uri: Uri): AuthResult {
        try {
            Log.d(TAG, "Callback URI: $uri")

            // extract parameters from uri
            val code = uri.getQueryParameter("code")
            if (code.isNullOrBlank()) {
                Log.e(TAG, "Authorization code missing from callback URI")
                return AuthResult.Failure("Authorization code missing from callback URI")
            }

            val state = uri.getQueryParameter("state")
            if (state.isNullOrBlank()) {
                Log.e(TAG, "Authorization state missing from callback URI")
                return AuthResult.Failure("Authorization state missing from callback URI")
            }

            // validate state for csrf protection
            val storedState = secureStorage.retrieveState()
            if (state != storedState) {
                Log.e(TAG, "State mismatch in OAuth callback")
                return AuthResult.Failure("State mismatch in OAuth callback")
            }
            secureStorage.storeState(null)

            // retrieve client credentials
            val clientId = secureStorage.retrieveClientId()
            if (clientId.isBlank()) {
                return AuthResult.Failure("Client ID is missing or null")
            }

            val clientSecret = secureStorage.retrieveClientSecret()
            if (clientSecret.isBlank()) {
                return AuthResult.Failure("Client Secret is missing or null")
            }

            // clear previous tokens before requesting new ones
            secureStorage.storeAccessToken(null)
            secureStorage.storeRefreshToken(null)

            // prepare form body for token request
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("code", code)
                .add("audience", AUDIENCE)
                .add("redirect_uri", CALLBACK_URI)
                .build()

            return fetchAndStoreTokenData(formBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error during token exchange", e)
            return AuthResult.Failure("Token exchange failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Retrieves or refreshes an authentication token for accessing the Tesla Fleet API.
     *
     * This method:
     * - Checks if stored tokens are present and valid.
     * - If the token's remaining life is less than 20%, it refreshes the token.
     * - Returns the `access_token` if valid, otherwise `null`.
     *
     * @return The `access_token` if valid or newly refreshed, `null` if authentication fails or requires restart.
     */
    fun getAccessToken(): String? {
        try {
            // retrieve tokens from secure storage
            val accessToken = secureStorage.retrieveAccessToken()
            if (accessToken.isBlank()) {
                Log.d(TAG, "Access token is missing")
                return null
            }

            val refreshToken = secureStorage.retrieveRefreshToken()
            if (refreshToken.isBlank()) {
                Log.d(TAG, "Refresh token is missing")
                return null
            }

            // evaluate token's expiration
            val expInfo = getJwtExpInfo(accessToken)
            if (expInfo == null || expInfo.second < 20) {
                Log.d(TAG, "Token life below 20%, refreshing")
                when (val result = refreshAccessToken()) {
                    is AuthResult.Success -> {
                        // after refresh, fetch new tokens
                        val newAccessToken = secureStorage.retrieveAccessToken()
                        if (newAccessToken.isBlank()) {
                            Log.d(TAG, "New access token is missing")
                            return null
                        }

                        val newRefreshToken = secureStorage.retrieveRefreshToken()
                        if (newRefreshToken.isBlank()) {
                            Log.d(TAG, "New refresh token is missing")
                            return null
                        }

                        return newAccessToken
                    }
                    is AuthResult.Failure -> {
                        Log.e(TAG, "Token refresh failed: ${result.errorMessage}")
                        return null
                    }
                }
            }

            return accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving access token: ${e.message}", e)
            return null
        }
    }

    /**
     * Refreshes the access token using the stored refresh token.
     * 
     * @return An [AuthResult] indicating if the refresh was successful or not.
     */
    private fun refreshAccessToken(): AuthResult {
        try {
            val clientId = secureStorage.retrieveClientId()
            if (clientId.isBlank()) {
                Log.d(TAG, "Client ID is missing")
                return AuthResult.Failure("Client ID is missing")
            }

            val refreshToken = secureStorage.retrieveRefreshToken()
            if (refreshToken.isBlank()) {
                Log.d(TAG, "Refresh token is missing")
                return AuthResult.Failure("Refresh token is missing")
            }

            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("refresh_token", refreshToken)
                .build()

            return fetchAndStoreTokenData(formBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error while refreshing token", e)
            return AuthResult.Failure("Token refresh failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Fetches and stores token data from the OAuth2 token endpoint.
     * 
     * @param body The form body to send with the token request.
     * @return An [AuthResult] indicating success or failure of token fetching and storage.
     */
    private fun fetchAndStoreTokenData(body: FormBody): AuthResult {
        try {
            val request = Request.Builder().url(TOKEN_EP).post(body).build()
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return AuthResult.Failure("HTTP error: ${response.code}")
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    return AuthResult.Failure("Empty response body")
                }
                // parse the json response
                Log.d(TAG, "Response body: $responseBody")

                val jsonObject = JSONObject(responseBody)

                val accessToken = jsonObject.optString("access_token", "")
                if (accessToken.isNullOrBlank()) {
                    return AuthResult.Failure("Access token is missing from response")
                }

                val refreshToken = jsonObject.optString("refresh_token", "")
                if (refreshToken.isNullOrBlank()) {
                    return AuthResult.Failure("Refresh token is missing from response")
                }

                val tokenType = jsonObject.optString("token_type", "")
                if (tokenType.isNullOrBlank()) {
                    return AuthResult.Failure("Token type is missing from response")
                }

                val expiresIn = jsonObject.optInt("expires_in", 0)
                if (expiresIn < 1) {
                    return AuthResult.Failure("Access token is expired")
                }

                secureStorage.storeAccessToken(accessToken)
                secureStorage.storeRefreshToken(refreshToken)
                Log.d(TAG, "Access and refresh tokens stored successfully")

                return AuthResult.Success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching and storing token data", e)
            return AuthResult.Failure("Token data fetch failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Decodes a JWT token to get expiration information.
     * 
     * @param jwtToken The JWT token to decode.
     * @return A pair of (expiration time in seconds, percentage of life remaining) or null if parsing fails.
     */
    private fun getJwtExpInfo(jwtToken: String? = null): Pair<Long, Int>? {
        val token = jwtToken ?: secureStorage.retrieveAccessToken()
        if (token.isBlank()) {
                Log.d(TAG, "JWT is null or empty")
                return null
        }

        val parts = token.split('.')
        if (parts.size != 3) {
            Log.d(TAG, "Invalid JWT format: expected 3 parts, but got ${parts.size}")
            return null
        }

        val payload = try {
            String(Base64.decode(parts[1], Base64.URL_SAFE))
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Failed to decode JWT payload", e)
            return null
        }

        val json = try {
            JSONObject(payload)
        } catch (e: JSONException) {
            Log.d(TAG, "Failed to json parse JWT payload", e)
            return null
        }

        val now = System.currentTimeMillis() / 1000 // current time in seconds
        val exp = json.getLong("exp")
        val iat = json.getLong("iat")

        // calculate the percentage of life remaining
        var lifeRemain = 0
        if (exp > iat) {
            lifeRemain = max(floor((exp - now).toDouble() / (exp - iat) * 100).toInt(), 0)
        }
        Log.d(TAG, "Token life remaining: $lifeRemain%")

        return Pair(exp, lifeRemain)
    }

    /**
     * Generates a secure state for OAuth2 flow to prevent CSRF attacks.
     * 
     * @return A URL-safe Base64 encoded string for the state parameter.
     */
    private fun generateState(): String {
        val uuid = UUID.randomUUID()
        val uuidBytes = ByteArray(16)
        val bb = java.nio.ByteBuffer.wrap(uuidBytes)
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return Base64.encodeToString(uuidBytes, Base64.URL_SAFE or Base64.NO_PADDING)
    }

    /**
     * Represents possible results of authentication operations.
     */
    sealed class AuthResult {
        object Success : AuthResult()
        data class Failure(val errorMessage: String) : AuthResult()
    }
}
