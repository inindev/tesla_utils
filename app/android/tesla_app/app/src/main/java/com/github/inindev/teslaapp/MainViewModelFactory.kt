package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val oauth2Client: OAuth2Client
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(settingsRepository, oauth2Client) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
