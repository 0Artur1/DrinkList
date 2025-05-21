package com.example.drinkopedia.viewmodel
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.example.drinkopedia.network.RetrofitClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.drinkopedia.model.DrinkDetail
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class PostViewModel(application: Application) : AndroidViewModel(application){
    var postListalcoholic by mutableStateOf<List<DrinkDetail>>(emptyList())
        private set

    var postListnonalcoholic by mutableStateOf<List<DrinkDetail>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isLoading2 by mutableStateOf(true)
        private set

    var isLoading3 by mutableStateOf(true)
        private set

    var postListAll by mutableStateOf<List<DrinkDetail>>(emptyList())
        private set

    var filtereddrinks by mutableStateOf<List<DrinkDetail>>(emptyList())
        private set

    var filtereddrinks1 by mutableStateOf<List<DrinkDetail>>(emptyList())
        private set

    var filtereddrinks2 by mutableStateOf<List<DrinkDetail>>(emptyList())
        private set

    enum class FilterMode {
        NAME, DESCRIPTION, INGREDIENTS
    }

    private val _filterMode = MutableStateFlow(FilterMode.NAME)
    val filterMode: StateFlow<FilterMode> = _filterMode

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategoryIndex = MutableStateFlow(0)
    val selectedCategoryIndex: StateFlow<Int> = _selectedCategoryIndex
    val categories = listOf("Home", "Alcoholic", "Nonalcoholic")

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected

    val loadingProgress = MutableStateFlow(0f)

    fun fetchAlcoholic() {
        viewModelScope.launch {
            try {
                isLoading = true
                isLoading2 = true
                loadingProgress.value = 0f
                val response = RetrofitClient.api.getAlcoholics()
                val basicDrinks = response.drinks
                val semaphore = Semaphore(permits = 1)

                val total = basicDrinks.size
                var completed = 0
                Log.d("API", "Fetched ${basicDrinks.size} alcoholic base drinks")

                val detailedDrinks: List<DrinkDetail> = basicDrinks.mapNotNull { drink ->
                    async {
                        semaphore.withPermit {
                            try {
                                delay(250L)
                                val details = RetrofitClient.api.lookupDetails(drink.idDrink)
                                details.drinks.firstOrNull()
                            } catch (e: Exception) {
                                Log.e("DrinkFetch", "Error fetching drink ${drink.idDrink}", e)
                                null
                            } finally {
                                completed++
                                loadingProgress.value = completed.toFloat() / total
                            }

                        }

                    }
                }.awaitAll().filterNotNull()

                Log.d("API", "Detailed drinks fetched alcoholic: ${detailedDrinks.size}")
                postListalcoholic = detailedDrinks
                setCategory("Alcoholic")
            } catch (e: Exception) {
                postListalcoholic = emptyList()
            } finally {
                isLoading2 = false
                if(isLoading3 == false)
                {
                    val complete=5
                    loadingProgress.value = complete.toFloat()
                }
                updateFilteredDrinks()
            }
        }
    }

    fun fetchNonAlcoholic() {
        viewModelScope.launch {
            try {
                isLoading = true
                isLoading3 = true
                val response = RetrofitClient.api.getNonAlcoholics()
                val basicDrinks = response.drinks

                val semaphore = Semaphore(permits = 1)
                Log.d("API", "Fetched ${basicDrinks.size} nonalcoholic base drinks")

                val detailedDrinks: List<DrinkDetail> = basicDrinks.mapNotNull { drink ->
                    async {
                        semaphore.withPermit {
                            try {
                                delay(250L)
                                val details = RetrofitClient.api.lookupDetails(drink.idDrink)
                                details.drinks.firstOrNull()
                            } catch (e: Exception) {
                                Log.e("DrinkFetch", "Error fetching drink ${drink.idDrink}", e)
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()

                Log.d("API", "Detailed drinks fetched nonalcoholic: ${detailedDrinks.size}")

                postListnonalcoholic = detailedDrinks
            } catch (e: Exception) {
                postListnonalcoholic = emptyList()
            } finally {
                isLoading3 = false
                if(isLoading2 == false)
                {
                    val complete=5
                    loadingProgress.value = complete.toFloat()
                }
                updateFilteredDrinks()
            }
        }
    }
    fun setCategory(category: String) {
        postListAll = when(category) {
            "Alcoholic" -> postListalcoholic
            "Nonalcoholic" -> postListnonalcoholic
            else -> emptyList()
        }
        updateFilteredDrinks()
    }

    fun setSelectedCategory(index: Int) {
        if (_selectedCategoryIndex.value != index) {
            _selectedCategoryIndex.value = index
        }
    }
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateFilteredDrinks()
    }

    fun updateSearchText(value: String) {
        _searchText.value = value
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
        updateFilteredDrinks()
    }

    private fun updateFilteredDrinks() {
        filtereddrinks1 = getFilteredDrinks(postListalcoholic)
        filtereddrinks2 = getFilteredDrinks(postListnonalcoholic)
        filtereddrinks = getFilteredDrinks(postListAll)
    }

    fun getFilteredDrinks(lista: List<DrinkDetail>): List<DrinkDetail> {
        val query = _searchQuery.value.trim().lowercase()
        if (query.isEmpty()) return lista

        return when (_filterMode.value) {
            FilterMode.NAME -> lista.filter {
                it.strDrink.lowercase().contains(query)
            }
            FilterMode.DESCRIPTION -> lista.filter {
                it.strInstructions?.lowercase()?.contains(query) == true
            }
            FilterMode.INGREDIENTS -> lista.filter { drink ->
                val ingredients = listOf(
                    drink.strIngredient1, drink.strIngredient2, drink.strIngredient3,
                    drink.strIngredient4, drink.strIngredient5, drink.strIngredient6,
                    drink.strIngredient7, drink.strIngredient8, drink.strIngredient9,
                    drink.strIngredient10
                ).filterNotNull().map { it.lowercase() }

                ingredients.any { it.contains(query) }
            }
        }
    }
    private fun monitorNetwork() {
        val isCurrentlyConnected = connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } == true
        _isConnected.value = isCurrentlyConnected
        if(_isConnected.value == false)
        {
            val complete=5
            loadingProgress.value = complete.toFloat()
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
                // Po odzyskaniu połączenia spróbuj ponownie pobrać dane
                viewModelScope.launch {
                    if (postListalcoholic.size<30) fetchAlcoholic()
                    if (postListnonalcoholic.size<20) fetchNonAlcoholic()
                }
            }

            override fun onLost(network: Network) {
                _isConnected.value = false
            }
        }

        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    init {
        monitorNetwork()
    }
}