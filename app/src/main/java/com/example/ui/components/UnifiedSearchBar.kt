package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SearchManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchManager: SearchManager,
    modifier: Modifier = Modifier
) {
    val recentSearches by searchManager.recentSearches.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    
    val searchBackgroundColor = if (isSearchActive) Color(0xFF001F3F) else Color.White.copy(alpha = 0.15f)
    val searchBorderColor = if (isSearchActive) Color.Transparent else Color.White.copy(alpha = 0.5f)

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                onQueryChange(it)
                if (it.isNotEmpty()) {
                    // Update search history immediately - though we might want to debounce or update on search action
                    // But requirement says "Results should appear instantly while typing. No Search Button required."
                    // Let's add it to history if it's > 2 chars, or we can just let it be. 
                    // To avoid spamming history with "a", "ab", "abc", we update history only on unfocus or maybe a delay.
                    // Actually, we'll update history when user taps on a search result instead.
                }
            },
            placeholder = { Text("Search customers...", color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(searchBackgroundColor)
                .border(
                    width = 1.dp,
                    color = searchBorderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged { 
                    isSearchActive = it.isFocused 
                    if (!it.isFocused && searchQuery.isNotBlank()) {
                        searchManager.addSearchQuery(searchQuery)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            singleLine = true
        )

        if (recentSearches.isNotEmpty() && searchQuery.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Recent Searches", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentSearches) { search ->
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable {
                            onQueryChange(search)
                            searchManager.addSearchQuery(search)
                        }
                    ) {
                        Text(
                            text = search,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
