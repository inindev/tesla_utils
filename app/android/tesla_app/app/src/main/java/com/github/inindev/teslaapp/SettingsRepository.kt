package com.github.inindev.teslaapp

class SettingsRepository(private val secureStorage: SecureStorage) {
    fun loadSettings(): SettingsViewModel.Settings {
        return SettingsViewModel.Settings(
            proxyUrl = secureStorage.retrieveProxyUrl(),
            clientId = secureStorage.retrieveClientId(),
            clientSecret = secureStorage.retrieveClientSecret()
        )
    }

    fun saveSettings(settings: SettingsViewModel.Settings) {
        secureStorage.storeProxyUrl(settings.proxyUrl)
        secureStorage.storeClientId(settings.clientId)
        secureStorage.storeClientSecret(settings.clientSecret)
    }
}
