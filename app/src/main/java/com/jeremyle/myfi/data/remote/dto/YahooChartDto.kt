package com.jeremyle.myfi.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Yahoo Finance GET /v8/finance/chart/{symbol} response ───────────────────

data class YahooChartResponse(
    @SerializedName("chart") val chart: YahooChart,
)

data class YahooChart(
    @SerializedName("result") val result: List<YahooChartResult>?,
    @SerializedName("error") val error: String?,
)

data class YahooChartResult(
    @SerializedName("timestamp") val timestamps: List<Long>?,
    @SerializedName("indicators") val indicators: YahooIndicators?,
)

data class YahooIndicators(
    @SerializedName("quote") val quote: List<YahooQuote>?,
)

data class YahooQuote(
    // Yahoo returns null for the current (still-open) candle
    @SerializedName("close") val closes: List<Double?>?,
)

/**
 * Flatten the chart result into a list of (unixTimestamp, closePrice) pairs,
 * discarding any candles where close is null (i.e. the in-progress bar).
 */
fun YahooChartResult.toCloseTimeSeries(): List<Pair<Long, Double>> {
    val timestamps = timestamps ?: return emptyList()
    val closes = indicators?.quote?.firstOrNull()?.closes ?: return emptyList()
    return timestamps.indices.mapNotNull { i ->
        val close = closes.getOrNull(i) ?: return@mapNotNull null
        timestamps[i] to close
    }
}
