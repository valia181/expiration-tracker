package com.example.expirationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.api.ApiResponse
import com.example.expirationtracker.data.api.ItemApiController
import com.example.expirationtracker.data.repository.ItemRepository
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.usecase.GetSortedItemsWithStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val addDialogError: String? = null
)

/**
 * ViewModel managing the item list UI state, sorting, item creation, and status color calculations.
 * Traceability: TSK-001, TSK-003, TSK-005, REQ-F-001, REQ-F-002, REQ-F-003, REQ-F-004, REQ-F-005.
 */
class ItemListViewModel(
    private val repository: ItemRepository,
    private val getSortedItemsUseCase: GetSortedItemsWithStatusUseCase = GetSortedItemsWithStatusUseCase(),
    private val apiController: ItemApiController = ItemApiController(repository = repository)
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
        SystemLogger.info(TAG, "Loading items from repository.")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val rawItems = repository.getAllItems()
                val sortedWithStatus = getSortedItemsUseCase.execute(
                    items = rawItems,
                    currentDate = currentDate,
                    leadTimeDays = _uiState.value.notificationLeadTimeDays
                )

                _uiState.value = _uiState.value.copy(
                    items = sortedWithStatus,
                    isLoading = false
                )
                SystemLogger.info(
                    TAG,
                    "Loaded and rendered ${sortedWithStatus.size} items in chronological order."
                )
            } catch (e: Exception) {
                SystemLogger.error(TAG, "Failed to load items", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load items."
                )
            }
        }
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(isAddDialogOpen = true, addDialogError = null)
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(isAddDialogOpen = false, addDialogError = null)
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
        viewModelScope.launch {
            val input = ItemInput(
                title = title,
                expirationDate = expirationDate,
                category = category,
                storageLocation = storageLocation
            )

            SystemLogger.info(TAG, "Calling createItem API for title='$title'")
            val response: ApiResponse<Item> = apiController.createItem(input)

            if (response.isSuccessful) {
                SystemLogger.info(TAG, "Item created successfully via API: ${response.data?.id}")
                _uiState.value = _uiState.value.copy(isAddDialogOpen = false, addDialogError = null)
                loadItems()
            } else {
                val errorMsg = response.error?.message ?: "Failed to create item."
                SystemLogger.warn(TAG, "Item creation failed: $errorMsg")
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
        SystemLogger.info(TAG, "Requested deletion of item ID: $id")
        viewModelScope.launch {
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
