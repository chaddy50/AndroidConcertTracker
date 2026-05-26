package com.chaddy50.concerttracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chaddy50.concerttracker.navigation.NavigationHost
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import dagger.hilt.android.HiltAndroidApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConcertTrackerTheme {
                NavigationHost()
            }
        }
    }
}

@HiltAndroidApp
class ConcertTrackerApplication : Application()