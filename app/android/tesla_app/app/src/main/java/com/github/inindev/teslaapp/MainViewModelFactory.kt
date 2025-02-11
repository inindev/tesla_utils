package com.github.inindev.teslaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(
    private val secureStorage: SecureStorage,
    private val oauth2Client: OAuth2Client
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(secureStorage, oauth2Client) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
