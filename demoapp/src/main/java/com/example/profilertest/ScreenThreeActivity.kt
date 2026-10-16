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

class ScreenThreeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfilerTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BurnScreen(
                        title = "Screen 3",
                        nextLabel = "Back to Screen 1",
                        onNext = {
                            // CLEAR_TOP returns to the existing Screen 1 and closes Screens 2 and 3
                            val intent = Intent(this@ScreenThreeActivity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}