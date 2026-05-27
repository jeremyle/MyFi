package com.jeremyle.myfi.domain.model

/**
 * A single point on the portfolio value chart.
 * @param timestamp Unix epoch seconds (from Finnhub candle response)
 * @param value     Total portfolio value in USD at this moment
 */
data class ChartPoint(
    val timestamp: Long,
    val value: Double
)
