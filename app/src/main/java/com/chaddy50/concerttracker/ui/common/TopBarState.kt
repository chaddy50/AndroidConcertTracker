package com.chaddy50.concerttracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.RowScope

class TopBarState {
    var title by mutableStateOf("")
    var actions: @Composable RowScope.() -> Unit by mutableStateOf({})

    fun update(title: String, actions: @Composable RowScope.() -> Unit = {}) {
        this.title = title
        this.actions = actions
    }
}
