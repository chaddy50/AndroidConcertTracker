package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.fields.performerList

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.ui.composables.editableItemList.EditableItemList
import com.chaddy50.concerttracker.ui.composables.editableItemList.EditableItemRow
import com.chaddy50.concerttracker.ui.composables.LabeledOutlineCard

@Composable
fun PerformerEditList(
    performers: List<Performer>,
    onAddPerformerClick: () -> Unit,
    onRemovePerformer: (String) -> Unit,
    onMovePerformer: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LabeledOutlineCard(
        label = stringResource(R.string.performance_form_performers_label),
        modifier = modifier
    ) {
        EditableItemList(
            items = performers,
            key = { it.id },
            onMove = onMovePerformer
        ) { performer, dragHandleModifier ->
            EditableItemRow(
                title = performer.name,
                subtitle = performer.specialty,
                onRemoveClick = { onRemovePerformer(performer.id) },
                modifier = dragHandleModifier
            )
        }
        TextButton(
            onClick = onAddPerformerClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.performance_form_add_performer))
        }
    }
}
