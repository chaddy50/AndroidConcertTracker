package com.chaddy50.concerttracker.data.enum

import kotlinx.serialization.Serializable

@Serializable
enum class PerformerType {
    ORCHESTRA, ENSEMBLE, SOLO, CHORUS, CONDUCTOR, OTHER
}
