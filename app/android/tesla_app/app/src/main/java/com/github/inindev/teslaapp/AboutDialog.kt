package com.github.inindev.teslaapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import com.github.inindev.teslaapp.ui.theme.TeslaRed

/**
 * Displays a professional and branded About dialog for the Tesla App, featuring animations and Tesla-specific branding.
 * Includes accessibility enhancements for screen readers and touch targets.
 *
 * @param viewModel The MainViewModel managing the dialog state.
 * @param onDismiss Callback to dismiss the dialog.
 */
@Composable
fun AboutDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    // use animatedvisibility for a smooth fade-in/out effect
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp) // increased padding for a more spacious look
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // tesla app icon
                    Icon(
                        painter = painterResource(id = R.drawable.car), // Load Tesla logo from drawable resources
                        contentDescription = "Tesla App Logo",
                        modifier = Modifier
                            .size(64.dp) // larger icon for prominence
                            .padding(bottom = 16.dp),
                        tint = TeslaRed // use tesla-specific color for branding
                    )

                    // title
                    Text(
                        text = "About Tesla App",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // version and copyright
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Version: 0.7",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Copyright © 2025, John Clark <inindev@gmail.com>",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // description
                    Text(
                        text = "A third-party Tesla vehicle control app using the Tesla Fleet API.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // visit tesla developer link (as a button for better ux)
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.tesla.com"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .height(48.dp)
                            .widthIn(min = 180.dp) // ensure minimum touch target width
                            .semantics { contentDescription = "Visit Tesla Developer website" }
                    ) {
                        Text(
                            text = "Visit Tesla Developer",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // close button
                    TextButton(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(48.dp)
                            .widthIn(min = 96.dp) // ensure minimum touch target width
                            .semantics { contentDescription = "Close About dialog" }
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
