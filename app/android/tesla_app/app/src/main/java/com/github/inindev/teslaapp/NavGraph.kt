package com.github.inindev.teslaapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

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

