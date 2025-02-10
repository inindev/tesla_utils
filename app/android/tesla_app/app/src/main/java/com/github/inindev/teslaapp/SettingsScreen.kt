package com.github.inindev.teslaapp

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavHostController, secureStorage: SecureStorage, oauth2Client: OAuth2Client, viewModel: MainViewModel) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(secureStorage))
    val settings by settingsViewModel.settingsState.collectAsState()
    val clientIdValidationState by settingsViewModel.clientIdValidationState.collectAsState()
    val clientSecretValidationState by settingsViewModel.clientSecretValidationState.collectAsState()
    val scope = rememberCoroutineScope()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val statusTextState by viewModel.statusText.collectAsState() // Collect the state
    val statusText = statusTextState // Use the collected state

    // load settings when the screen is composed
    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
    }

    Scaffold(
        bottomBar = { StatusBar(statusText = statusText) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp), // Add padding at the bottom for spacing
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp)) // Add space between icon and text
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }

            // thin line under the title and arrow
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            // Client ID
            OutlinedTextField(
                value = settings.clientId,
                onValueChange = { settingsViewModel.updateClientId(it) },
                label = { Text("Client ID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                // set text color based on validation state
                textStyle = LocalTextStyle.current.copy(
                    color = when (clientIdValidationState) {
                        SettingsViewModel.ValidationState.INVALID -> MaterialTheme.colorScheme.error // Red
                        SettingsViewModel.ValidationState.VALID_BUT_INCOMPLETE -> Color(0xFF1E90FF) // DodgerBlue
                        SettingsViewModel.ValidationState.VALID, SettingsViewModel.ValidationState.EMPTY -> MaterialTheme.colorScheme.onSurface // Default
                    }
                ),
                // add a trailing icon to show validation state
                trailingIcon = {
                    when (clientIdValidationState) {
                        SettingsViewModel.ValidationState.VALID_BUT_INCOMPLETE -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Incomplete",
                            tint = Color(0xFF1E90FF)
                        )

                        SettingsViewModel.ValidationState.INVALID -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )

                        else -> { } // No icon for VALID or EMPTY
                    }
                },
                placeholder = {
                    Text(
                        "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                        color = Color.LightGray
                    )
                }
            )

            // warning / error message below the TextField
            when (clientIdValidationState) {
                SettingsViewModel.ValidationState.VALID_BUT_INCOMPLETE -> {
                    Text(
                        text = "Client ID is partially valid but incomplete. Please complete the UUID format.",
                        color = Color(0xFF1E90FF),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                SettingsViewModel.ValidationState.INVALID -> {
                    Text(
                        text = "Please enter a valid UUID format for Client ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                else -> {} // no message for VALID or EMPTY
            }

            // Client Secret
            OutlinedTextField(
                value = settings.clientSecret,
                onValueChange = { settingsViewModel.updateClientSecret(it) },
                label = { Text("Client Secret") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                // set text color based on validation state
                textStyle = LocalTextStyle.current.copy(
                    color = when (clientSecretValidationState) {
                        SettingsViewModel.ValidationState.INVALID -> MaterialTheme.colorScheme.error // Red
                        SettingsViewModel.ValidationState.VALID_BUT_INCOMPLETE -> Color(0xFF1E90FF) // DodgerBlue
                        SettingsViewModel.ValidationState.VALID, SettingsViewModel.ValidationState.EMPTY -> MaterialTheme.colorScheme.onSurface // Default
                    }
                ),
                // add a trailing icon to show validation state
                trailingIcon = {
                    when (clientSecretValidationState) {
                        SettingsViewModel.ValidationState.VALID_BUT_INCOMPLETE -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Incomplete",
                            tint = Color(0xFF1E90FF)
                        )

                        SettingsViewModel.ValidationState.INVALID -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )

                        else -> {} // No icon for VALID or EMPTY
                    }
                },
                // Placeholder text
                placeholder = { Text("ta-secret.xxxxxxxxxxxxxxxx", color = Color.LightGray) }
            )

            // Add a message below the Client Secret field for validation feedback
            when (clientSecretValidationState) {
                SettingsViewModel.ValidationState.VALID_BUT_INCOMPLETE -> {
                    Text(
                        text = "Client Secret is partially valid but incomplete. Please enter the full 26 characters.",
                        color = Color(0xFF1E90FF),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                SettingsViewModel.ValidationState.INVALID -> {
                    Text(
                        text = "Client Secret is invalid. It must start with 'ta-secret.' and be 26 characters long.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                else -> {} // no message for VALID or EMPTY
            }

            // Authenticate Button
            Button(
                onClick = {
                    viewModel.setIsAuthenticating(true)
                    scope.launch {
                        try {
                            // only proceed with authentication if the clientId is VALID
                            if (clientIdValidationState == SettingsViewModel.ValidationState.VALID) {
                                // begin oauth2 authentication sequence
                                viewModel.updateStatusText("Authentication process started...")
                                val authUri = oauth2Client.initiateAuthFlow()
                                val customTabsIntent = CustomTabsIntent.Builder().build()
                                customTabsIntent.launchUrl(context, authUri)
                            } else {
                                viewModel.updateStatusText("Please enter a valid Client ID to authenticate.")
                                viewModel.setIsAuthenticating(false)
                            }
                        } catch (e: Exception) {
                            viewModel.updateStatusText("Authentication failed: ${e.message}")
                            viewModel.setIsAuthenticating(false)
                        }
                    }
                },
                enabled = clientIdValidationState == SettingsViewModel.ValidationState.VALID && !isAuthenticating,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Text("Authenticate")
            }

            // Message with link
            Text(
                text = buildAnnotatedString {
                    append("The Client ID and Secret values can be found in the ")
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Tesla developer dashboard")
                        addStringAnnotation(
                            tag = "URL",
                            annotation = "https://developer.tesla.com/en_US/dashboard",
                            start = length - "Tesla developer dashboard".length,
                            end = length
                        )
                    }
                    pop()
                },
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://developer.tesla.com/en_US/dashboard")
                        )
                        context.startActivity(intent)
                    },
            )

            // Visual Separator
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            // VIN
            OutlinedTextField(
                value = settings.vin,
                onValueChange = { settingsViewModel.updateVin(it) },
                label = { Text("VIN") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            // Base URL
            OutlinedTextField(
                value = settings.baseUrl,
                onValueChange = { settingsViewModel.updateBaseUrl(it) },
                label = { Text("Base URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}