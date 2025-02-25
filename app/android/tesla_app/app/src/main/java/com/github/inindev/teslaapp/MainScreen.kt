package com.github.inindev.teslaapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

/**
 * Displays the main app layout with a navigation drawer and vehicle controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val settingsValid = viewModel.settingsValid.collectAsState(initial = false).value
    val statusText = viewModel.statusText.collectAsState().value
    val vehicles = viewModel.vehicles.collectAsState().value
    val selectedVehicle = viewModel.selectedVehicle.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = viewModel.snackbarMessage.collectAsState().value // Move collectAsState here
    val showAboutDialog = viewModel.showAboutDialog.collectAsState().value

    // initial status update
    LaunchedEffect(Unit) {
        viewModel.updateStatusText("Status: Ready")
    }

    // show snackbar when message changes
    LaunchedEffect(snackbarMessage) { // Use the State value directly
        snackbarMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearSnackbarMessage()
        }
    }

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
                            title = { Text("Tesla App", color = Color.White) },
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
                        item {
                            VehicleDropdown(viewModel, vehicles, selectedVehicle)
                            Spacer(modifier = Modifier.height(64.dp))
                        }
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
                            onDismissRequest = {},
                            title = { Text("Settings Update Required", style = MaterialTheme.typography.headlineSmall) },
                            text = { Text("Please update your settings to continue.") },
                            confirmButton = {
                                Button(onClick = { navController.navigate("settings") }) {
                                    Text("Update Settings")
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
                    val context = LocalContext.current
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
