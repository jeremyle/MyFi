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
            // Resolve to the most recent session that has (or had) data:
            //   • during market hours  → from open to now  (live intraday)
            //   • after market close   → from open to 4 PM (full session replay)
            //   • before today's open  → previous trading day's full session
            //   • weekend              → most recent Friday's full session
            val (open, close) = latestTradingSession(now, nyZone)
            val to = minOf(now.toEpochSecond(), close.toEpochSecond())
            Triple(open.toEpochSecond(), to, "60")   // 1-hour candles
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

/**
 * Returns the open/close timestamps of the most recent NYSE trading session.
 *
 * Rules:
 *  1. Skip weekends back to Friday.
 *  2. If [now] is before today's 9:30 AM open, step back one more trading day
 *     (Monday → Friday, otherwise → previous calendar day which is always a weekday
 *     because we already stripped weekends in step 1).
 */
private fun latestTradingSession(
    now: ZonedDateTime,
    nyZone: ZoneId,
): Pair<ZonedDateTime, ZonedDateTime> {
    var day = now.toLocalDate()

    // 1. Normalise weekends → most recent Friday
    day = when (day.dayOfWeek) {
        DayOfWeek.SATURDAY -> day.minusDays(1)
        DayOfWeek.SUNDAY   -> day.minusDays(2)
        else               -> day
    }

    // 2. If the market hasn't opened yet on this day, use the previous trading day
    val todayOpen = ZonedDateTime.of(day, LocalTime.of(9, 30), nyZone)
    if (now.isBefore(todayOpen)) {
        day = when (day.dayOfWeek) {
            DayOfWeek.MONDAY -> day.minusDays(3)  // Monday pre-open → Friday
            else             -> day.minusDays(1)  // weekday pre-open → previous weekday
        }
    }

    val open  = ZonedDateTime.of(day, LocalTime.of(9, 30), nyZone)
    val close = ZonedDateTime.of(day, LocalTime.of(16, 0),  nyZone)
    return open to close
}
