package com.jeremyle.myfi.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeremyle.myfi.R
import com.jeremyle.myfi.domain.model.Stock
import com.jeremyle.myfi.ui.components.StockListItem
import com.jeremyle.myfi.ui.theme.FontSize
import com.jeremyle.myfi.ui.theme.Spacing
import com.jeremyle.myfi.ui.theme.dividerColor
import com.jeremyle.myfi.ui.theme.selectedTextColor
import com.jeremyle.myfi.ui.theme.GainGreen
import com.jeremyle.myfi.ui.theme.LossOrange
import com.jeremyle.myfi.ui.theme.TextSecondary
import com.jeremyle.myfi.ui.theme.MyFiTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun InvestingScreen(viewModel: InvestingViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    InvestingContent(uiState = uiState)
}

@Composable
private fun InvestingContent(
    uiState: InvestingUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg)
    ) {
        item { PortfolioHeader(uiState) }
        item {
            HorizontalDivider(
                color = dividerColor(),
                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md)
            )
            BuyingPowerRow(uiState.buyingPower)
        }
        item { StocksSectionHeader() }
        items(uiState.stocks) { stock ->
            StockListItem(stock = stock)
            HorizontalDivider(
                color = dividerColor(),
                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md)
            )
        }
    }
}

@Composable
private fun PortfolioHeader(uiState: InvestingUiState) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val isGain = uiState.todayChange >= 0
    val changeSign = if (isGain) "▲" else "▼"
    val changeColor = if (isGain) GainGreen else LossOrange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.lg)
    ) {
        Text(
            text = currencyFormat.format(uiState.portfolioValue),
            color = selectedTextColor(true),
            fontSize = FontSize.xxxl,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$changeSign ${currencyFormat.format(abs(uiState.todayChange))} " +
                "(${String.format("%.2f", abs(uiState.todayChangePercent))}%) " +
                stringResource(R.string.today_label),
            color = changeColor,
            fontSize = FontSize.sm
        )
    }
}

@Composable
private fun BuyingPowerRow(buyingPower: Double) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.buying_power),
            color = selectedTextColor(true),
            fontSize = FontSize.md
        )
        Text(
            text = currencyFormat.format(buyingPower),
            color = selectedTextColor(true),
            fontSize = FontSize.md
        )
    }
}

@Composable
private fun StocksSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.md, end = Spacing.md, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stocks_and_etfs),
            color = selectedTextColor(true),
            fontSize = FontSize.xxl,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "›",
            color = TextSecondary,
            fontSize = FontSize.xxl
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InvestingContentPreview() {
    MyFiTheme {
        InvestingContent(
            uiState = InvestingUiState(
                portfolioValue = 81010.51,
                todayChange = 739.07,
                todayChangePercent = 0.92,
                buyingPower = 840.25,
                stocks = listOf(
                    Stock("PYPL", 50, 44.17, -1.23, -2.71),
                    Stock("DIS", 50, 103.19, 1.45, 1.43),
                    Stock("AAPL", 160, 308.77, -3.21, -1.03),
                    Stock("F", 200, 15.33, 0.18, 1.19)
                ),
                isLoading = false
            )
        )
    }
}
