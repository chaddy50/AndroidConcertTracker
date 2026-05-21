package com.chaddy50.concerttracker.ui.performanceEdit.setList

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.ui.common.LabeledOutlineCard

@Composable
fun SetListEditList(
    setList: List<SetListEntry>,
    isCreateMode: Boolean,
    onAddSetListEntryClick: () -> Unit,
    onEditSetListEntryClick: (entryId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LabeledOutlineCard(
        label = "Set List",
        modifier = modifier
    ) {
        if (isCreateMode) {
            Text(
                text = "Save performance first to manage set list entries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            setList.forEachIndexed { index, entry ->
                SetListEntryRow(
                    entry = entry,
                    onEditClick = { onEditSetListEntryClick(entry.id) }
                )
                if (index < setList.lastIndex) {
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
}
