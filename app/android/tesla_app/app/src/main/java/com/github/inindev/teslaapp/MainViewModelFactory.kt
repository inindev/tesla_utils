package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(
    private val secureStorage: SecureStorage,
    private val oauth2Client: OAuth2Client,
    private val settingsViewModel: SettingsViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(secureStorage, oauth2Client, settingsViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
