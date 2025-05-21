package com.example.drinkopedia.network

import com.example.drinkopedia.model.DrinkDetailResponse
import com.example.drinkopedia.model.ResponseAPI
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/json/v1/1/filter.php?a=Alcoholic")
    suspend fun getAlcoholics(): ResponseAPI

    @GET("api/json/v1/1/filter.php?a=Non_Alcoholic")
    suspend fun getNonAlcoholics(): ResponseAPI

    @GET("api/json/v1/1/lookup.php")
    suspend fun lookupDetails(@Query("i") id: String): DrinkDetailResponse
}