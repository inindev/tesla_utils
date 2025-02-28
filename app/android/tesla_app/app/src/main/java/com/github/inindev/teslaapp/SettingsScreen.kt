package com.github.inindev.teslaapp

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
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
fun SettingsScreen(
    oauth2Client: OAuth2Client,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()
    val statusText by mainViewModel.statusText.collectAsState()
    val validator = SettingsValidator()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // backup of initial settings for undo
    val backupSettings = remember { mutableStateOf(settings) }

    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
        backupSettings.value = settingsViewModel.settingsState.value // Capture initial state
    }

    Scaffold(
        bottomBar = { StatusBar(statusText = statusText) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 8.dp, end = 8.dp)
        ) {
            item {
                SettingsHeader(onBackClicked = { backDispatcher?.onBackPressed() })

                Text("Tesla API Proxy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                ProxyUrlInput(
                    value = settings.proxyUrl,
                    backupValue = backupSettings.value.proxyUrl,
                    onValueChange = { newValue ->
                        val trimmedValue = newValue.trimEnd('/')
                        settingsViewModel.updateProxyUrl(trimmedValue)
                        mainViewModel.updateProxyUrl(trimmedValue)
                    },
                    validator = validator
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 16.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )

                Text("Authentication Credentials", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                ClientIdInput(
                    value = settings.clientId,
                    backupValue = backupSettings.value.clientId,
                    onValueChange = { settingsViewModel.updateClientId(it) },
                    validator = validator
                )
                ClientSecretInput(
                    value = settings.clientSecret,
                    backupValue = backupSettings.value.clientSecret,
                    onValueChange = { settingsViewModel.updateClientSecret(it) },
                    validator = validator
                )

                AuthenticateSection(context, mainViewModel, validator, settings)
                TokenInfoDisplay(oauth2Client, mainViewModel)
            }
        }
    }
}

@Composable
fun SettingsHeader(onBackClicked: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClicked) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
    }

    // thin line under the title and arrow
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        thickness = 1.dp,
        color = Color.LightGray
    )
}

@Composable
fun ProxyUrlInput(
    value: String,
    backupValue: String,
    onValueChange: (String) -> Unit,
    validator: SettingsValidator
) {
    val proxyUrlValidationState = validator.validateProxyUrl(value)
    val isModified = value != backupValue

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Proxy URL") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        textStyle = LocalTextStyle.current.copy(
            color = when (proxyUrlValidationState) {
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
            if (isModified) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Revert Proxy URL",
                    modifier = Modifier
                        .clickable { onValueChange(backupValue) }
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                when (proxyUrlValidationState) {
                    SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Proxy URL incomplete",
                        tint = ValidationWarningColor
                    )
                    SettingsValidator.ValidationState.INVALID -> Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Invalid Proxy URL",
                        tint = MaterialTheme.colorScheme.error
                    )
                    else -> { }
                }
            }
        },
        placeholder = { Text("https://hostname", color = Color.LightGray) },
        isError = proxyUrlValidationState == SettingsValidator.ValidationState.INVALID
    )
    ValidationFeedback(
        validationState = proxyUrlValidationState,
        validButIncompleteMessage = "Proxy URL is partially valid but lacks full https specification. Please include scheme and host.",
        invalidMessage = "Proxy URL is invalid. It must begin with 'https://' and include a valid host."
    )
}

@Composable
fun ClientIdInput(
    value: String,
    backupValue: String,
    onValueChange: (String) -> Unit,
    validator: SettingsValidator
) {
    val clientIdValidationState = validator.validateClientId(value)
    val isModified = value != backupValue

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Client ID") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        textStyle = LocalTextStyle.current.copy(
            color = when (clientIdValidationState) {
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
            if (isModified) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Revert Client ID",
                    modifier = Modifier
                        .clickable { onValueChange(backupValue) }
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
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
                    else -> { }
                }
            }
        },
        placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", color = Color.LightGray) },
        isError = clientIdValidationState == SettingsValidator.ValidationState.INVALID
    )
    ValidationFeedback(
        validationState = clientIdValidationState,
        validButIncompleteMessage = "Client ID is partially valid but incomplete. Please complete the uuid format.",
        invalidMessage = "Please enter a valid uuid format for Client ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    )
}

@Composable
fun ClientSecretInput(
    value: String,
    backupValue: String,
    onValueChange: (String) -> Unit,
    validator: SettingsValidator
) {
    val clientSecretValidationState = validator.validateClientSecret(value)
    val isModified = value != backupValue

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Client Secret") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        textStyle = LocalTextStyle.current.copy(
            color = when (clientSecretValidationState) {
                SettingsValidator.ValidationState.INVALID -> MaterialTheme.colorScheme.error
                SettingsValidator.ValidationState.VALID_BUT_INCOMPLETE -> ValidationWarningColor
                else -> MaterialTheme.colorScheme.onSurface
            }
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            if (isModified) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Revert Client Secret",
                    modifier = Modifier
                        .clickable { onValueChange(backupValue) }
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
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
                    else -> { }
                }
            }
        },
        placeholder = { Text("ta-secret.xxxxxxxxxxxxxxxx", color = Color.LightGray) },
        isError = clientSecretValidationState == SettingsValidator.ValidationState.INVALID
    )
    ValidationFeedback(
        validationState = clientSecretValidationState,
        validButIncompleteMessage = "Client Secret is partially valid but incomplete. Please enter the full 26 characters.",
        invalidMessage = "Client Secret is invalid. It must begin with 'ta-secret.' and be 26 characters long."
    )
}

@Composable
fun AuthenticateSection(
    context: android.content.Context,
    mainViewModel: MainViewModel,
    validator: SettingsValidator,
    settings: SettingsViewModel.Settings
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = buildAnnotatedString {
                append("The Client ID and Secret values can be found in the ")
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Tesla Developer Dashboard")
                    addStringAnnotation(
                        tag = "URL",
                        annotation = "https://developer.tesla.com/en_US/dashboard",
                        start = length - "Tesla Developer Dashboard".length,
                        end = length
                    )
                }
                pop()
                append(".")
            },
            modifier = Modifier
                .clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://developer.tesla.com/en_US/dashboard")
                    )
                    context.startActivity(intent)
                }
        )

        Button(
            onClick = {
                if (validator.validateSettings(settings)) {
                    mainViewModel.initiateAuthFlow(context)
                } else {
                    mainViewModel.updateStatusText("Please enter a valid Client ID and Client Secret to authenticate.")
                }
            },
            enabled = validator.validateSettings(settings),
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.End)
                .height(48.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Authenticate")
        }
    }
}

@Composable
fun TokenInfoDisplay(oauth2Client: OAuth2Client, viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    var tokenInfo by remember { mutableStateOf(oauth2Client.getJwtExpInfo()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.Start
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
            enabled = tokenInfo != null,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.End)
                .height(48.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Refresh Token")
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
        else -> { }
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
