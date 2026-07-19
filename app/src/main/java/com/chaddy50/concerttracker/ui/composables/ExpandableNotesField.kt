package com.chaddy50.concerttracker.ui.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import kotlinx.coroutines.delay

@Composable
fun ExpandableNotesField(
    notes: String,
    label: String,
    emptyText: String,
    isEditing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onNotesChange: (String) -> Unit
) {
    var editingNotes by remember { mutableStateOf(TextFieldValue(notes)) }
    var hasFocusedOnce by remember { mutableStateOf(false) }

    fun collapse() {
        hasFocusedOnce = false
        onNotesChange(editingNotes.text)
        onEditingChange(false)
    }
    val focusRequester = remember { FocusRequester() }
    val fadeOutDuration = 150
    val expandDuration = 300

    LaunchedEffect(isEditing) {
        if (isEditing) {
            editingNotes = TextFieldValue(notes, selection = TextRange(notes.length))
            delay((fadeOutDuration + expandDuration).toLong())
            focusRequester.requestFocus()
        } else if (hasFocusedOnce) {
            hasFocusedOnce = false
            onNotesChange(editingNotes.text)
        }
    }

    AnimatedContent(
        targetState = isEditing,
        transitionSpec = {
            val enterTransition = if (targetState) {
                expandVertically(animationSpec = tween(durationMillis = expandDuration, delayMillis = fadeOutDuration))
            } else {
                fadeIn(animationSpec = tween(durationMillis = expandDuration, delayMillis = fadeOutDuration))
            }
            enterTransition togetherWith fadeOut(animationSpec = tween(durationMillis = fadeOutDuration))
        },
        label = "notesFieldTransition"
    ) { editing ->
        if (editing) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editingNotes,
                    onValueChange = { editingNotes = it },
                    label = { Text(stringResource(R.string.performance_form_notes_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                hasFocusedOnce = true
                            } else if (hasFocusedOnce) {
                                collapse()
                            }
                        }
                )
                TextButton(
                    onClick = { collapse() },
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (notes.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
