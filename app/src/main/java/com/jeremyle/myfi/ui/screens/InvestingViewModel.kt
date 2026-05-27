package com.jeremyle.myfi.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyle.myfi.data.repository.PortfolioRepositoryImpl
import com.jeremyle.myfi.domain.model.Stock
import kotlinx.coroutines.Dispatchers
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
    val error: String? = null
)

class InvestingViewModel(application: Application) : AndroidViewModel(application) {

    // TODO: Replace with Hilt-injected PortfolioRepository once DI is wired up
    private val repository = PortfolioRepositoryImpl(application)

    private val _uiState = MutableStateFlow(InvestingUiState())
    val uiState: StateFlow<InvestingUiState> = _uiState.asStateFlow()

    init {
        loadPortfolio()
    }

    private fun loadPortfolio() {
        viewModelScope.launch {
            try {
                val portfolio = withContext(Dispatchers.IO) { repository.getPortfolio() }
                _uiState.update {
                    it.copy(
                        portfolioValue = portfolio.value,
                        todayChange = portfolio.todayChange,
                        todayChangePercent = portfolio.todayChangePercent,
                        buyingPower = portfolio.buyingPower,
                        stocks = portfolio.stocks,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
