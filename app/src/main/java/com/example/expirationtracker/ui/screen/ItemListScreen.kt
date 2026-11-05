package com.example.expirationtracker.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
 * Main Chronological Item List Screen.
 * Traceability: REQ-F-001, REQ-F-004, REQ-F-005, REQ-NF-001, REQ-NF-005, REQ-NF-006, TSK-005.
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            key = { it.id }
                        ) { item ->
                            ItemCard(
                                item = item,
                                onDeleteClick = { viewModel.deleteItem(it) }
                            )
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
    }
}
