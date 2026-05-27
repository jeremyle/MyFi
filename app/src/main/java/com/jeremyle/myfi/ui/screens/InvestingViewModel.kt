package com.jeremyle.myfi.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyle.myfi.data.repository.PortfolioRepositoryImpl
import com.jeremyle.myfi.di.NetworkModule
import com.jeremyle.myfi.domain.model.ChartPeriod
import com.jeremyle.myfi.domain.model.ChartPoint
import com.jeremyle.myfi.domain.model.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InvestingUiState(
    val portfolioValue: Double = 0.0,
    val todayChange: Double = 0.0,
    val todayChangePercent: Double = 0.0,
    val buyingPower: Double = 0.0,
    val stocks: List<Stock> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Chart
    val chartPoints: List<ChartPoint> = emptyList(),
    val selectedPeriod: ChartPeriod = ChartPeriod.ONE_DAY,
    val isChartLoading: Boolean = true,
    val chartError: String? = null,
    // Change for the currently selected period (derived from chart points)
    val periodChange: Double = 0.0,
    val periodChangePercent: Double = 0.0,
)

class InvestingViewModel(application: Application) : AndroidViewModel(application) {

    // TODO: Replace with Hilt-injected PortfolioRepository once DI is wired up
    private val repository = PortfolioRepositoryImpl(
        context    = application,
        finnhubApi = NetworkModule.provideFinnhubApi(),
        yahooApi   = NetworkModule.provideYahooFinanceApi(),
    )

    private val _uiState = MutableStateFlow(InvestingUiState())
    val uiState: StateFlow<InvestingUiState> = _uiState.asStateFlow()

    init {
        loadAll(ChartPeriod.ONE_DAY)
    }

    /** Load portfolio quotes and chart data concurrently. */
    private fun loadAll(period: ChartPeriod) {
        viewModelScope.launch {
            // Kick off both fetches in parallel
            withContext(Dispatchers.IO) {
                val portfolioDeferred = async {
                    runCatching { repository.getPortfolio() }
                }
                val chartDeferred = async {
                    runCatching { repository.getPortfolioChart(period) }
                }

                val portfolioResult = portfolioDeferred.await()
                val chartResult     = chartDeferred.await()

                portfolioResult.fold(
                    onSuccess = { portfolio ->
                        _uiState.update {
                            it.copy(
                                portfolioValue     = portfolio.value,
                                todayChange        = portfolio.todayChange,
                                todayChangePercent = portfolio.todayChangePercent,
                                buyingPower        = portfolio.buyingPower,
                                stocks             = portfolio.stocks,
                                isLoading          = false
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                )

                chartResult.fold(
                    onSuccess = { points ->
                        val (pChange, pChangePct) = periodChangeFrom(points)
                        _uiState.update {
                            it.copy(
                                chartPoints          = points,
                                isChartLoading       = false,
                                selectedPeriod       = period,
                                periodChange         = pChange,
                                periodChangePercent  = pChangePct,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(chartError = e.message, isChartLoading = false) }
                    }
                )
            }
        }
    }

    /** Returns (absoluteChange, percentChange) for the given chart point series. */
    private fun periodChangeFrom(points: List<ChartPoint>): Pair<Double, Double> {
        if (points.size < 2) return 0.0 to 0.0
        val start  = points.first().value
        val end    = points.last().value
        val change = end - start
        val pct    = if (start != 0.0) (change / start) * 100.0 else 0.0
        return change to pct
    }

    fun onPeriodSelected(period: ChartPeriod) {
        if (period == _uiState.value.selectedPeriod) return
        _uiState.update { it.copy(isChartLoading = true, chartError = null, selectedPeriod = period) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { repository.getPortfolioChart(period) }
                    .fold(
                        onSuccess = { points ->
                            val (pChange, pChangePct) = periodChangeFrom(points)
                            _uiState.update {
                                it.copy(
                                    chartPoints         = points,
                                    isChartLoading      = false,
                                    periodChange        = pChange,
                                    periodChangePercent = pChangePct,
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(chartError = e.message, isChartLoading = false) }
                        }
                    )
            }
        }
    }
}
