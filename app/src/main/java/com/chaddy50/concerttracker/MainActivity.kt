package com.chaddy50.concerttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chaddy50.concerttracker.navigation.AppNavigation
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConcertTrackerTheme {
                AppNavigation()
            }
        }
    }
}
