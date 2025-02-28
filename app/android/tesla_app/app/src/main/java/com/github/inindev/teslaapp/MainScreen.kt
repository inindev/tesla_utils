package com.github.inindev.teslaapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.inindev.teslaapp.ui.theme.TeslaPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Displays the main app layout with a navigation drawer and vehicle controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val statusText = viewModel.statusText.collectAsState().value
    val vehicles = viewModel.vehicles.collectAsState().value
    val selectedVehicle = viewModel.selectedVehicle.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = viewModel.snackbarMessage.collectAsState().value
    val showAboutDialog = viewModel.showAboutDialog.collectAsState().value
    val showLoginDialog by viewModel.showLoginDialog.collectAsState()
    val settingsValid by viewModel.settingsValid.collectAsState()

    // show snackbar when message changes
    ShowSnackbarWhenMessageChanges(scope, snackbarHostState, snackbarMessage, viewModel)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text("Tesla App", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = navController.currentDestination?.route == "home",
                    onClick = {
                        navController.navigate("home") { popUpTo(navController.graph.startDestinationId) }
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = navController.currentDestination?.route == "settings",
                    onClick = {
                        navController.navigate("settings")
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                )
                NavigationDrawerItem(
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        viewModel.showAboutDialog()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = "About") }
                )
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        viewModel.logout()
                        navController.navigate("home") { popUpTo(navController.graph.startDestinationId) }
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") }
                )
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = TeslaPrimary,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .heightIn(min = 64.dp)
                            )
                        }
                    },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tesla App", color = Color.White)
                                    VehicleDropdown(viewModel, vehicles, selectedVehicle)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Open Menu", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = TeslaPrimary,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White
                            )
                        )
                    },
                    bottomBar = { StatusBar(statusText = statusText) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        item { Spacer(modifier = Modifier.height(110.dp)) }
                        item { GridButtons(viewModel) }
                        item { OutputPanel(viewModel) }
                    }
                }

                // overlay when settings are invalid
                if (!settingsValid) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.7f)
                            .background(Color.Black)
                    ) {
                        AlertDialog(
                            onDismissRequest = {  },
                            title = { Text("Settings Update Required", style = MaterialTheme.typography.headlineSmall) },
                            text = { Text("Please update your settings to continue.") },
                            confirmButton = {
                                Button(onClick = { navController.navigate("settings") }) {
                                    Text("Update Settings")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {  }) {
                                    Text("Cancel")
                                }
                            },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }
                }

                // overlay when not logged in
                if (showLoginDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.7f)
                            .background(Color.Black)
                    ) {
                        AlertDialog(
                            onDismissRequest = { viewModel.hideLoginDialog() },
                            title = { Text("Login Required", style = MaterialTheme.typography.headlineSmall) },
                            text = { Text("You need to log in to access vehicle controls.") },
                            confirmButton = {
                                Button(onClick = { viewModel.initiateAuthFlow(context) }) {
                                    Text("Login")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.hideLoginDialog() }) {
                                    Text("Cancel")
                                }
                            },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }
                }

                // about dialog
                if (showAboutDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.hideAboutDialog() },
                        title = {
                            Column {
                                Text("About Tesla App", style = MaterialTheme.typography.headlineSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Version: 0.7", style = MaterialTheme.typography.bodyMedium)
                                Text("Copyright (c) 2025, John Clark <inindev@gmail.com>", style = MaterialTheme.typography.bodyMedium)
                            }
                        },
                        text = {
                            Column {
                                Text("A third-party Tesla vehicle control app using the Tesla Fleet API.")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    buildAnnotatedString {
                                        append("Visit ")
                                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                                            append("Tesla Developer")
                                        }
                                        append(" for more information.")
                                    },
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.tesla.com"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.hideAboutDialog() }) { Text("Close") }
                        },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color.White
                    )
                }
            }
        }
    )
}

/**
 * Vehicle selection dropdown adapted for the TopAppBar title section.
 */
@Composable
private fun VehicleDropdown(
    viewModel: MainViewModel,
    vehicles: List<MainViewModel.Vehicle>,
    selectedVehicle: MainViewModel.Vehicle?
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(180.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = selectedVehicle?.displayName ?: "Select Vehicle",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(256.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (vehicles.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No vehicles found") },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(
                        text = { Text("${vehicle.displayName}: ${vehicle.vin}") },
                        onClick = {
                            viewModel.selectVehicle(vehicle)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// show snackbar when message changes
@Composable
private fun ShowSnackbarWhenMessageChanges(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    snackbarMessage: String?,
    viewModel: MainViewModel
) {
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearSnackbarMessage()
        }
    }
}
