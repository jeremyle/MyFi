package com.jeremyle.myfi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.jeremyle.myfi.R
import com.jeremyle.myfi.ui.theme.FontSize
import com.jeremyle.myfi.ui.theme.MyFiTheme
import com.jeremyle.myfi.ui.theme.TabUnselected

@Composable
fun RetirementScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.tab_retirement),
            color = TabUnselected,
            fontSize = FontSize.lg
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RetirementScreenPreview() {
    MyFiTheme { RetirementScreen() }
}
