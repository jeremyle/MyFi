package com.jeremyle.myfi.domain.repository

import com.jeremyle.myfi.domain.model.Portfolio

interface PortfolioRepository {
    suspend fun getPortfolio(): Portfolio
}
