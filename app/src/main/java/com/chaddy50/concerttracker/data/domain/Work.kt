package com.chaddy50.concerttracker.data.domain

data class Work(
    val id: String,
    val title: String,
    val composers: List<Composer> = emptyList(),
    val openOpusId: String? = null
)
