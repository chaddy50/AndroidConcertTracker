package com.chaddy50.concerttracker.navigation.topBarActions.syncStatusIndicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.domain.SyncJob
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.util.formatDate

/** One row in the sync-status popup: the op's action + entity, with the target's identifying context
 *  beneath it and a "Failed" tag when the op was permanently rejected. */
@Composable
fun SyncJobRow(job: SyncJob) {
    val context = LocalContext.current
    val detail = listOfNotNull(
        job.description,
        job.performanceDateIso?.let { formatDate(it, context) }
    ).joinToString(" · ").ifEmpty { null }

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = jobLabel(job), style = MaterialTheme.typography.bodyMedium)
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (job.failed) {
            Text(
                text = "Failed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun jobLabel(job: SyncJob): String {
    val action = when (job.operationType) {
        SyncOperationType.CREATE -> "Create"
        SyncOperationType.UPDATE -> "Update"
        SyncOperationType.DELETE -> "Delete"
    }
    val entity = when (job.entityType) {
        SyncEntityType.PERFORMANCE -> "performance"
        SyncEntityType.SET_LIST_ENTRY -> "set list entry"
        SyncEntityType.PERFORMER -> "performer"
        SyncEntityType.WORK -> "work"
    }
    return "$action $entity"
}
