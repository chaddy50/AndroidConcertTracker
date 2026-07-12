package com.chaddy50.concerttracker.data.enum

enum class SyncState {
    SYNCED,
    PENDING,
    FAILED,
    PENDING_DELETE;

    /** The stored string form of this state (inverse of [fromName]). */
    fun toName(): String = name

    companion object {
        fun fromName(name: String): SyncState = entries.firstOrNull { it.name == name } ?: SYNCED
    }
}
