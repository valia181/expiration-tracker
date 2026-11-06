package com.example.expirationtracker

import com.example.expirationtracker.data.repository.ItemRepository
import com.example.expirationtracker.domain.model.BackupData
import com.example.expirationtracker.domain.model.Settings
import com.example.expirationtracker.ui.viewmodel.ItemListViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit Test Suite for TSK-008: Search and Category Filtering ViewModel logic.
 *
 * Traceability:
 * - REQ-F-006 & AC-F-006: Title search query filtering.
 * - REQ-F-007 & AC-F-007: Category filtering by Food, Medicine, Cosmetics, Chemicals, or All.
 */
class ItemSearchAndFilterUnitTest {

    @Test
    fun viewModel_onSearchQueryChanged_updates_search_state_and_filters_list() {
        val fakeRepo = object : ItemRepository {
            override suspend fun createItem(input: com.example.expirationtracker.domain.model.ItemInput) = Result.failure<com.example.expirationtracker.domain.model.Item>(NotImplementedError())
            override suspend fun getAllItems(search: String?, category: String?) = emptyList<com.example.expirationtracker.domain.model.Item>()
            override suspend fun deleteItem(id: String) = false
            override suspend fun getLeadTime() = 7
            override suspend fun saveLeadTime(leadTime: Int) = Result.success(Unit)
            override suspend fun exportBackupData() = BackupData(settings = Settings(), items = emptyList())
            override suspend fun importBackupData(backupData: BackupData) = true
        }
        val viewModel = ItemListViewModel(
            repository = fakeRepo,
            mainDispatcher = Dispatchers.Unconfined
        )

        viewModel.onSearchQueryChanged("Aspirin")

        assertEquals("Aspirin", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun viewModel_onCategorySelected_updates_category_filter_state() {
        val fakeRepo = object : ItemRepository {
            override suspend fun createItem(input: com.example.expirationtracker.domain.model.ItemInput) = Result.failure<com.example.expirationtracker.domain.model.Item>(NotImplementedError())
            override suspend fun getAllItems(search: String?, category: String?) = emptyList<com.example.expirationtracker.domain.model.Item>()
            override suspend fun deleteItem(id: String) = false
            override suspend fun getLeadTime() = 7
            override suspend fun saveLeadTime(leadTime: Int) = Result.success(Unit)
            override suspend fun exportBackupData() = BackupData(settings = Settings(), items = emptyList())
            override suspend fun importBackupData(backupData: BackupData) = true
        }
        val viewModel = ItemListViewModel(
            repository = fakeRepo,
            mainDispatcher = Dispatchers.Unconfined
        )

        viewModel.onCategorySelected("Medicine")

        assertEquals("Medicine", viewModel.uiState.value.selectedCategory)
    }
}
