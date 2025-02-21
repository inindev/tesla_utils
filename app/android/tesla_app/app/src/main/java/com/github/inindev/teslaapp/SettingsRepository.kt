package com.github.inindev.teslaapp

class SettingsRepository(private val secureStorage: SecureStorage) {
    suspend fun loadSettings(): SettingsViewModel.Settings {
        return SettingsViewModel.Settings(
            vin = secureStorage.retrieveVin(),
            baseUrl = secureStorage.retrieveBaseUrl(),
            clientId = secureStorage.retrieveClientId(),
            clientSecret = secureStorage.retrieveClientSecret()
        )
    }

    fun saveSettings(settings: SettingsViewModel.Settings) {
        secureStorage.storeVin(settings.vin)
        secureStorage.storeBaseUrl(settings.baseUrl)
        secureStorage.storeClientId(settings.clientId)
        secureStorage.storeClientSecret(settings.clientSecret)
    }
}
