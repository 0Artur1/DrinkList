package com.example.drinkopedia.model

data class ResponseAPI(
    val drinks: List<Drink>
)
data class DrinkDetailResponse(
    val drinks: List<DrinkDetail>
)

data class Drink(
    val idDrink: String,
    val strDrink: String,
    val strDrinkThumb: String
)

data class DrinkDetail(
    val idDrink: String,
    val strDrink: String,
    val strDrinkThumb: String,
    val strInstructions: String?,
    val strIngredient1: String?,
    val strIngredient2: String?,
    val strIngredient3: String?,
    val strIngredient4: String?,
    val strIngredient5: String?,
    val strIngredient6: String?,
    val strIngredient7: String?,
    val strIngredient8: String?,
    val strIngredient9: String?,
    val strIngredient10: String?,
    val strMeasure1: String?,
    val strMeasure2: String?,
    val strMeasure3: String?,
    val strMeasure4: String?,
    val strMeasure5: String?,
    val strMeasure6: String?,
    val strMeasure7: String?,
    val strMeasure8: String?,
    val strMeasure9: String?,
    val strMeasure10: String?,
)
