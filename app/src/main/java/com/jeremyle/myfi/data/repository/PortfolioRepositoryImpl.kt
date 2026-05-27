package com.jeremyle.myfi.data.repository

import android.content.Context
import com.jeremyle.myfi.data.remote.FinnhubApi
import com.jeremyle.myfi.data.remote.dto.toStock
import com.jeremyle.myfi.domain.model.Portfolio
import com.jeremyle.myfi.domain.repository.PortfolioRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

class PortfolioRepositoryImpl(
    private val context: Context,
    private val api: FinnhubApi
) : PortfolioRepository {

    override suspend fun getPortfolio(): Portfolio {
        val (buyingPower, holdings) = parseHoldings()

        // Fetch all quotes in parallel
        val stocks = coroutineScope {
            holdings.map { (ticker, shares) ->
                async { api.getQuote(ticker).toStock(ticker, shares) }
            }.awaitAll()
        }

        val portfolioValue  = stocks.sumOf { it.shares * it.currentPrice }
        val todayChange     = stocks.sumOf { it.shares * it.priceChange }
        val previousValue   = portfolioValue - todayChange
        val todayChangePct  = if (previousValue != 0.0) (todayChange / previousValue) * 100 else 0.0

        return Portfolio(
            value = portfolioValue,
            todayChange = todayChange,
            todayChangePercent = todayChangePct,
            buyingPower = buyingPower,
            stocks = stocks
        )
    }

    private fun parseHoldings(): Pair<Double, List<Pair<String, Int>>> {
        val json = context.assets.open("portfolio.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("holdings")
        val holdings = (0 until arr.length()).map { i ->
            val h = arr.getJSONObject(i)
            h.getString("ticker") to h.getInt("shares")
        }
        return obj.getDouble("buyingPower") to holdings
    }
}
