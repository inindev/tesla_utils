package com.github.inindev.teslaapp

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SecureStorage class for securely storing and retrieving sensitive data using
 * EncryptedSharedPreferences with AES-256 encryption. Manages client credentials,
 * tokens, OAuth state, ensuring no blank strings are stored.
 */
class SecureStorage(private val context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Stores the authentication access token.
     * @param accessToken The token to store or remove if blank.
     */
    fun storeAccessToken(accessToken: String?) {
        with(sharedPreferences.edit()) {
            if (accessToken.isNullOrBlank()) {
                remove("access_token")
            } else {
                putString("access_token", accessToken)
            }
            apply()
        }
    }

    /**
     * Retrieves the authentication access token, returning an empty string if not found or on error.
     * @return The stored token or an empty string.
     */
    fun retrieveAccessToken(): String {
        return try {
            sharedPreferences.getString("access_token", "") ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error retrieving access token", e)
            ""
        }
    }

    /**
     * Stores the refresh token.
     * @param refreshToken The token to store or remove if blank.
     */
    fun storeRefreshToken(refreshToken: String?) {
        with(sharedPreferences.edit()) {
            if (refreshToken.isNullOrBlank()) {
                remove("refresh_token")
            } else {
                putString("refresh_token", refreshToken)
            }
            apply()
        }
    }

    /**
     * Retrieves the refresh token, returning an empty string if not found or on error.
     * @return The stored refresh token or an empty string.
     */
    fun retrieveRefreshToken(): String {
        return try {
            sharedPreferences.getString("refresh_token", "") ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error retrieving refresh token", e)
            ""
        }
    }

    /**
     * Stores the client ID.
     * @param clientId The client ID to store or remove if blank.
     */
    fun storeClientId(clientId: String?) {
        with(sharedPreferences.edit()) {
            if (clientId.isNullOrBlank()) {
                remove("client_id")
            } else {
                putString("client_id", clientId)
            }
            apply()
        }
    }

    /**
     * Retrieves the client ID, returning an empty string if not found or on error.
     * @return The stored client ID or an empty string.
     */
    fun retrieveClientId(): String {
        return try {
            sharedPreferences.getString("client_id", "") ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error retrieving client ID", e)
            ""
        }
    }

    /**
     * Stores the client secret.
     * @param clientSecret The client secret to store or remove if blank.
     */
    fun storeClientSecret(clientSecret: String?) {
        with(sharedPreferences.edit()) {
            if (clientSecret.isNullOrBlank()) {
                remove("client_secret")
            } else {
                putString("client_secret", clientSecret)
            }
            apply()
        }
    }

    /**
     * Retrieves the client secret, returning an empty string if not found or on error.
     * @return The stored client secret or an empty string.
     */
    fun retrieveClientSecret(): String {
        return try {
            sharedPreferences.getString("client_secret", "") ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error retrieving client secret", e)
            ""
        }
    }

    /**
     * Clears the client credentials from storage.
     */
    fun clearClientCredentials() {
        with(sharedPreferences.edit()) {
            remove("client_id")
            remove("client_secret")
            apply()
        }
    }

    /**
     * Stores the OAuth state for CSRF protection.
     * @param state The state to store or remove if blank.
     */
    fun storeState(state: String?) {
        with(sharedPreferences.edit()) {
            if (state.isNullOrBlank()) {
                remove("oauth_state")
            } else {
                putString("oauth_state", state)
            }
            apply()
        }
    }

    /**
     * Retrieves the OAuth state, returning an empty string if not found or on error.
     * @return The stored state or an empty string.
     */
    fun retrieveState(): String {
        return try {
            sharedPreferences.getString("oauth_state", "") ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error retrieving OAuth state", e)
            ""
        }
    }

    /**
     * Stores the vehicle identification number (VIN).
     * @param vin The VIN to store or remove if blank.
     */
    fun storeVin(vin: String?) {
        with(sharedPreferences.edit()) {
            if (vin.isNullOrBlank()) {
                remove("vin")
            } else {
                putString("vin", vin)
            }
            apply()
        }
    }

    /**
     * Retrieves the VIN, returning an empty string if not found or on error.
     * @return The stored VIN or an empty string.
     */
    fun retrieveVin(): String {
        return try {
            sharedPreferences.getString("vin", "") ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error retrieving VIN", e)
            ""
        }
    }

    /**
     * Stores the base URL for Tesla's API.
     * @param baseUrl The base URL to store or remove if blank.
     */
    fun storeBaseUrl(baseUrl: String?) {
        with(sharedPreferences.edit()) {
            if (baseUrl.isNullOrBlank()) {
                remove("base_url")
            } else {
                putString("base_url", baseUrl)
            }
            apply()
        }
    }

    /**
     * Retrieves the base URL for Tesla's API, returning an empty string if not found or on error.
     * @return The stored base URL or an empty string.
     */
    fun retrieveBaseUrl(): String {
        return try {
            sharedPreferences.getString("base_url", "") ?: ""
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error retrieving base URL", e)
            ""
        }
    }

    /**
     * Clears all data stored in the encrypted shared preferences.
     */
    fun clearSecureStorage() {
        sharedPreferences.edit().clear().apply()
    }
}
