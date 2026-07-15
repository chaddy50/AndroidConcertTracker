package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch

import com.chaddy50.concerttracker.data.external.api.OpenOpusWork
import com.chaddy50.concerttracker.data.domain.Work

/** One row in the merged work picker list; the user can't distinguish the two origins. */
sealed interface WorkSearchResult {
    val title: String
    val genre: String?

    data class Local(val work: Work) : WorkSearchResult {
        override val title: String get() = work.title
        override val genre: String? get() = work.genre
    }

    data class FromApi(val work: OpenOpusWork) : WorkSearchResult {
        override val title: String get() = work.title
        override val genre: String? get() = work.genre
    }
}
