package com.example.demoapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.profilertest.ui.theme.ProfilerTestTheme
import io.github.doubleddoge.pulsemonitor.ProfilerOverlay
//import com.example.profiler_overlay.metrics.memory.MemoryCollector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfilerTestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    MemoryTestScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Keep the profiler overlay attached to this Activity.
        // This is independent of the Compose UI below.
        ProfilerOverlay.attach(this)
    }
}

@Composable
fun MemoryTestScreen( modifier: Modifier = Modifier ) {
    /*
     * We keep the allocated ByteArrays in this list.
     *
     * If we don't keep a reference to them, the garbage collector
     * could eventually remove them and our RAM usage could fall again.
     */
    val allocatedMemory = remember {
        mutableStateListOf<ByteArray>()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Allocated test memory: " +
                    "${allocatedMemory.size * 10} MB"
        )

        Button(
            onClick = {

                /*
                 * Allocate another 10 MB.
                 *
                 * The reference is stored in allocatedMemory so
                 * that the memory remains allocated.
                 */
                val block = ByteArray(10 * 1024 * 1024)

                allocatedMemory.add(block)

                Log.d(
                    "ProfilerRAM",
                    "Allocated another 10 MB"
                )
            }
        ) {
            Text("Allocate 10 MB")
        }
    }
}