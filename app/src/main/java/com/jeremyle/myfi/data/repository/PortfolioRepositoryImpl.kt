package com.jeremyle.myfi.data.repository

import android.content.Context
import com.jeremyle.myfi.data.remote.FinnhubApi
import com.jeremyle.myfi.data.remote.YahooFinanceApi
import com.jeremyle.myfi.data.remote.dto.toCloseTimeSeries
import com.jeremyle.myfi.data.remote.dto.toStock
import com.jeremyle.myfi.domain.model.ChartPeriod
import com.jeremyle.myfi.domain.model.ChartPoint
import com.jeremyle.myfi.domain.model.Portfolio
import com.jeremyle.myfi.domain.repository.PortfolioRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import org.json.JSONObject

class PortfolioRepositoryImpl(
    private val context: Context,
    private val finnhubApi: FinnhubApi,
    private val yahooApi: YahooFinanceApi,
) : PortfolioRepository {

    // ─── Portfolio (real-time quotes via Finnhub) ─────────────────────────────

    override suspend fun getPortfolio(): Portfolio {
        val (buyingPower, holdings) = parseHoldings()

        val stocks = coroutineScope {
            holdings.map { (ticker, shares) ->
                async { finnhubApi.getQuote(ticker).toStock(ticker, shares) }
            }.awaitAll()
        }

        val portfolioValue = stocks.sumOf { it.shares * it.currentPrice }
        val todayChange    = stocks.sumOf { it.shares * it.priceChange }
        val previousValue  = portfolioValue - todayChange
        val todayChangePct = if (previousValue != 0.0) (todayChange / previousValue) * 100 else 0.0

        return Portfolio(
            value              = portfolioValue,
            todayChange        = todayChange,
            todayChangePercent = todayChangePct,
            buyingPower        = buyingPower,
            stocks             = stocks,
        )
    }

    // ─── Chart (historical candles via Yahoo Finance — free, no key) ──────────

    override suspend fun getPortfolioChart(period: ChartPeriod): List<ChartPoint> {
        val (_, holdings) = parseHoldings()
        val (interval, range) = period.toYahooParams()

        // Fetch chart data for all holdings in parallel.
        // supervisorScope + runCatching: one failed ticker never cancels the others.
        val seriesByTicker: Map<String, List<Pair<Long, Double>>> = supervisorScope {
            holdings.map { (ticker, _) ->
                async {
                    ticker to runCatching {
                        yahooApi.getChart(ticker, interval, range)
                            .chart.result
                            ?.firstOrNull()
                            ?.toCloseTimeSeries()
                            ?: emptyList()
                    }.getOrDefault(emptyList())
                }
            }.awaitAll()
        }.toMap()

        // Use the timestamp spine from the first ticker that has data
        val baseTimeline = seriesByTicker.values.firstOrNull { it.isNotEmpty() }
            ?: return emptyList()

        // For each timestamp, sum holdings value = Σ(shares × close)
        return baseTimeline.mapIndexed { idx, (ts, _) ->
            val totalValue = holdings.sumOf { (ticker, shares) ->
                val series = seriesByTicker[ticker]
                val price  = series?.getOrElse(idx) { series.last() }?.second ?: 0.0
                shares * price
            }
            ChartPoint(timestamp = ts, value = totalValue)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun parseHoldings(): Pair<Double, List<Pair<String, Int>>> {
        val json = context.assets.open("portfolio.json").bufferedReader().use { it.readText() }
        val obj  = JSONObject(json)
        val arr  = obj.getJSONArray("holdings")
        val holdings = (0 until arr.length()).map { i ->
            val h = arr.getJSONObject(i)
            h.getString("ticker") to h.getInt("shares")
        }
        return obj.getDouble("buyingPower") to holdings
    }
}

// ─── ChartPeriod → Yahoo Finance params ──────────────────────────────────────

/**
 * Maps each period to a (interval, range) pair accepted by Yahoo Finance's
 * GET /v8/finance/chart/{symbol}?interval=...&range=... endpoint.
 */
private fun ChartPeriod.toYahooParams(): Pair<String, String> = when (this) {
    ChartPeriod.ONE_DAY      -> "1h"  to "1d"
    ChartPeriod.ONE_WEEK     -> "1h"  to "5d"
    ChartPeriod.ONE_MONTH    -> "1d"  to "1mo"
    ChartPeriod.THREE_MONTHS -> "1d"  to "3mo"
    ChartPeriod.YTD          -> "1d"  to "ytd"
    ChartPeriod.ONE_YEAR     -> "1d"  to "1y"
}
