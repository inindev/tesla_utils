package com.github.inindev.teslaapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// constants for grid layout
private val BUTTON_SPACING = 8.dp
private val BUTTON_HEIGHT = 64.dp

// reusable command button composable
@Composable
private fun CommandButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(BUTTON_HEIGHT)
            .padding(horizontal = BUTTON_SPACING)
    ) {
        Text(text)
    }
}

// vehicle command buttons
@Composable
fun GridButtons(viewModel: MainViewModel) {
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val buttonsEnabled = selectedVehicle != null // buttons enabled only if a vehicle is selected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BUTTON_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // first row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = BUTTON_SPACING),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // lock doors
            CommandButton("Lock", { viewModel.lockDoors() }, buttonsEnabled, Modifier.weight(1f))
            // climate on
            CommandButton("Climate", { viewModel.climateOn() }, buttonsEnabled, Modifier.weight(1f))
            // charger door close
            CommandButton("CP Close", { viewModel.chargeClose() }, buttonsEnabled, Modifier.weight(1f))
        }

        // second row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = BUTTON_SPACING),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // unlock doors
            CommandButton("Unlock", { viewModel.unlockDoors() }, buttonsEnabled, Modifier.weight(1f))
            // vent windows
            CommandButton("Windows", { viewModel.ventWindows() }, buttonsEnabled, Modifier.weight(1f))
            // wake up
            CommandButton("Wake Up", { viewModel.wakeUp() }, buttonsEnabled, Modifier.weight(1f))
        }

        // third row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = BUTTON_SPACING),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // actuate trunk
            CommandButton("Trunk", { viewModel.rearTrunk() }, buttonsEnabled, Modifier.weight(1f))
            // honk horn
            CommandButton("Honk", { viewModel.honkHorn() }, buttonsEnabled, Modifier.weight(1f))
            // vehicle
            CommandButton("Info", { viewModel.vehicle() }, buttonsEnabled, Modifier.weight(1f))
        }

        // fourth row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = BUTTON_SPACING),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // open frunk
            CommandButton("Frunk", { viewModel.frontTrunk() }, buttonsEnabled, Modifier.weight(1f))
            // flash lights
            CommandButton("Flash", { viewModel.flashLights() }, buttonsEnabled, Modifier.weight(1f))
            // vehicle
            CommandButton("InfoEx", { viewModel.vehicleData() }, buttonsEnabled, Modifier.weight(1f))
        }
    }
}
