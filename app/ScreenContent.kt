package com.example.actimate.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ScreenContent(
    selectedPage: String,
    accelerometerViewModel: AccelerometerViewModel = viewModel(),
    //compassViewModel: CompassViewModel = viewModel()
) {
    when (selectedPage) {
        "Today" -> TodayScreen()
        "Data" -> DataScreen(accelerometerViewModel= accelerometerViewModel)
        "Settings" -> SettingsScreen()
        else -> NotFoundScreen()
    }
}
