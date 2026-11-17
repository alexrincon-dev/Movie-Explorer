package dev.alexrincon.movieclient.presentation.language

import dev.alexrincon.movieclient.domain.language.MovieContentLanguageStore
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Selected language for movie text and in-app UI strings.
 * The value is null until the persisted language has been read.
 * A change is stored only when [updateContentLanguage] is called.
 */
@Singleton
class MovieContentLanguageState @Inject constructor(
    private val store: MovieContentLanguageStore,
) {
    private val currentLanguage = MutableStateFlow<MovieContentLanguage?>(null)
    private val loadMutex = Mutex()

    val contentLanguage: StateFlow<MovieContentLanguage?> = currentLanguage.asStateFlow()

    /**
     * Returns the persisted language, reading it once.
     * A later [updateContentLanguage] is not overwritten by that read.
     */
    suspend fun awaitLanguage(): MovieContentLanguage {
        currentLanguage.value?.let { return it }
        return loadMutex.withLock {
            currentLanguage.value?.let { return@withLock it }
            val loaded = try {
                store.read()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                MovieContentLanguage.EN_US
            }
            currentLanguage.value = loaded
            loaded
        }
    }

    suspend fun updateContentLanguage(language: MovieContentLanguage) {
        loadMutex.withLock {
            store.write(language)
            currentLanguage.value = language
        }
    }
}
