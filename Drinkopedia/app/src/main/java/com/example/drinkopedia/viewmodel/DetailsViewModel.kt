package com.example.drinkopedia.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drinkopedia.model.DrinkDetail
import com.example.drinkopedia.network.RetrofitClient
import kotlinx.coroutines.launch

class DetailsViewModel : ViewModel() {
    var drinkDetail by mutableStateOf<DrinkDetail?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun fetchDrinkDetail(drinkId: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = RetrofitClient.api.lookupDetails(drinkId)
                drinkDetail = response.drinks.firstOrNull()
            } catch (e: Exception) {
                drinkDetail = null
            } finally {
                isLoading = false
            }
        }
    }
}