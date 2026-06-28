package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchManager(context: Context) {
    private val prefs = context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
    
    private val _recentSearches = MutableStateFlow<List<String>>(loadSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private fun loadSearches(): List<String> {
        val str = prefs.getString("history", "") ?: ""
        return if (str.isEmpty()) emptyList() else str.split("|||")
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        
        val current = loadSearches().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(10)
        
        prefs.edit().putString("history", limited.joinToString("|||")).apply()
        _recentSearches.value = limited
    }
}
