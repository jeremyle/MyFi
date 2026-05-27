package com.jeremyle.myfi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeremyle.myfi.R
import com.jeremyle.myfi.domain.model.ChartPeriod
import com.jeremyle.myfi.domain.model.ChartPoint
import com.jeremyle.myfi.domain.model.Stock
import com.jeremyle.myfi.ui.components.PortfolioChartSection
import com.jeremyle.myfi.ui.components.StockListItem
import com.jeremyle.myfi.ui.theme.FontSize
import com.jeremyle.myfi.ui.theme.GainGreen
import com.jeremyle.myfi.ui.theme.LossOrange
import com.jeremyle.myfi.ui.theme.MyFiTheme
import com.jeremyle.myfi.ui.theme.Spacing
import com.jeremyle.myfi.ui.theme.TextSecondary
import com.jeremyle.myfi.ui.theme.dividerColor
import com.jeremyle.myfi.ui.theme.selectedTextColor
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun InvestingScreen(viewModel: InvestingViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    InvestingContent(
        uiState          = uiState,
        onPeriodSelected = viewModel::onPeriodSelected
    )
}

@Composable
private fun InvestingContent(
    uiState: InvestingUiState,
    onPeriodSelected: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier      = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg)
    ) {
        // Portfolio header (value + today's change)
        item { PortfolioHeader(uiState) }

        // Chart + period selector
        item {
            val chartIsPositive = uiState.chartPoints.let { pts ->
                if (pts.size >= 2) pts.last().value >= pts.first().value
                else uiState.todayChange >= 0   // fallback while chart is loading
            }
            PortfolioChartSection(
                points           = uiState.chartPoints,
                isPositive       = chartIsPositive,
                selectedPeriod   = uiState.selectedPeriod,
                isLoading        = uiState.isChartLoading,
                onPeriodSelected = onPeriodSelected
            )
        }

        // Divider → buying power
        item {
            HorizontalDivider(
                color    = dividerColor(),
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
            BuyingPowerRow(uiState.buyingPower)
        }

        // Stocks & ETFs list
        item { StocksSectionHeader() }
        items(uiState.stocks) { stock ->
            StockListItem(stock = stock)
            HorizontalDivider(
                color    = dividerColor(),
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }
    }
}

// ─── Portfolio header ─────────────────────────────────────────────────────────

@Composable
private fun PortfolioHeader(uiState: InvestingUiState) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    // While chart is loading fall back to today's change from the quote endpoint;
    // once chart data is available use the period's start→end delta.
    val hasPeriodData = !uiState.isChartLoading && uiState.chartPoints.size >= 2
    val displayChange    = if (hasPeriodData) uiState.periodChange    else uiState.todayChange
    val displayChangePct = if (hasPeriodData) uiState.periodChangePercent else uiState.todayChangePercent
    val periodLabel = if (!hasPeriodData || uiState.selectedPeriod == ChartPeriod.ONE_DAY) {
        stringResource(R.string.today_label)
    } else {
        uiState.selectedPeriod.label
    }

    val isGain      = displayChange >= 0
    val changeSign  = if (isGain) "▲" else "▼"
    val changeColor = if (isGain) GainGreen else LossOrange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.lg)
    ) {
        Text(
            text       = currencyFormat.format(uiState.portfolioValue),
            color      = selectedTextColor(true),
            fontSize   = FontSize.xxxl,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text     = "$changeSign ${currencyFormat.format(abs(displayChange))} " +
                "(${String.format("%.2f", abs(displayChangePct))}%) " +
                periodLabel,
            color    = changeColor,
            fontSize = FontSize.sm
        )
    }
}

// ─── Buying power row ─────────────────────────────────────────────────────────

@Composable
private fun BuyingPowerRow(buyingPower: Double) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text     = stringResource(R.string.buying_power),
            color    = selectedTextColor(true),
            fontSize = FontSize.md
        )
        Text(
            text     = currencyFormat.format(buyingPower),
            color    = selectedTextColor(true),
            fontSize = FontSize.md
        )
    }
}

// ─── Stocks & ETFs section header ─────────────────────────────────────────────

@Composable
private fun StocksSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.md, end = Spacing.md, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = stringResource(R.string.stocks_and_etfs),
            color      = selectedTextColor(true),
            fontSize   = FontSize.xxl,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text     = "›",
            color    = TextSecondary,
            fontSize = FontSize.xxl
        )
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InvestingContentPreview() {
    MyFiTheme {
        val fakePoints = buildList {
            var value = 80_500.0
            var ts    = 1_700_000_000L
            repeat(78) {
                value += (-150..150).random()
                add(ChartPoint(ts, value))
                ts += 300L
            }
        }
        InvestingContent(
            uiState = InvestingUiState(
                portfolioValue     = 81_010.51,
                todayChange        = 739.07,
                todayChangePercent = 0.92,
                buyingPower        = 840.25,
                chartPoints        = fakePoints,
                isChartLoading     = false,
                stocks             = listOf(
                    Stock("PYPL", 50,  44.17,   -1.23, -2.71),
                    Stock("DIS",  50,  103.19,   1.45,  1.43),
                    Stock("AAPL", 160, 308.77,  -3.21, -1.03),
                    Stock("F",    200,  15.33,   0.18,  1.19)
                ),
                isLoading = false
            ),
            onPeriodSelected = {}
        )
    }
}
