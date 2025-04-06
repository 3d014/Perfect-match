package com.example.coupleswipe.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coupleswipe.fragments.FilterSelection



class MoviesFilterViewModel : ViewModel() {
    // LiveData to store and observe filter selections
    private val _selectedFilters = MutableLiveData<List<FilterSelection>>(emptyList())
    val selectedFilters: LiveData<List<FilterSelection>> = _selectedFilters

    // Method to update selected filters
    fun updateFilters(filterName: String, values: List<Any>) {
        val currentList = _selectedFilters.value ?: mutableListOf()
        val newList = mutableListOf<FilterSelection>()

        // 1. Copy all filters except the one we're updating
        currentList.forEach { filter ->
            if (filter.filterName != filterName) {
                newList.add(filter)
            }
        }

        // 2. Add the updated filter (if values aren't empty)
        if (values.isNotEmpty()) {
            newList.add(FilterSelection(filterName, values))
        }

        // 3. Update LiveData
        _selectedFilters.value = newList
    }

    // Method to clear filters
    fun clearFilters() {
        _selectedFilters.value = emptyList()
    }

    // Method to get current filter values
    fun getCurrentFilters(): List<FilterSelection> {
        return _selectedFilters.value ?: emptyList()
    }
}