package com.github.inindev.teslaapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.github.inindev.teslaapp.ui.theme.TeslaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// initialize app components
class MainActivity : ComponentActivity() {
    private lateinit var secureStorage: SecureStorage
    private lateinit var oauth2Client: OAuth2Client
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsValidator: SettingsValidator

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(secureStorage, settingsRepository, oauth2Client)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(settingsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // initialize properties here, after context is available
        secureStorage = SecureStorage(this)
        oauth2Client = OAuth2Client(secureStorage)
        settingsRepository = SettingsRepository(secureStorage)
        settingsValidator = SettingsValidator()

        // check settings and login state on startup
        lifecycleScope.launch {
            try {
                val settings = settingsRepository.loadSettings()
                val isValid = settingsValidator.validateSettings(settings)
                val hasToken = withContext(Dispatchers.IO) { oauth2Client.getAccessToken() != null }
                mainViewModel.updateSettingsValid(isValid)
                Log.d("MainActivity", "Initial settings valid: $isValid, has token: $hasToken")

                if (hasToken) {
                    withContext(Dispatchers.IO) {
                        mainViewModel.fetchVehicles()
                    }
                } else {
                    mainViewModel.updateStatusText("Status: Ready - Please authenticate")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Startup error: ${e.message}", e)
                mainViewModel.updateStatusText("Error initializing: ${e.message}")
            }
        }

        setContent {
            TeslaAppTheme {
                val navController = rememberNavController()
                NavGraph(navController, oauth2Client, mainViewModel, settingsViewModel)
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // handle oauth callback intent
    private fun handleIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return

        // check if the intent contains the expected data from the oauth callback
        val uri = intent.data
        if (uri == null) {
            mainViewModel.updateStatusText("Error: URI is null")
            return
        }

        // exchanging the code for tokens involves network operations
        // dispatch to a worker thread for the network operation
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                oauth2Client.exchangeCodeForTokens(uri)
            }

            // callback to ui thread
            when (result) {
                is OAuth2Client.AuthResult.Success -> {
                    mainViewModel.updateStatusText("Authentication successful")
                    // fetch vehicles after successful login
                    withContext(Dispatchers.IO) {
                        mainViewModel.fetchVehicles()
                    }
                }
                is OAuth2Client.AuthResult.Failure -> {
                    mainViewModel.updateStatusText("Authentication failed: ${result.errorMessage}")
                }
            }
        }
    }
}
