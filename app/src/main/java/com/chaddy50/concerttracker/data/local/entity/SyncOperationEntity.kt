package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chaddy50.concerttracker.data.domain.SyncJob
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [com.chaddy50.concerttracker.data.enum.SyncEntityType] name. */
    val entityType: String,
    /** [com.chaddy50.concerttracker.data.enum.SyncOperationType] name. */
    val operationType: String,
    val entityId: String,
    val payloadJson: String?,
    val createdAt: String,
    val attemptCount: Int = 0,
    val lastError: String? = null
)

internal fun SyncOperationEntity.toDomain() = SyncJob(
    id = id,
    entityId = entityId,
    entityType = SyncEntityType.valueOf(entityType),
    operationType = SyncOperationType.valueOf(operationType),
    failed = lastError != null
)
