package com.chaddy50.concerttracker.data.sync

data class SyncResult(
    val numberOfOperationsProcessed: Int,
    val didFinish: Boolean
)
