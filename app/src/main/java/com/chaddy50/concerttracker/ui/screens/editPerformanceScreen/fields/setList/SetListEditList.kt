package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.fields.setList

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.ui.composables.editableItemList.EditableItemList
import com.chaddy50.concerttracker.ui.composables.editableItemList.EditableItemRow
import com.chaddy50.concerttracker.ui.composables.LabeledOutlineCard
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.PendingSetListEntry

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
        EditableItemList(
            items = setList,
            key = { it.id },
            onMove = onMoveSetListEntry
        ) { entry, dragHandleModifier ->
            val composerNames = entry.work.composers.joinToString(", ") { it.sortName ?: it.name }
            val featuredPerformerLabels = entry.featuredPerformers.map { fp ->
                if (fp.role != null) "${fp.performer.name}, ${fp.role}" else fp.performer.name
            }
            EditableItemRow(
                title = entry.work.title,
                subtitle = composerNames.ifEmpty { null },
                labels = featuredPerformerLabels,
                onEditClick = { onEditSetListEntryClick(entry.id) },
                modifier = dragHandleModifier
            )
        }
        EditableItemList(
            items = pendingSetListEntries,
            key = { it.localId },
            onMove = onMovePendingSetListEntry
        ) { pendingEntry, dragHandleModifier ->
            val featuredPerformerLabels = pendingEntry.featuredPerformers.map { fp ->
                if (fp.role.isNotBlank()) "${fp.name}, ${fp.role}" else fp.name
            }
            EditableItemRow(
                title = pendingEntry.workTitle,
                subtitle = pendingEntry.composerName.ifEmpty { null },
                labels = featuredPerformerLabels,
                onEditClick = { onEditPendingSetListEntryClick(pendingEntry.localId) },
                modifier = dragHandleModifier
            )
        }
        TextButton(
            onClick = onAddSetListEntryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Entry")
        }
    }
}
