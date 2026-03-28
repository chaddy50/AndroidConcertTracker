package com.chaddy50.concerttracker.ui.performanceEdit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDropdown(
    selectedStatus: PerformanceStatus,
    onStatusSelected: (PerformanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedStatus.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.performance_form_status_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            PerformanceStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name) },
                    onClick = {
                        onStatusSelected(status)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun StatusDropdownPreview() {
    ConcertTrackerTheme {
        StatusDropdown(
            selectedStatus = PerformanceStatus.ATTENDED,
            onStatusSelected = {}
        )
    }
}
// endregion