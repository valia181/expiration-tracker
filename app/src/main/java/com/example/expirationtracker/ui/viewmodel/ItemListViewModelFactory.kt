package com.example.expirationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expirationtracker.data.repository.ItemRepository

/**
 * ViewModelProvider.Factory for instantiating ItemListViewModel with its ItemRepository dependency.
 * Traceability: REQ-NF-007 (MVVM Architecture).
 */
class ItemListViewModelFactory(
    private val repository: ItemRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemListViewModel::class.java)) {
            return ItemListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
