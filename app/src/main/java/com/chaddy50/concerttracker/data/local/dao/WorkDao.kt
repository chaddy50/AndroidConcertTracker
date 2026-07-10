package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chaddy50.concerttracker.data.local.entity.WorkComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import com.chaddy50.concerttracker.data.local.relation.WorkWithComposers
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {

    @Upsert
    suspend fun upsert(works: List<WorkEntity>)

    @Upsert
    suspend fun upsertWorkComposers(crossRefs: List<WorkComposerEntity>)

    @Transaction
    @Query("SELECT * FROM works WHERE id = :id")
    suspend fun getWorkWithComposers(id: String): WorkWithComposers?

    @Transaction
    @Query(
        "SELECT w.* FROM works w " +
            "INNER JOIN work_composers wc ON wc.workId = w.id " +
            "WHERE wc.composerId = :composerId AND w.title LIKE '%' || :query || '%' " +
            "ORDER BY w.title COLLATE NOCASE ASC"
    )
    fun searchWorksForComposer(composerId: String, query: String): Flow<List<WorkWithComposers>>
}
