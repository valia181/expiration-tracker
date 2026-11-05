package com.example.expirationtracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.ui.viewmodel.ItemListViewModel

/**
 * Main Chronological Item List Screen with Search Bar, Category Filtering, Deletion Confirmation, and Settings.
 * Traceability: REQ-F-001, REQ-F-004, REQ-F-005, REQ-F-006, REQ-F-007, REQ-F-008, REQ-F-011, AC-F-008, AC-F-011, REQ-NF-001, REQ-NF-002, REQ-NF-005, REQ-NF-006.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    viewModel: ItemListViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(uiState.items.size) {
        SystemLogger.info(
            "ItemListScreen",
            "Rendered ItemListScreen with ${uiState.items.size} chronological items."
        )
        onDispose { }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Expiration Tracker") },
                actions = {
                    IconButton(
                        onClick = { viewModel.openSettingsDialog() },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_item")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar (REQ-F-006, AC-F-006)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("input_search"),
                placeholder = { Text("Search items...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true
            )

            // Category Filter Chips (REQ-F-007, AC-F-007)
            val categories = listOf("All", "Food", "Medicine", "Cosmetics", "Chemicals")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { label ->
                    val isSelected = if (label == "All") {
                        uiState.selectedCategory == null
                    } else {
                        uiState.selectedCategory.equals(label, ignoreCase = true)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(if (label == "All") null else label) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("filter_chip_${label.lowercase()}")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.items.isEmpty() -> {
                        Text(
                            text = "No items tracked yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("empty_list_text")
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("item_list_lazy_column"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.items,
                                key = { item -> item.id }
                            ) { item ->
                                ItemCard(
                                    item = item,
                                    onDeleteClick = { id -> viewModel.requestDelete(id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Item Dialog
        if (uiState.isAddDialogOpen) {
            AddItemDialog(
                errorMessage = uiState.addDialogError,
                onDismiss = { viewModel.closeAddDialog() },
                onSave = { title, expirationDate, category, storageLocation ->
                    viewModel.addItem(title, expirationDate, category, storageLocation)
                }
            )
        }

        // Deletion Confirmation Dialog (AC-F-011, REQ-F-011)
        if (uiState.isDeleteConfirmationOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirmation() },
                title = { Text("Delete Item") },
                text = { Text("Are you sure you want to delete this item?") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDelete() },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("btn_confirm_delete")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeleteConfirmation() },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("btn_cancel_delete")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Settings Dialog (REQ-F-008, AC-F-008)
        if (uiState.isSettingsDialogOpen) {
            SettingsDialog(
                currentLeadTime = uiState.notificationLeadTimeDays,
                onDismiss = { viewModel.closeSettingsDialog() },
                onSave = { leadTime -> viewModel.updateLeadTimeSetting(leadTime) }
            )
        }
    }
}
