package com.example.expirationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.api.ApiResponse
import com.example.expirationtracker.data.api.ItemApiController
import com.example.expirationtracker.data.repository.ItemRepository
import com.example.expirationtracker.domain.model.BackupData
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.usecase.GetSortedItemsWithStatusUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * UI State for the Item List screen.
 */
data class ItemListUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val notificationLeadTimeDays: Int = 7,
    val errorMessage: String? = null,
    val isAddDialogOpen: Boolean = false,
    val addDialogError: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val itemToDeleteId: String? = null,
    val isDeleteConfirmationOpen: Boolean = false,
    val isSettingsDialogOpen: Boolean = false,
    val itemToEdit: Item? = null
)

/**
 * ViewModel managing the item list UI state, sorting, item creation, and status color calculations.
 * Traceability: TSK-001, TSK-003, TSK-005, REQ-F-001, REQ-F-002, REQ-F-003, REQ-F-004, REQ-F-005.
 */
class ItemListViewModel(
    private val repository: ItemRepository,
    private val getSortedItemsUseCase: GetSortedItemsWithStatusUseCase = GetSortedItemsWithStatusUseCase(),
    private val apiController: ItemApiController = ItemApiController(repository = repository),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    companion object {
        private const val TAG = "ItemListViewModel"
    }

    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState: StateFlow<ItemListUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems(currentDate: LocalDate = LocalDate.now()) {
        val search = _uiState.value.searchQuery.ifBlank { null }
        val category = _uiState.value.selectedCategory
        SystemLogger.info(TAG, "Loading items from repository with search='$search', category='$category'.")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch(mainDispatcher) {
            try {
                // Fetch latest global lead time settings on IO dispatcher (REQ-NF-001)
                val settingsResponse = withContext(Dispatchers.IO) { apiController.getSettings() }
                val leadTimeDays = if (settingsResponse.isSuccessful && settingsResponse.data != null) {
                    settingsResponse.data.notificationLeadTime
                } else {
                    _uiState.value.notificationLeadTimeDays
                }

                val response: ApiResponse<List<Item>> = withContext(Dispatchers.IO) {
                    apiController.getItems(search = search, category = category)
                }
                if (response.isSuccessful) {
                    val rawItems = response.data ?: emptyList()
                    val sortedWithStatus = withContext(Dispatchers.Default) {
                        getSortedItemsUseCase.execute(
                            items = rawItems,
                            currentDate = currentDate,
                            leadTimeDays = leadTimeDays
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        items = sortedWithStatus,
                        notificationLeadTimeDays = leadTimeDays,
                        isLoading = false
                    )
                    SystemLogger.info(
                        TAG,
                        "Loaded and rendered ${sortedWithStatus.size} items matching search='$search', category='$category' (leadTime=$leadTimeDays)."
                    )
                } else {
                    val errorMsg = response.error?.message ?: "Failed to load items."
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            } catch (e: Exception) {
                SystemLogger.error(TAG, "Failed to load items", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load items."
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        SystemLogger.info(TAG, "Search query changed: '$query'")
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadItems()
    }

    fun onCategorySelected(category: String?) {
        SystemLogger.info(TAG, "Category filter selected: '$category'")
        val categoryToSet = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) null else category
        _uiState.value = _uiState.value.copy(selectedCategory = categoryToSet)
        loadItems()
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(isAddDialogOpen = true, itemToEdit = null, addDialogError = null)
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(isAddDialogOpen = false, itemToEdit = null, addDialogError = null)
    }

    fun openEditDialog(item: Item) {
        SystemLogger.info(TAG, "Opening edit dialog for item ID: ${item.id}")
        _uiState.value = _uiState.value.copy(
            itemToEdit = item,
            isAddDialogOpen = true,
            addDialogError = null
        )
    }

    fun openSettingsDialog() {
        SystemLogger.info(TAG, "Opening settings dialog.")
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = true)
    }

    fun closeSettingsDialog() {
        SystemLogger.info(TAG, "Closing settings dialog.")
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = false)
    }

    fun updateLeadTimeSetting(leadTime: Int) {
        SystemLogger.info(TAG, "Updating lead time setting to $leadTime days.")
        viewModelScope.launch(mainDispatcher) {
            val response = apiController.updateSettings(com.example.expirationtracker.domain.model.Settings(notificationLeadTime = leadTime))
            if (response.isSuccessful) {
                SystemLogger.info(TAG, "Successfully updated lead time setting via API: $leadTime")
                _uiState.value = _uiState.value.copy(
                    notificationLeadTimeDays = leadTime,
                    isSettingsDialogOpen = false
                )
                loadItems() // Triggers dynamic recalculation of StatusColor for existing items per AC-F-008
            } else {
                val errorMsg = response.error?.message ?: "Failed to update settings."
                SystemLogger.warn(TAG, "Failed to update lead time setting: $errorMsg")
                _uiState.value = _uiState.value.copy(errorMessage = errorMsg, isSettingsDialogOpen = false)
            }
        }
    }

    /**
     * Creates a new item via ItemApiController per OpenAPI contract (REQ-F-001, AC-F-001).
     * On 201 Created: closes dialog and reloads chronological list.
     * On 400 Bad Request: exposes ErrorResponse.message to UI (AC-F-002, AC-F-003).
     */
    fun addItem(
        title: String,
        expirationDate: String,
        category: Category,
        storageLocation: String?
    ) {
        viewModelScope.launch(mainDispatcher) {
            val input = ItemInput(
                title = title,
                expirationDate = expirationDate,
                category = category,
                storageLocation = storageLocation
            )

            val editItem = _uiState.value.itemToEdit
            val response = if (editItem != null) {
                SystemLogger.info(TAG, "Calling updateItem API for ID='${editItem.id}', title='$title'")
                apiController.updateItem(editItem.id, input)
            } else {
                SystemLogger.info(TAG, "Calling createItem API for title='$title'")
                apiController.createItem(input)
            }

            if (response.isSuccessful) {
                SystemLogger.info(TAG, "Item successfully saved via API: ${response.data?.id}")
                _uiState.value = _uiState.value.copy(isAddDialogOpen = false, itemToEdit = null, addDialogError = null)
                loadItems()
            } else {
                val errorMsg = response.error?.message ?: "Failed to save item."
                SystemLogger.warn(TAG, "Item save failed: $errorMsg")
                _uiState.value = _uiState.value.copy(addDialogError = errorMsg)
            }
        }
    }

    fun updateLeadTime(leadTimeDays: Int, currentDate: LocalDate = LocalDate.now()) {
        SystemLogger.info(TAG, "Updating lead time to $leadTimeDays days.")
        _uiState.value = _uiState.value.copy(notificationLeadTimeDays = leadTimeDays)
        loadItems(currentDate)
    }

    fun deleteItem(id: String) {
        requestDelete(id)
    }

    fun requestDelete(id: String) {
        SystemLogger.info(TAG, "Requested deletion confirmation for item ID: $id")
        _uiState.value = _uiState.value.copy(
            itemToDeleteId = id,
            isDeleteConfirmationOpen = true
        )
    }

    fun dismissDeleteConfirmation() {
        SystemLogger.info(TAG, "Deletion confirmation dismissed by user.")
        _uiState.value = _uiState.value.copy(
            itemToDeleteId = null,
            isDeleteConfirmationOpen = false
        )
    }

    fun confirmDelete() {
        val id = _uiState.value.itemToDeleteId
        if (id != null) {
            SystemLogger.info(TAG, "Confirmed deletion of item ID: $id")
            _uiState.value = _uiState.value.copy(
                itemToDeleteId = null,
                isDeleteConfirmationOpen = false
            )
            viewModelScope.launch(mainDispatcher) {
                val response = apiController.deleteItem(id)
                if (response.isSuccessful) {
                    SystemLogger.info(TAG, "Item successfully deleted via API: $id")
                    loadItems()
                } else {
                    val errorMsg = response.error?.message ?: "Failed to delete item: $id"
                    SystemLogger.warn(TAG, "Failed to delete item via API: $errorMsg")
                    _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
                }
            }
        }
    }

    fun exportBackup(onResult: (BackupData?) -> Unit) {
        SystemLogger.info(TAG, "Export backup requested.")
        viewModelScope.launch(mainDispatcher) {
            val response = apiController.exportBackup()
            if (response.isSuccessful) {
                SystemLogger.info(TAG, "Export backup payload generated successfully.")
                onResult(response.data)
            } else {
                SystemLogger.warn(TAG, "Export backup failed: ${response.error?.message}")
                _uiState.value = _uiState.value.copy(errorMessage = response.error?.message ?: "Export failed.")
                onResult(null)
            }
        }
    }

    fun importBackup(backupData: BackupData) {
        SystemLogger.info(TAG, "Import backup requested with ${backupData.items.size} items.")
        viewModelScope.launch(mainDispatcher) {
            val response = apiController.importBackup(backupData)
            if (response.isSuccessful) {
                SystemLogger.info(TAG, "Import backup succeeded. Refreshing items list.")
                loadItems()
            } else {
                val errorMsg = response.error?.message ?: "Failed to import backup."
                SystemLogger.warn(TAG, "Import backup failed: $errorMsg")
                _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
            }
        }
    }

    fun exportBackupToUri(uri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        SystemLogger.info(TAG, "Exporting backup to SAF URI: $uri")
        viewModelScope.launch(mainDispatcher) {
            val response = withContext(Dispatchers.IO) { apiController.exportBackup() }
            if (response.isSuccessful && response.data != null) {
                val success = withContext(Dispatchers.IO) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val jsonString = com.example.expirationtracker.util.JsonUtil.toJson(response.data)
                            outputStream.write(jsonString.toByteArray())
                        }
                        true
                    } catch (e: Exception) {
                        SystemLogger.error(TAG, "Failed to write JSON backup to SAF URI", e)
                        false
                    }
                }
                if (success) {
                    SystemLogger.info(TAG, "Successfully wrote JSON backup to SAF URI: $uri")
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Failed to write backup file.")
                }
            } else {
                val errorMsg = response.error?.message ?: "Failed to export backup."
                SystemLogger.warn(TAG, "Export backup failed: $errorMsg")
                _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
            }
        }
    }

    fun importBackupFromUri(uri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        SystemLogger.info(TAG, "Importing backup from SAF URI: $uri")
        viewModelScope.launch(mainDispatcher) {
            val backupData = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        com.example.expirationtracker.util.JsonUtil.fromJson(jsonString)
                    }
                } catch (e: Exception) {
                    SystemLogger.error(TAG, "Error reading SAF import file stream", e)
                    null
                }
            }

            if (backupData != null) {
                val response = withContext(Dispatchers.IO) { apiController.importBackup(backupData) }
                if (response.isSuccessful) {
                    SystemLogger.info(TAG, "Import backup succeeded. Refreshing items list.")
                    loadItems()
                } else {
                    val errorMsg = response.error?.message ?: "Failed to import backup."
                    SystemLogger.warn(TAG, "Import backup failed: $errorMsg")
                    _uiState.value = _uiState.value.copy(errorMessage = errorMsg)
                }
            } else {
                SystemLogger.warn(TAG, "Invalid or corrupted backup file at URI: $uri")
                _uiState.value = _uiState.value.copy(errorMessage = "Invalid or corrupted backup file.")
            }
        }
    }
}
