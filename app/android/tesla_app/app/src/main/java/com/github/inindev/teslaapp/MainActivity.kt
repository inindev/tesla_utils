package com.github.inindev.teslaapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.inindev.teslaapp.ui.theme.TeslaAppTheme
import com.github.inindev.teslaapp.ui.theme.TeslaPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var secureStorage: SecureStorage
    private lateinit var oauth2Client: OAuth2Client
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        secureStorage = SecureStorage(this)
        oauth2Client = OAuth2Client(secureStorage)

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

@Composable
fun NavGraph(navController: NavHostController, secureStorage: SecureStorage, oauth2Client: OAuth2Client, viewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(navController, viewModel)
        }
        composable("settings") {
            SettingsScreen(navController, secureStorage, oauth2Client, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: MainViewModel) {
    val statusTextState = viewModel.statusText.collectAsState()
    val statusText = statusTextState.value

    LaunchedEffect(Unit) {
        viewModel.updateStatusText("Status: Ready")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tesla App", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { /* handle navigation click */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TeslaPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = { StatusBar(statusText = statusText) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Text(
            text = "Welcome to Tesla App",
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        )
    }
}

@Composable
fun StatusBar(statusText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFB0B0B0)) // Medium gray color
    ) {
        // Status bar content here
        Text(
            text = statusText,
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                .align(Alignment.CenterStart),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

class MainViewModel : ViewModel() {
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    fun setIsAuthenticating(value: Boolean) {
        viewModelScope.launch {
            _isAuthenticating.value = value
        }
    }

    private val _statusText = MutableStateFlow("Status: Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun updateStatusText(newStatus: String) {
        viewModelScope.launch {
            _statusText.value = newStatus
        }
    }
}
