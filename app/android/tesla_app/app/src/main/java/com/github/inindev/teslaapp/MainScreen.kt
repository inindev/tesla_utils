package com.github.inindev.teslaapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.inindev.teslaapp.ui.theme.TeslaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: MainViewModel) {
    val settingsValid = viewModel.settingsValid.collectAsState(initial = false).value
    val statusText = viewModel.statusText.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.updateStatusText("Status: Ready")
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        if (!settingsValid) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(10.dp)
                                    .background(Color.Red, shape = RoundedCornerShape(50))
                            )
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
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    Spacer(modifier = Modifier.height(128.dp))
                    GridButtons(viewModel)
                }
                item {
                    OutputPanel(viewModel)
                }
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
                    onDismissRequest = { },
                    title = {
                        Text("Settings Update Required", style = MaterialTheme.typography.headlineSmall)
                    },
                    text = {
                        Text("Please update your settings to continue.")
                    },
                    confirmButton = {
                        Button(
                            onClick = { navController.navigate("settings") }
                        ) {
                            Text("Update Settings")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }
}
