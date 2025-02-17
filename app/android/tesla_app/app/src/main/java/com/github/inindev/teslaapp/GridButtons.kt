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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GridButtons(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // first row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // lock doors
            Button(
                onClick = { viewModel.lockDoors() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Lock")
            }

            // climate on
            Button(
                onClick = { viewModel.climateOn() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Climate")
            }

            // charger door close
            Button(
                onClick = { viewModel.chargeClose() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("CP Close")
            }
        }

        // second row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // unlock doors
            Button(
                onClick = { viewModel.unlockDoors() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Unlock")
            }

            // vent windows
            Button(
                onClick = { viewModel.ventWindows() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Windows")
            }

            // wake up
            Button(
                onClick = { viewModel.wakeUp() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Wake Up")
            }
        }

        // third row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // actuate trunk
            Button(
                onClick = { viewModel.rearTrunk() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Trunk")
            }

            // honk horn
            Button(
                onClick = { viewModel.honkHorn() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Honk")
            }

            // vehicle
            Button(
                onClick = { viewModel.vehicle() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Info")
            }
        }

        // fourth row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // open frunk
            Button(
                onClick = { viewModel.frontTrunk() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Frunk")
            }

            // flash lights
            Button(
                onClick = { viewModel.flashLights() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Flash")
            }

            // vehicle
            Button(
                onClick = { viewModel.vehicleData() },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("InfoEx")
            }
        }
    }
}
