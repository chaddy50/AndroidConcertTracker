package com.chaddy50.concerttracker.data.domain

data class Venue(
    val id: String,
    val name: String,
    val osmId: String?,
    val osmType: String?,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val website: String? = null
)
