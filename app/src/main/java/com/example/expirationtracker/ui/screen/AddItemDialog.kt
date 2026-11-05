package com.example.expirationtracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.expirationtracker.domain.model.Category

/**
 * Add Item Dialog Composable.
 * Traceability: REQ-F-001, REQ-F-002, REQ-F-003, AC-F-001, AC-F-002, AC-F-003.
 * Adheres to Material Design 3 and WCAG 2.1 AA accessibility guidelines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, expirationDate: String, category: Category, storageLocation: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.FOOD) }
    var storageLocation by remember { mutableStateOf("") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Tracked Item",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Input (REQ-F-001, REQ-F-002)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Title *") },
                    placeholder = { Text("e.g. Fresh Milk, Aspirin") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title"),
                    singleLine = true
                )

                // Expiration Date Input (REQ-F-001, REQ-F-003)
                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { expirationDate = it },
                    label = { Text("Expiration Date (YYYY-MM-DD) *") },
                    placeholder = { Text("2027-01-01") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_expiration_date"),
                    singleLine = true
                )

                // Category Dropdown (Predefined: Food, Medicine, Cosmetics, Chemicals)
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("input_category")
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        Category.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Optional Storage Location Input (REQ-F-001)
                OutlinedTextField(
                    value = storageLocation,
                    onValueChange = { storageLocation = it },
                    label = { Text("Storage Location (Optional)") },
                    placeholder = { Text("e.g. Fridge, First Aid Kit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_storage_location"),
                    singleLine = true
                )

                // Visibly display ErrorResponse message on UI (AC-F-002, AC-F-003)
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_message")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        expirationDate,
                        selectedCategory,
                        storageLocation.ifBlank { null }
                    )
                },
                modifier = Modifier.testTag("btn_save_item")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_item")
            ) {
                Text("Cancel")
            }
        }
    )
}
