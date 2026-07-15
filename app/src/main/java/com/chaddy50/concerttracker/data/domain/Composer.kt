package com.chaddy50.concerttracker.data.domain

data class Composer(
    val id: String,
    val name: String,
    val sortName: String? = null,
    val openOpusId: String? = null,
    val epoch: String? = null
)
