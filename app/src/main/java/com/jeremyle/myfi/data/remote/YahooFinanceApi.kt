package com.jeremyle.myfi.data.remote

import com.jeremyle.myfi.data.remote.dto.YahooChartResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Unofficial Yahoo Finance chart API — free, no API key required.
 * Base URL: https://query1.finance.yahoo.com/
 */
interface YahooFinanceApi {

    /**
     * @param symbol   Ticker (e.g. "SPY", "QQQ")
     * @param interval Bar size: "1h", "1d", "1wk", "1mo"
     * @param range    How far back: "1d", "5d", "1mo", "3mo", "ytd", "1y", "2y", "5y"
     */
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("range") range: String,
    ): YahooChartResponse
}
