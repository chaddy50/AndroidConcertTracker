package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.fields.setList

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.ui.composables.LabeledOutlineCard
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.PendingSetListEntry

@Composable
fun SetListEditList(
    setList: List<SetListEntry>,
    pendingSetListEntries: List<PendingSetListEntry>,
    onAddSetListEntryClick: () -> Unit,
    onEditSetListEntryClick: (entryId: String) -> Unit,
    onEditPendingSetListEntryClick: (localId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LabeledOutlineCard(
        label = "Set List",
        modifier = modifier
    ) {
        setList.forEachIndexed { index, entry ->
            val composerNames = entry.work.composers.joinToString(", ") { it.shortName ?: it.name }
            SetListEntryRow(
                workTitle = entry.work.title,
                composerNames = composerNames,
                featuredPerformerLabels = entry.featuredPerformers.map { fp ->
                    if (fp.role != null) "${fp.performer.name}, ${fp.role}" else fp.performer.name
                },
                onEditClick = { onEditSetListEntryClick(entry.id) }
            )

            if (index < setList.lastIndex) {
                HorizontalDivider()
            }
        }
        pendingSetListEntries.forEachIndexed { index, pendingEntry ->
            SetListEntryRow(
                workTitle = pendingEntry.workTitle,
                composerNames = "",
                featuredPerformerLabels = pendingEntry.featuredPerformers.map { fp ->
                    if (fp.role.isNotBlank()) "${fp.name}, ${fp.role}" else fp.name
                },
                onEditClick = { onEditPendingSetListEntryClick(pendingEntry.localId) }
            )
            if (index < pendingSetListEntries.lastIndex) {
                HorizontalDivider()
            }
        }

        TextButton(
            onClick = onAddSetListEntryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Entry")
        }
    }
}
