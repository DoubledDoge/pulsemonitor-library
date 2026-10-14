package com.example.demoapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// One screen layout, reused by all 3 screens
@Composable
fun BurnScreen(
    title: String,          // e.g. "Screen 1"
    nextLabel: String,      // Text on the navigation button
    onNext: () -> Unit,     // What happens when it's tapped
    modifier: Modifier = Modifier
) {
    // Memory shown on screen. Updates when you allocate or free
    var heldMb by remember { mutableIntStateOf(ResourceBurner.heldMemoryMb) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text("Held test memory: $heldMb MB")

        // CPU
        Button(onClick = { ResourceBurner.burnCpuInBackground() }) {
            Text("Burn CPU (background, 5 s)")
        }
        Button(onClick = { ResourceBurner.burnCpuOnMainThread() }) {
            Text("Burn CPU (main thread, 2 s)")
        }

        // Memory
        Button(onClick = {
            ResourceBurner.allocateMemory()
            heldMb = ResourceBurner.heldMemoryMb
        }) {
            Text("Allocate ${ResourceBurner.ALLOCATION_MB} MB")
        }
        Button(onClick = {
            ResourceBurner.freeMemory()
            heldMb = ResourceBurner.heldMemoryMb
        }) {
            Text("Free memory")
        }

        // Network
        Button(onClick = { ResourceBurner.downloadData() }) {
            Text("Download 10 MB")
        }
        // Sent trigger
        Button(onClick = { ResourceBurner.uploadData() }) {
            Text("Upload ${ResourceBurner.UPLOAD_MB} MB")
        }

        // Navigation (outlined so it looks different from the burn buttons)
        OutlinedButton(onClick = onNext) {
            Text(nextLabel)
        }
    }
}