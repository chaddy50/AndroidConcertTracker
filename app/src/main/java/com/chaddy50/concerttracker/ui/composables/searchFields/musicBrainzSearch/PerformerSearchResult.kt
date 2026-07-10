package com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch

import com.chaddy50.concerttracker.data.external.api.MusicBrainzResult
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.PerformerType

/** One row in the merged performer picker list; the user can't distinguish the two origins. */
sealed interface PerformerSearchResult {
    val name: String
    val type: String?

    data class Local(val performer: Performer) : PerformerSearchResult {
        override val name: String get() = performer.name
        override val type: String? get() = performer.specialty ?: formatType(performer.type)
    }

    data class FromApi(val result: MusicBrainzResult) : PerformerSearchResult {
        override val name: String get() = result.name
        override val type: String? get() = result.description ?: result.performerType?.let(::formatType)
    }
}

private fun formatType(type: PerformerType): String =
    "(${type.name.lowercase().replaceFirstChar { it.uppercase() }})"
