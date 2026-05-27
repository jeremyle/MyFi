package com.jeremyle.myfi.data.repository

import android.content.Context
import com.jeremyle.myfi.data.remote.FinnhubApi
import com.jeremyle.myfi.data.remote.dto.CandleDataDto
import com.jeremyle.myfi.data.remote.dto.toStock
import com.jeremyle.myfi.domain.model.ChartPeriod
import com.jeremyle.myfi.domain.model.ChartPoint
import com.jeremyle.myfi.domain.model.Portfolio
import com.jeremyle.myfi.domain.repository.PortfolioRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

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

        val portfolioValue = stocks.sumOf { it.shares * it.currentPrice }
        val todayChange    = stocks.sumOf { it.shares * it.priceChange }
        val previousValue  = portfolioValue - todayChange
        val todayChangePct = if (previousValue != 0.0) (todayChange / previousValue) * 100 else 0.0

        return Portfolio(
            value              = portfolioValue,
            todayChange        = todayChange,
            todayChangePercent = todayChangePct,
            buyingPower        = buyingPower,
            stocks             = stocks
        )
    }

    override suspend fun getPortfolioChart(period: ChartPeriod): List<ChartPoint> {
        val (_, holdings) = parseHoldings()
        val (from, to, resolution) = period.toApiParams()

        // Fetch candle data for all holdings in parallel
        val candlesByTicker: Map<String, CandleDataDto> = coroutineScope {
            holdings.map { (ticker, _) ->
                async { ticker to api.getCandle(ticker, resolution, from, to) }
            }.awaitAll()
        }.toMap()

        // Use the timestamps from the first holding that has data as the canonical timeline
        val baseCandle = candlesByTicker.values.firstOrNull { it.hasData } ?: return emptyList()
        val timestamps = baseCandle.timestamps!!

        // Build a lookup table: ticker -> index -> close price
        // (indexes correspond 1-to-1 with timestamps when the candle is "ok")
        val closeLookup: Map<String, List<Double>> = candlesByTicker
            .filterValues { it.hasData }
            .mapValues { (_, dto) -> dto.closes!! }

        // For each timestamp, sum holdings value = Σ(shares × close)
        return timestamps.mapIndexed { idx, ts ->
            val totalValue = holdings.sumOf { (ticker, shares) ->
                val closes = closeLookup[ticker]
                val price = closes?.getOrElse(idx) { closes.last() } ?: 0.0
                shares * price
            }
            ChartPoint(timestamp = ts, value = totalValue)
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

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

// ─── ChartPeriod → Finnhub API params ─────────────────────────────────────────

private fun ChartPeriod.toApiParams(): Triple<Long, Long, String> {
    val nyZone = ZoneId.of("America/New_York")
    val now    = ZonedDateTime.now(nyZone)

    return when (this) {
        ChartPeriod.ONE_DAY -> {
            // Most-recent weekday trading session (9:30 AM – 4:00 PM ET)
            val today = now.toLocalDate()
            val tradingDay = when (today.dayOfWeek) {
                DayOfWeek.SATURDAY -> today.minusDays(1)
                DayOfWeek.SUNDAY   -> today.minusDays(2)
                else               -> today
            }
            val open  = ZonedDateTime.of(tradingDay, LocalTime.of(9, 30), nyZone)
            val close = ZonedDateTime.of(tradingDay, LocalTime.of(16, 0), nyZone)
            val to    = minOf(now.toEpochSecond(), close.toEpochSecond())
            Triple(open.toEpochSecond(), to, "5")
        }

        ChartPeriod.ONE_WEEK -> {
            Triple(now.minusDays(7).toEpochSecond(), now.toEpochSecond(), "60")
        }

        ChartPeriod.ONE_MONTH -> {
            Triple(now.minusDays(30).toEpochSecond(), now.toEpochSecond(), "D")
        }

        ChartPeriod.THREE_MONTHS -> {
            Triple(now.minusDays(90).toEpochSecond(), now.toEpochSecond(), "D")
        }

        ChartPeriod.YTD -> {
            val startOfYear = ZonedDateTime.of(
                now.toLocalDate().withDayOfYear(1), LocalTime.MIDNIGHT, nyZone
            )
            Triple(startOfYear.toEpochSecond(), now.toEpochSecond(), "D")
        }

        ChartPeriod.ONE_YEAR -> {
            Triple(now.minusDays(365).toEpochSecond(), now.toEpochSecond(), "D")
        }
    }
}
