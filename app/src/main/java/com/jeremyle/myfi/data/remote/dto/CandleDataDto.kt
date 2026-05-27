package com.jeremyle.myfi.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Finnhub GET /stock/candle response.
 * When the market is closed or no data exists, [status] == "no_data"
 * and [closes] / [timestamps] are null.
 */
data class CandleDataDto(
    @SerializedName("c") val closes: List<Double>?,
    @SerializedName("t") val timestamps: List<Long>?,
    @SerializedName("s") val status: String
) {
    val hasData: Boolean get() = status == "ok" && !closes.isNullOrEmpty() && !timestamps.isNullOrEmpty()
}
