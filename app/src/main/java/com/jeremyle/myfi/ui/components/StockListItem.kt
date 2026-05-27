package com.jeremyle.myfi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeremyle.myfi.R
import com.jeremyle.myfi.domain.model.Stock
import com.jeremyle.myfi.ui.theme.FontSize
import com.jeremyle.myfi.ui.theme.Spacing
import com.jeremyle.myfi.ui.theme.selectedTextColor
import com.jeremyle.myfi.ui.theme.GainGreen
import com.jeremyle.myfi.ui.theme.LossOrange
import com.jeremyle.myfi.ui.theme.TextSecondary
import com.jeremyle.myfi.ui.theme.MyFiTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StockListItem(
    stock: Stock,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stock.ticker,
                color = selectedTextColor(true),
                fontWeight = FontWeight.Bold,
                fontSize = FontSize.lg
            )
            Text(
                text = stringResource(R.string.shares_format, stock.shares),
                color = TextSecondary,
                fontSize = FontSize.xs
            )
        }
        Box(
            modifier = Modifier
                .background(
                    color = if (stock.isPositive) GainGreen else LossOrange,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp, vertical = Spacing.xs)
        ) {
            Text(
                text = currencyFormat.format(stock.currentPrice),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = FontSize.md
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StockListItemGainPreview() {
    MyFiTheme {
        StockListItem(stock = Stock("DIS", 50, 103.19, 1.45, 1.43))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StockListItemLossPreview() {
    MyFiTheme {
        StockListItem(stock = Stock("PYPL", 50, 44.17, -1.23, -2.71))
    }
}
