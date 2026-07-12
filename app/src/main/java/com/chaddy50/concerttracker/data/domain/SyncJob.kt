package com.chaddy50.concerttracker.data.domain

import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType

/**
 * A single queued outbox operation, surfaced to the UI for the sync-status popup.
 *
 * @param entityId the id of the local entity the op targets, used to resolve [description] /
 *   [performanceDateIso].
 * @param description a best-effort name identifying the target (work title, performer name, venue),
 *   or null if it couldn't be resolved.
 * @param performanceDateIso the ISO date of the associated performance (the target itself for a
 *   PERFORMANCE op, its parent performance for a SET_LIST_ENTRY op), or null. Formatted by the UI.
 */
data class SyncJob(
    val id: Long,
    val entityId: String,
    val entityType: SyncEntityType,
    val operationType: SyncOperationType,
    val failed: Boolean,
    val description: String? = null,
    val performanceDateIso: String? = null
)
