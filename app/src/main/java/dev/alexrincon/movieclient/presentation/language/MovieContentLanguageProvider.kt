package dev.alexrincon.movieclient.presentation.language

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import java.util.Locale

/**
 * Shows [content] in the persisted content language.
 * Nothing is drawn until that language has been read, so a restart does not flash the other language.
 */
@Composable
fun MovieContentLanguageHost(
    contentLanguageState: MovieContentLanguageState,
    content: @Composable () -> Unit,
) {
    val language by contentLanguageState.contentLanguage.collectAsStateWithLifecycle()
    LaunchedEffect(contentLanguageState) {
        contentLanguageState.awaitLanguage()
    }
    val selected = language
    if (selected == null) {
        Box(Modifier.fillMaxSize())
    } else {
        ProvideMovieContentLanguage(language = selected, content = content)
    }
}

/**
 * Resolves UI strings from [language] without changing the Android system locale.
 *
 * `stringResource` reads [LocalResources]. That value is computed from [LocalConfiguration]
 * and `LocalContext.resources`, and the activity already provides both for the system locale.
 * Supplying a new configuration, context, and resources makes the existing string resources
 * recompose when the content language changes. The wrapper keeps the Activity as its base
 * so Hilt can still find it.
 */
@Composable
fun ProvideMovieContentLanguage(
    language: MovieContentLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val locale = language.toAppLocale()
    val localizedConfiguration = remember(baseConfiguration, locale) {
        Configuration(baseConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val localizedResources = remember(baseContext, localizedConfiguration) {
        baseContext.createConfigurationContext(localizedConfiguration).resources
    }
    val localizedContext = remember(baseContext, localizedResources) {
        LocalizedContext(baseContext, localizedResources)
    }
    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
        LocalResources provides localizedResources,
        content = content,
    )
}

internal fun MovieContentLanguage.toAppLocale(): Locale {
    val languageTag = when (this) {
        MovieContentLanguage.EN_US -> "en-US"
        MovieContentLanguage.ES_MX -> "es-MX"
    }
    return Locale.forLanguageTag(languageTag)
}

private class LocalizedContext(
    base: Context,
    private val localizedResources: Resources,
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}
