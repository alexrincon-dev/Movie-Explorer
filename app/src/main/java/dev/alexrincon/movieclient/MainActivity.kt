package dev.alexrincon.movieclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.alexrincon.movieclient.presentation.navigation.MovieNavHost
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieClientTheme {
                MovieNavHost()
            }
        }
    }
}