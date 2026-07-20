package com.chaddy50.concerttracker.ui.composables.editableItemList

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun <T> EditableItemList(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, dragHandleModifier: Modifier) -> Unit
) {
    if (items.isEmpty()) return
    val stableItems = items.toList()
    ReorderableColumn(
        list = stableItems,
        onSettle = onMove,
        modifier = modifier
    ) { _, item, _ ->
        key(key(item)) {
            ReorderableItem {
                itemContent(item, Modifier.draggableHandle())
            }
        }
    }
}
