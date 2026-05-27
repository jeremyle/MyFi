package com.jeremyle.myfi.domain.model

data class Stock(
    val ticker: String,
    val shares: Int,
    val currentPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double
) {
    val isPositive: Boolean get() = priceChangePercent >= 0
}
