package com.example.expirationtracker.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Settings Dialog allowing selection of global notification lead time (1, 3, 7, or 14 days).
 * Traceability: REQ-F-008, AC-F-005, AC-F-008.
 */
@Composable
fun SettingsDialog(
    currentLeadTime: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var selectedLeadTime by remember { mutableIntStateOf(currentLeadTime) }
    val options = listOf(1, 3, 7, 14)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Lead Time") },
        text = {
            Column {
                Text(
                    text = "Select advance warning period (days):",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                for (option in options) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLeadTime = option }
                            .padding(vertical = 4.dp)
                            .testTag("lead_time_option_$option"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedLeadTime == option),
                            onClick = { selectedLeadTime = option }
                        )
                        Text(
                            text = "$option days",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedLeadTime) },
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("btn_save_settings")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("btn_cancel_settings")
            ) {
                Text("Cancel")
            }
        }
    )
}
