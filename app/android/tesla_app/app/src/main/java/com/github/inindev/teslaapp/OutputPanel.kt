package com.github.inindev.teslaapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OutputPanel(viewModel: MainViewModel) {
    val jsonContent = viewModel.jsonContent.collectAsState(initial = "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (jsonContent.value.isNotBlank()) {
                val horizontalScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 40.dp, top = 24.dp, end = 40.dp)
                        .background(Color(0xFFFFFFF0))
                        .border(1.dp, Color.Black, shape = RectangleShape)
                ) {
                    Text(
                        text = jsonContent.value,
                        modifier = Modifier
                            .horizontalScroll(horizontalScrollState)
                            .padding(8.dp),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}
