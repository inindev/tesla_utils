package com.github.inindev.teslaapp

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.github.inindev.teslaapp.ui.theme.ValidationWarningColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun SettingsScreen(navController: NavHostController, oauth2Client: OAuth2Client, mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()
    val statusText by mainViewModel.statusText.collectAsState()

    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
    }

    // update settingsValid when settings change
    LaunchedEffect(settings) {
        val isValid = SettingsValidator().validateSettings(settings)
        mainViewModel.updateSettingsValid(isValid)
        if (isValid) mainViewModel.updateStatusText("Settings saved successfully")
        Log.d("SettingsScreen", "Settings updated, valid: $isValid")
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

            // VIN
            val vinValidationState by settingsViewModel.vinValidationState.collectAsState()
            OutlinedTextField(
                value = settings.vin,
                onValueChange = { settingsViewModel.updateVin(it.uppercase()) },
                label = { Text("VIN") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textStyle = LocalTextStyle.current.copy(
                    color = when (vinValidationState) {
                        SettingsValidator.ValidationState.INVALID -> MaterialTheme.colorScheme.error
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> ValidationWarningColor
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    when (vinValidationState) {
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "VIN incomplete",
                            tint = ValidationWarningColor
                        )
                        SettingsValidator.ValidationState.INVALID -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Invalid VIN",
                            tint = MaterialTheme.colorScheme.error
                        )
                        else -> { } // No icon for VALID or EMPTY
                    }
                },
                placeholder = {
                    Text(
                        "XXXXXXXXXXXXXXXXX",
                        color = Color.LightGray
                    )
                },
                isError = vinValidationState == SettingsValidator.ValidationState.INVALID
            )
            ValidationFeedback(
                validationState = vinValidationState,
                validButIncompleteMessage = "VIN is partially valid but too short. Please enter all 17 characters.",
                invalidMessage = "Not a valid VIN: Please enter a 17-character VIN with valid characters."
            )

            // Base URL
            val baseUrlValidationState by settingsViewModel.baseUrlValidationState.collectAsState()
            OutlinedTextField(
                value = settings.baseUrl,
                onValueChange = { settingsViewModel.updateBaseUrl(it) },
                label = { Text("Base URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textStyle = LocalTextStyle.current.copy(
                    color = when (baseUrlValidationState) {
                        SettingsValidator.ValidationState.INVALID -> MaterialTheme.colorScheme.error
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> ValidationWarningColor
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    when (baseUrlValidationState) {
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Base URL incomplete",
                            tint = ValidationWarningColor
                        )
                        SettingsValidator.ValidationState.INVALID -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Invalid Base URL",
                            tint = MaterialTheme.colorScheme.error
                        )
                        else -> { } // No icon for VALID or EMPTY
                    }
                },
                placeholder = {
                    Text(
                        "https://hostname",
                        color = Color.LightGray
                    )
                },
                isError = baseUrlValidationState == SettingsValidator.ValidationState.INVALID
            )
            ValidationFeedback(
                validationState = baseUrlValidationState,
                validButIncompleteMessage = "Base URL is partially valid but lacks full HTTPS specification. Please include scheme and host.",
                invalidMessage = "Base URL is invalid. Must start with 'https://' and include a valid host."
            )

            // horizontal separator
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            // Client ID
            val clientIdValidationState by settingsViewModel.clientIdValidationState.collectAsState()
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
                        SettingsValidator.ValidationState.INVALID -> MaterialTheme.colorScheme.error // Red
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> ValidationWarningColor
                        else -> MaterialTheme.colorScheme.onSurface // Default
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    when (clientIdValidationState) {
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Client ID incomplete",
                            tint = ValidationWarningColor
                        )
                        SettingsValidator.ValidationState.INVALID -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Invalid Client ID",
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
                },
                isError = clientIdValidationState == SettingsValidator.ValidationState.INVALID
            )
            // warning / error message below the TextField
            ValidationFeedback(
                validationState = clientIdValidationState,
                validButIncompleteMessage = "Client ID is partially valid but incomplete. Please complete the UUID format.",
                invalidMessage = "Please enter a valid UUID format for Client ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            )

            // Client Secret
            val clientSecretValidationState by settingsViewModel.clientSecretValidationState.collectAsState()
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
                        SettingsValidator.ValidationState.INVALID -> MaterialTheme.colorScheme.error // Red
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> ValidationWarningColor
                        else -> MaterialTheme.colorScheme.onSurface // Default
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    when (clientSecretValidationState) {
                        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Client Secret incomplete",
                            tint = ValidationWarningColor
                        )
                        SettingsValidator.ValidationState.INVALID -> Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Invalid Client Secret",
                            tint = MaterialTheme.colorScheme.error
                        )
                        else -> { } // No icon for VALID or EMPTY
                    }
                },
                // Placeholder text
                placeholder = { Text("ta-secret.xxxxxxxxxxxxxxxx", color = Color.LightGray) },
                isError = clientSecretValidationState == SettingsValidator.ValidationState.INVALID
            )
            // Add a message below the Client Secret field for validation feedback
            ValidationFeedback(
                validationState = clientSecretValidationState,
                validButIncompleteMessage = "Client Secret is partially valid but incomplete. Please enter the full 26 characters.",
                invalidMessage = "Client Secret is invalid. It must start with 'ta-secret.' and be 26 characters long."
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        append(".")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 32.dp)
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://developer.tesla.com/en_US/dashboard")
                            )
                            context.startActivity(intent)
                        },
                )

                // Authenticate Button
                val scope = rememberCoroutineScope()
                val isAuthenticating by mainViewModel.isAuthenticating.collectAsState()
                Button(
                    onClick = {
                        if (clientIdValidationState == SettingsValidator.ValidationState.VALID) {
                            mainViewModel.initiateAuthFlow(context)
                        } else {
                            mainViewModel.updateStatusText("Please enter a valid Client ID to authenticate.")
                            mainViewModel.setIsAuthenticating(false)
                        }
                    },
                    enabled = clientIdValidationState == SettingsValidator.ValidationState.VALID && !isAuthenticating,
                ) {
                    Text("Authenticate")
                }
            }

            // Usage in SettingsScreen
            TokenInfoDisplay(oauth2Client, mainViewModel)
        }
    }
}

