package dev.alexrincon.movieclient.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.alexrincon.movieclient.data.local.entity.MovieListEntity
import dev.alexrincon.movieclient.domain.model.MovieListType

@Dao
abstract class MovieListDao {
    @Query("DELETE FROM movie_lists WHERE listType = :listType")
    abstract suspend fun deleteByListType(listType: MovieListType)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun append(entries: List<MovieListEntity>)

    @Transaction
    open suspend fun replaceForListType(
        listType: MovieListType,
        entries: List<MovieListEntity>,
    ) {
        require(entries.all { it.listType == listType }) {
            "Membership entries must use $listType."
        }
        deleteByListType(listType)
        if (entries.isNotEmpty()) {
            append(entries)
        }
    }
}
