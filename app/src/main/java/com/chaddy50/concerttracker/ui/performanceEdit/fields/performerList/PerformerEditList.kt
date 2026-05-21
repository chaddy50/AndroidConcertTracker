package com.chaddy50.concerttracker.ui.performanceEdit.fields.performerList

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.ui.common.LabeledOutlineCard

@Composable
fun PerformerEditList(
    performers: List<Performer>,
    onAddPerformerClick: () -> Unit,
    onRemovePerformer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LabeledOutlineCard(
        label = stringResource(R.string.performance_form_performers_label),
        modifier = modifier
    ) {
        performers.forEachIndexed { index, performer ->
            PerformerListRow(
                performer = performer,
                onRemove = { onRemovePerformer(performer.id) }
            )
            if (index < performers.lastIndex) {
                HorizontalDivider()
            }
        }
        TextButton(
            onClick = onAddPerformerClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.performance_form_add_performer))
        }
    }
}
