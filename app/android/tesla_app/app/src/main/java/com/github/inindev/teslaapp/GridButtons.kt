package com.github.inindev.teslaapp

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// constants for grid layout
private val buttonSpacing = 8.dp
private val buttonHeight = 64.dp

// reusable command button composable
@Composable
private fun CommandButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    commandInProgress: Map<String, Boolean>,
    modifier: Modifier = Modifier
) {
    var key = ""
    try {
        val getNameMethod = onClick::class.java.getMethod("getName")
        key = getNameMethod.invoke(onClick) as String
        Log.d("CommandButton", "onClick name: $key")
    } catch (e: Exception) {
        Log.w("CommandButton", "Failed to get name for button '$text', no spinner will be shown: ${e.message}")
    }

    Box(modifier, Alignment.Center) {
        Button(
            onClick = onClick,
            enabled = enabled && commandInProgress[key] != true,
            modifier = Modifier.fillMaxWidth().height(buttonHeight).padding(horizontal = buttonSpacing)
        ) { Text(text) }
        if (commandInProgress[key] == true) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

// vehicle command buttons
@Composable
fun GridButtons(viewModel: MainViewModel) {
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val buttonsEnabled = selectedVehicle != null // buttons enabled only if a vehicle is selected
    val commandInProgress by viewModel.commandInProgress.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = buttonSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // first row
        Row(Modifier.fillMaxWidth().padding(vertical = buttonSpacing), Arrangement.SpaceEvenly) {
            // lock doors
            CommandButton("Lock", viewModel::lockDoors, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // climate on
            CommandButton("Climate", viewModel::climateOn, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // charger door close
            CommandButton("CP Close", viewModel::chargeClose, buttonsEnabled, commandInProgress, Modifier.weight(1f))
        }
        // second row
        Row(Modifier.fillMaxWidth().padding(vertical = buttonSpacing), Arrangement.SpaceEvenly) {
            // unlock doors
            CommandButton("Unlock", viewModel::unlockDoors, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // vent windows
            CommandButton("Windows", viewModel::ventWindows, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // wake up
            CommandButton("Wake Up", viewModel::wakeUp, buttonsEnabled, commandInProgress, Modifier.weight(1f))
        }
        // third row
        Row(Modifier.fillMaxWidth().padding(vertical = buttonSpacing), Arrangement.SpaceEvenly) {
            // open trunk
            CommandButton("Trunk", viewModel::rearTrunk, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // honk horn
            CommandButton("Honk", viewModel::honkHorn, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // vehicle info
            CommandButton("Info", viewModel::vehicle, buttonsEnabled, commandInProgress, Modifier.weight(1f))
        }
        // fourth row
        Row(Modifier.fillMaxWidth().padding(vertical = buttonSpacing), Arrangement.SpaceEvenly) {
            // open frunk
            CommandButton("Frunk", viewModel::frontTrunk, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // flash lights
            CommandButton("Flash", viewModel::flashLights, buttonsEnabled, commandInProgress, Modifier.weight(1f))
            // vehicle info extended
            CommandButton("InfoEx", viewModel::vehicleData, buttonsEnabled, commandInProgress, Modifier.weight(1f))
        }
    }
}
