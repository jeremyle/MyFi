package com.jeremyle.myfi.data.remote

import com.jeremyle.myfi.data.remote.dto.CandleDataDto
import com.jeremyle.myfi.data.remote.dto.StockQuoteDto
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApi {

    @GET("quote")
    suspend fun getQuote(@Query("symbol") symbol: String): StockQuoteDto

    /**
     * Fetch OHLCV candle data for a symbol.
     * @param resolution  1 | 5 | 15 | 30 | 60 | D | W | M
     * @param from        Unix epoch seconds (inclusive start)
     * @param to          Unix epoch seconds (inclusive end)
     */
    @GET("stock/candle")
    suspend fun getCandle(
        @Query("symbol")     symbol: String,
        @Query("resolution") resolution: String,
        @Query("from")       from: Long,
        @Query("to")         to: Long
    ): CandleDataDto
}
