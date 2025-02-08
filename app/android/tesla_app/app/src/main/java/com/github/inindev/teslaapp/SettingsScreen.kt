package com.github.inindev.teslaapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = viewModel()) {
    val uriHandler = LocalUriHandler.current

    // load settings when the screen is composed
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            onValueChange = { viewModel.updateClientId(it) },
            label = { Text("Client ID") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        // Client Secret
        OutlinedTextField(
            value = settings.clientSecret,
            onValueChange = { viewModel.updateClientSecret(it) },
            label = { Text("Client Secret") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

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
                    uriHandler.openUri("https://developer.tesla.com/en_US/dashboard")
                },
        )

        // VIN
        OutlinedTextField(
            value = settings.vin,
            onValueChange = { viewModel.updateVin(it) },
            label = { Text("VIN") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        // Base URL
        OutlinedTextField(
            value = settings.baseUrl,
            onValueChange = { viewModel.updateBaseUrl(it) },
            label = { Text("Base URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

    }
}