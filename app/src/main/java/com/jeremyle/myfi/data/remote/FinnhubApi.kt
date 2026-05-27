package com.jeremyle.myfi.data.remote

import com.jeremyle.myfi.data.remote.dto.StockQuoteDto
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApi {

    @GET("quote")
    suspend fun getQuote(@Query("symbol") symbol: String): StockQuoteDto
}
