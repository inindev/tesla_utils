package com.github.inindev.teslaapp

class SettingsRepository(private val secureStorage: SecureStorage) {
    suspend fun loadSettings(): SettingsViewModel.Settings {
        return SettingsViewModel.Settings(
            vin = secureStorage.retrieveVin(),
            proxyUrl = secureStorage.retrieveProxyUrl(),
            clientId = secureStorage.retrieveClientId(),
            clientSecret = secureStorage.retrieveClientSecret()
        )
    }

    fun saveSettings(settings: SettingsViewModel.Settings) {
        secureStorage.storeVin(settings.vin)
        secureStorage.storeProxyUrl(settings.proxyUrl)
        secureStorage.storeClientId(settings.clientId)
        secureStorage.storeClientSecret(settings.clientSecret)
    }
}
