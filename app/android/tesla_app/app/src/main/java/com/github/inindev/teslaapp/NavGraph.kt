package com.github.inindev.teslaapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(
    navController: NavHostController, oauth2Client: OAuth2Client, mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(navController, mainViewModel)
        }
        composable("settings") {
            SettingsScreen(navController, oauth2Client, mainViewModel, settingsViewModel)
        }
    }
}
