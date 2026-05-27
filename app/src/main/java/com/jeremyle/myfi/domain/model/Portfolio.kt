package com.jeremyle.myfi.domain.model

data class Portfolio(
    val value: Double,
    val todayChange: Double,
    val todayChangePercent: Double,
    val buyingPower: Double,
    val stocks: List<Stock>
)
