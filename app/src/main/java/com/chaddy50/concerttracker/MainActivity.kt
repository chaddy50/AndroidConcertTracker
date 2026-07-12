package com.chaddy50.concerttracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chaddy50.concerttracker.data.sync.SyncInitializer
import com.chaddy50.concerttracker.navigation.NavigationHost
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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
class ConcertTrackerApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncInitializer: SyncInitializer

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        syncInitializer.start()
    }
}