package com.example.demoapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.profilertest.ui.theme.ProfilerTestTheme
import io.github.doubleddoge.pulsemonitor.ui.ProfilerOverlay

class ScreenTwoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfilerTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BurnScreen(
                        title = "Screen 2",
                        nextLabel = "Go to Screen 3",
                        onNext = {
                            startActivity(Intent(this@ScreenTwoActivity, ScreenThreeActivity::class.java))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Show the profiler on this screen too
        ProfilerOverlay.attach(this)
    }
}