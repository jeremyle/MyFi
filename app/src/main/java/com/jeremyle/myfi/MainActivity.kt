package com.jeremyle.myfi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jeremyle.myfi.ui.theme.selectedTextColor
import androidx.compose.ui.text.font.FontWeight
import com.jeremyle.myfi.ui.screens.InvestingScreen
import com.jeremyle.myfi.ui.screens.RetirementScreen
import com.jeremyle.myfi.ui.theme.FontSize
import com.jeremyle.myfi.ui.theme.MyFiTheme
import com.jeremyle.myfi.ui.theme.Spacing
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFiTheme {
                MyFiApp()
            }
        }
    }
}

@Composable
fun MyFiApp() {
    val tabs = listOf(R.string.tab_investing, R.string.tab_retirement)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.sm, top = Spacing.lg, bottom = Spacing.sm)
        ) {
            tabs.forEachIndexed { index, labelRes ->
                val selected = pagerState.currentPage == index
                Text(
                    text = stringResource(labelRes),
                    color = selectedTextColor(selected),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = FontSize.xl,
                    modifier = Modifier
                        .padding(end = Spacing.md)
                        .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> InvestingScreen()
                1 -> RetirementScreen()
            }
        }
    }
}
