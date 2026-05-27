package com.jeremyle.myfi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeremyle.myfi.domain.model.ChartPeriod
import com.jeremyle.myfi.domain.model.ChartPoint
import com.jeremyle.myfi.ui.theme.FontSize
import com.jeremyle.myfi.ui.theme.GainGreen
import com.jeremyle.myfi.ui.theme.LossOrange
import com.jeremyle.myfi.ui.theme.MyFiTheme
import com.jeremyle.myfi.ui.theme.Spacing
import com.jeremyle.myfi.ui.theme.TextSecondary
import com.jeremyle.myfi.ui.theme.selectedTextColor

/**
 * Full chart section: the line chart canvas + period selector row.
 * Placed in the LazyColumn between the portfolio header and the buying-power row.
 */
@Composable
fun PortfolioChartSection(
    points: List<ChartPoint>,
    isPositive: Boolean,
    selectedPeriod: ChartPeriod,
    isLoading: Boolean,
    onPeriodSelected: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            isLoading -> ChartPlaceholder()
            points.isEmpty() -> ChartPlaceholder() // market closed / no data
            else -> PortfolioLineChart(
                points = points,
                isPositive = isPositive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }

    // Dotted divider below chart
    DottedDivider()

    // Period selector
    PeriodSelector(
        selectedPeriod = selectedPeriod,
        onPeriodSelected = onPeriodSelected,
    )
}

// ─── Line chart ──────────────────────────────────────────────────────────────

@Composable
fun PortfolioLineChart(
    points: List<ChartPoint>,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    val lineColor = if (isPositive) GainGreen else LossOrange

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val values = points.map { it.value.toFloat() }
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val vertPad = range * 0.15f // 15% breathing room top + bottom
        val paddedMin = minVal - vertPad
        val paddedMax = maxVal + vertPad
        val paddedRange = paddedMax - paddedMin

        val n = values.size

        fun xOf(i: Int) = i.toFloat() / (n - 1).coerceAtLeast(1) * w
        fun yOf(v: Float) = h * (1f - (v - paddedMin) / paddedRange)

        // ── Gradient fill ────────────────────────────────────────────────────
        val fillPath = Path().apply {
            moveTo(xOf(0), yOf(values[0]))
            for (i in 1 until n) lineTo(xOf(i), yOf(values[i]))
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = h,
            ),
        )

        // ── Line ─────────────────────────────────────────────────────────────
        val linePath = Path().apply {
            moveTo(xOf(0), yOf(values[0]))
            for (i in 1 until n) lineTo(xOf(i), yOf(values[i]))
        }
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

// ─── Loading placeholder ─────────────────────────────────────────────────────

@Composable
private fun ChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = GainGreen,
            modifier = Modifier.size(32.dp),
            strokeWidth = 2.dp,
        )
    }
}

// ─── Dotted divider ───────────────────────────────────────────────────────────

@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
        )
    }
}

// ─── Period selector ─────────────────────────────────────────────────────────

@Composable
private fun PeriodSelector(
    selectedPeriod: ChartPeriod,
    onPeriodSelected: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChartPeriod.entries.forEach { period ->
            PeriodChip(
                label = period.label,
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Gear symbol — placeholder for future chart-settings bottom sheet
        Text(
            text = "⚙",
            color = TextSecondary,
            fontSize = FontSize.lg,
        )
    }
}

@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) GainGreen else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else selectedTextColor(true),
            fontSize = FontSize.sm,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ChartSectionDarkPreview() {
    MyFiTheme(darkTheme = true) {
        val fakePoints = buildList {
            var value = 80_000.0
            var ts = 1_700_000_000L
            repeat(78) {
                value += (-200..200).random()
                add(ChartPoint(ts, value))
                ts += 300L
            }
        }
        PortfolioChartSection(
            points = fakePoints,
            isPositive = true,
            selectedPeriod = ChartPeriod.ONE_DAY,
            isLoading = false,
            onPeriodSelected = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ChartSectionLightPreview() {
    MyFiTheme(darkTheme = false) {
        val fakePoints = buildList {
            var value = 80_000.0
            var ts = 1_700_000_000L
            repeat(78) {
                value += (-200..200).random()
                add(ChartPoint(ts, value))
                ts += 300L
            }
        }
        PortfolioChartSection(
            points = fakePoints,
            isPositive = true,
            selectedPeriod = ChartPeriod.ONE_DAY,
            isLoading = false,
            onPeriodSelected = {},
        )
    }
}
