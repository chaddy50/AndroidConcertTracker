package com.chaddy50.concerttracker.ui.composables.searchFields.nominatimSearch

import com.chaddy50.concerttracker.data.external.api.NominatimResult
import com.chaddy50.concerttracker.data.domain.Venue

/** One row in the merged venue picker list; the user can't distinguish the two origins. */
sealed interface VenueSearchResult {
    val name: String
    val address: String?

    data class Local(val venue: Venue) : VenueSearchResult {
        override val name: String get() = venue.name
        override val address: String? get() = null
    }

    data class FromApi(val result: NominatimResult) : VenueSearchResult {
        override val name: String get() = result.name
        override val address: String get() = result.displayName
    }
}
