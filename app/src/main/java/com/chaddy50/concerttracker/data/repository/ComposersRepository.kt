package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.external.dataTransferObjects.ComposerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toRow
import com.chaddy50.concerttracker.data.local.dao.ComposerDao
import com.chaddy50.concerttracker.data.local.entity.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComposersRepository @Inject constructor(
    private val composerDao: ComposerDao
) {

    fun searchComposers(query: String): Flow<List<Composer>> =
        composerDao.searchComposers(query.trim()).map { list -> list.map { it.toDomain() } }

    /** Write-through upsert for composers. Composes inside a caller's transaction (no transaction of its own). */
    suspend fun upsert(composers: List<ComposerDto>) =
        composerDao.upsert(composers.map { it.toRow() })
}
