package com.example.expirationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.repository.ItemRepository
import com.example.expirationtracker.domain.model.Item
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
    val errorMessage: String? = null
)

/**
 * ViewModel managing the item list UI state, sorting, and status color calculations.
 * Traceability: TSK-005, REQ-F-004, REQ-F-005, REQ-NF-007 (MVVM).
 */
class ItemListViewModel(
    private val repository: ItemRepository,
    private val getSortedItemsUseCase: GetSortedItemsWithStatusUseCase = GetSortedItemsWithStatusUseCase()
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

    fun updateLeadTime(leadTimeDays: Int, currentDate: LocalDate = LocalDate.now()) {
        SystemLogger.info(TAG, "Updating lead time to $leadTimeDays days.")
        _uiState.value = _uiState.value.copy(notificationLeadTimeDays = leadTimeDays)
        loadItems(currentDate)
    }

    fun deleteItem(id: String) {
        SystemLogger.info(TAG, "Requested deletion of item ID: $id")
        viewModelScope.launch {
            val success = repository.deleteItem(id)
            if (success) {
                SystemLogger.info(TAG, "Item successfully deleted: $id")
                loadItems()
            } else {
                SystemLogger.warn(TAG, "Failed to delete item: $id")
            }
        }
    }
}