@Composable
fun ValidationFeedback(
    validationState: SettingsValidator.ValidationState,
    validButIncompleteMessage: String,
    invalidMessage: String
) {
    when (validationState) {
        SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> {
            Text(
                text = validButIncompleteMessage,
                color = ValidationWarningColor,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        SettingsValidator.ValidationState.INVALID -> {
            Text(
                text = invalidMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        else -> { } // no message for VALID or EMPTY
    }
}

@Composable
fun TokenInfoDisplay(oauth2Client: OAuth2Client, viewModel: MainViewModel) {
    var tokenInfo by remember { mutableStateOf(oauth2Client.getJwtExpInfo()) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Token Life Remaining: ")
                    }
                    append(if (tokenInfo != null) "${tokenInfo!!.second}%" else "N/A")
                }
            )
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Token Expiration: ")
                    }
                    append(if (tokenInfo != null) (tokenInfo!!.first * 1000).formatDateTime() else "N/A")
                }
            )
        }
        Button(
            onClick = {
                scope.launch(Dispatchers.Main) {
                    try {
                        viewModel.updateStatusText("Refreshing token...")
                        val authResult = withContext(Dispatchers.IO) {
                            oauth2Client.refreshAccessToken()
                        }
                        when (authResult) {
                            is OAuth2Client.AuthResult.Success -> {
                                val newTokenInfo = withContext(Dispatchers.IO) {
                                    oauth2Client.getJwtExpInfo()
                                }
                                if (newTokenInfo != null) {
                                    tokenInfo = newTokenInfo
                                    viewModel.updateStatusText("Token refreshed successfully.")
                                } else {
                                    viewModel.updateStatusText("Failed to get new token info.")
                                }
                            }
                            is OAuth2Client.AuthResult.Failure -> {
                                viewModel.updateStatusText("Token refresh failed: ${authResult.errorMessage}")
                            }
                        }
                    } catch (e: Exception) {
                        viewModel.updateStatusText("Exception during token refresh: ${e.message}")
                    }
                }
            },
            enabled = tokenInfo != null
        ) {
            Text("Refresh Token")
        }
    }
}

@Composable
fun Long.formatDateTime(): String {
    val utcDate = Date(this)
    val localTimeZone = TimeZone.getDefault()

    val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val localFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = localTimeZone
    }
    val localDate = utcFormat.parse(utcFormat.format(utcDate))!!

    return localFormat.format(localDate)
}
