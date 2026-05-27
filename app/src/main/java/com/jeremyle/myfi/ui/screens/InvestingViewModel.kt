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
    val chartError: String? = null
)

class InvestingViewModel(application: Application) : AndroidViewModel(application) {

    // TODO: Replace with Hilt-injected PortfolioRepository once DI is wired up
    private val repository = PortfolioRepositoryImpl(
        context = application,
        api     = NetworkModule.provideFinnhubApi()
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
                        _uiState.update {
                            it.copy(
                                chartPoints    = points,
                                isChartLoading = false,
                                selectedPeriod = period
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

    fun onPeriodSelected(period: ChartPeriod) {
        if (period == _uiState.value.selectedPeriod) return
        _uiState.update { it.copy(isChartLoading = true, chartError = null, selectedPeriod = period) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { repository.getPortfolioChart(period) }
                    .fold(
                        onSuccess = { points ->
                            _uiState.update { it.copy(chartPoints = points, isChartLoading = false) }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(chartError = e.message, isChartLoading = false) }
                        }
                    )
            }
        }
    }
}
