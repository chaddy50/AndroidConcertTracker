package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.fields.setList

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.ui.composables.LabeledOutlineCard
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.PendingSetListEntry
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun SetListEditList(
    setList: List<SetListEntry>,
    pendingSetListEntries: List<PendingSetListEntry>,
    onAddSetListEntryClick: () -> Unit,
    onEditSetListEntryClick: (entryId: String) -> Unit,
    onEditPendingSetListEntryClick: (localId: String) -> Unit,
    onMoveSetListEntry: (from: Int, to: Int) -> Unit,
    onMovePendingSetListEntry: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LabeledOutlineCard(
        label = "Set List",
        modifier = modifier
    ) {
        ReorderableColumn(
            list = setList,
            onSettle = onMoveSetListEntry
        ) { _, entry, _ ->
            key(entry.id) {
                ReorderableItem {
                    val composerNames = entry.work.composers.joinToString(", ") { it.sortName ?: it.name }
                    SetListEntryRow(
                        workTitle = entry.work.title,
                        composerNames = composerNames,
                        featuredPerformerLabels = entry.featuredPerformers.map { fp ->
                            if (fp.role != null) "${fp.performer.name}, ${fp.role}" else fp.performer.name
                        },
                        onEditClick = { onEditSetListEntryClick(entry.id) },
                        dragHandleModifier = Modifier.draggableHandle()
                    )
                }
            }
        }
        ReorderableColumn(
            list = pendingSetListEntries,
            onSettle = onMovePendingSetListEntry
        ) { _, pendingEntry, _ ->
            key(pendingEntry.localId) {
                ReorderableItem {
                    SetListEntryRow(
                        workTitle = pendingEntry.workTitle,
                        composerNames = pendingEntry.composerName,
                        featuredPerformerLabels = pendingEntry.featuredPerformers.map { fp ->
                            if (fp.role.isNotBlank()) "${fp.name}, ${fp.role}" else fp.name
                        },
                        onEditClick = { onEditPendingSetListEntryClick(pendingEntry.localId) },
                        dragHandleModifier = Modifier.draggableHandle()
                    )
                }
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
