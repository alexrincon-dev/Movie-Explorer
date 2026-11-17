package dev.alexrincon.movieclient.data.language

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.alexrincon.movieclient.core.dispatchers.IoDispatcher
import dev.alexrincon.movieclient.domain.language.MovieContentLanguageStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MovieContentLanguageModule {
    @Provides
    @Singleton
    fun provideMovieContentLanguageStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): MovieContentLanguageStore {
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(SupervisorJob() + ioDispatcher),
            produceFile = {
                File(context.filesDir, "movie_content_language.preferences_pb")
            },
        )
        return DataStoreMovieContentLanguageStore(dataStore)
    }
}
