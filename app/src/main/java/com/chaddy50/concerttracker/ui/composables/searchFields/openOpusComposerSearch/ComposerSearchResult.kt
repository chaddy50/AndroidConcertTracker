package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.external.api.OpenOpusComposer

sealed interface ComposerSearchResult {
    val name: String
    val epoch: String?
    val composerId: String?
    val openOpusId: String?

    data class Local(val composer: Composer) : ComposerSearchResult {
        override val name: String get() = composer.name
        override val epoch: String? get() = null
        override val composerId: String get() = composer.id
        override val openOpusId: String? get() = composer.openOpusId
    }

    data class FromApi(val composer: OpenOpusComposer) : ComposerSearchResult {
        override val name: String get() = composer.completeName
        override val epoch: String? get() = composer.epoch
        override val composerId: String? get() = null
        override val openOpusId: String get() = composer.id
    }
}
