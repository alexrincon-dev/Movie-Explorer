package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val SampleMovieId = 1

@Composable
fun MovieListPlaceholder(
    onMovieClick: (movieId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Movie list",
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(onClick = { onMovieClick(SampleMovieId) }) {
            Text(text = "Open movie $SampleMovieId")
        }
    }
}
