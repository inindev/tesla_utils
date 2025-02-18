package com.github.inindev.teslaapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.inindev.teslaapp.ui.theme.TeslaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var secureStorage: SecureStorage
    private lateinit var oauth2Client: OAuth2Client
    private lateinit var settingsViewModel: SettingsViewModel
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(secureStorage, oauth2Client, settingsViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        secureStorage = SecureStorage(this)
        oauth2Client = OAuth2Client(secureStorage)
        settingsViewModel = ViewModelProvider(this, SettingsViewModelFactory(secureStorage)).get(SettingsViewModel::class.java)

        setContent {
            TeslaAppTheme {
                val navController = rememberNavController()
                val navHostController = navController as NavHostController
                NavGraph(navHostController, secureStorage, oauth2Client, viewModel)
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return

        // check if the intent contains the expected data from the OAuth callback
        val uri = intent.data
        if (uri == null) {
            viewModel.updateStatusText("Error: URI is null")
            return
        }

        // exchanging the code for tokens involves network operations
        // dispatch to a worker thread for the network operation
        lifecycleScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                oauth2Client.exchangeCodeForTokens(uri)
            }

            // callback to ui thread
            when (result) {
                is OAuth2Client.AuthResult.Success -> {
                    viewModel.updateStatusText("Authentication successful")
                    viewModel.setIsAuthenticating(false)
                }
                is OAuth2Client.AuthResult.Failure -> {
                    viewModel.updateStatusText("Authentication failed: ${result.errorMessage}")
                    viewModel.setIsAuthenticating(false)
                }
            }
        }
    }
}
