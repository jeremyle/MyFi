package com.jeremyle.myfi.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.jeremyle.myfi.domain.model.Stock

data class StockQuoteDto(
    @SerializedName("c")  val currentPrice: Double,   // current price
    @SerializedName("d")  val change: Double,          // change
    @SerializedName("dp") val changePercent: Double,   // percent change
)

fun StockQuoteDto.toStock(ticker: String, shares: Int) = Stock(
    ticker = ticker,
    shares = shares,
    currentPrice = currentPrice,
    priceChange = change,
    priceChangePercent = changePercent
)
