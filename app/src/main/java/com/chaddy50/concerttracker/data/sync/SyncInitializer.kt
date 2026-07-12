package com.chaddy50.concerttracker.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncInitializer @Inject constructor(
    private val connectivityObserver: ConnectivityObserver,
    private val syncScheduler: SyncScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            connectivityObserver.observe().collect { online ->
                if (online) syncScheduler.requestImmediateSync()
            }
        }
    }
}
