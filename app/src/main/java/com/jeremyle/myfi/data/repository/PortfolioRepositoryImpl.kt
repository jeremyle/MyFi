package com.jeremyle.myfi.data.repository

import android.content.Context
import com.jeremyle.myfi.domain.model.Portfolio
import com.jeremyle.myfi.domain.model.Stock
import com.jeremyle.myfi.domain.repository.PortfolioRepository
import org.json.JSONObject

class PortfolioRepositoryImpl(private val context: Context) : PortfolioRepository {

    override suspend fun getPortfolio(): Portfolio {
        val json = context.assets.open("portfolio.json").bufferedReader().use { it.readText() }
        return parse(json)
    }

    private fun parse(json: String): Portfolio {
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("stocks")
        val stocks = (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            Stock(
                ticker = s.getString("ticker"),
                shares = s.getInt("shares"),
                currentPrice = s.getDouble("currentPrice"),
                priceChange = s.getDouble("priceChange"),
                priceChangePercent = s.getDouble("priceChangePercent")
            )
        }
        return Portfolio(
            value = obj.getDouble("portfolioValue"),
            todayChange = obj.getDouble("todayChange"),
            todayChangePercent = obj.getDouble("todayChangePercent"),
            buyingPower = obj.getDouble("buyingPower"),
            stocks = stocks
        )
    }
}
