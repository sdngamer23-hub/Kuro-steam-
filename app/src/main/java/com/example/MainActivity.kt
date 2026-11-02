package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.KuroStreamDatabase
import com.example.data.remote.NetworkModule
import com.example.data.repository.StreamingRepository
import com.example.ui.KuroStreamApp
import com.example.ui.StreamingViewModel
import com.example.ui.theme.KuroStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color(0xFF121622).toArgb())
        )

        val database = KuroStreamDatabase.getInstance(applicationContext)
        val repository = StreamingRepository(
            watchlistDao = database.watchlistDao(),
            dao = database.streamingDao(),
            jikanApi = NetworkModule.jikanApi,
            tvMazeApi = NetworkModule.tvMazeApi
        )

        setContent {
            KuroStreamTheme(darkTheme = true) {
                val streamingViewModel: StreamingViewModel = viewModel(
                    factory = StreamingViewModel.Factory(repository)
                )
                KuroStreamApp(viewModel = streamingViewModel)
            }
        }
    }
}
