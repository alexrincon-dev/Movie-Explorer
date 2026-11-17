package dev.alexrincon.movieclient

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.alexrincon.movieclient.presentation.language.MovieContentLanguageHost
import dev.alexrincon.movieclient.presentation.language.MovieContentLanguageState
import dev.alexrincon.movieclient.presentation.navigation.MovieNavHost
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var contentLanguageState: MovieContentLanguageState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The top app bars are dark in both themes, so status bar icons must always be light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            MovieClientTheme {
                MovieContentLanguageHost(contentLanguageState) {
                    MovieNavHost()
                }
            }
        }
    }
}