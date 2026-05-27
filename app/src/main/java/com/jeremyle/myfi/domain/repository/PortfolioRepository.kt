package com.jeremyle.myfi.domain.repository

import com.jeremyle.myfi.domain.model.ChartPeriod
import com.jeremyle.myfi.domain.model.ChartPoint
import com.jeremyle.myfi.domain.model.Portfolio

interface PortfolioRepository {
    suspend fun getPortfolio(): Portfolio
    suspend fun getPortfolioChart(period: ChartPeriod): List<ChartPoint>
}
