package dev.alexrincon.movieclient.presentation.movielist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieGridLoadMoreTest {
    @Test
    fun loadsWhenTheLastVisibleItemIsInsideThePrefetchWindow() {
        assertTrue(
            movieGridShouldLoadMore(
                lastVisibleIndex = 6,
                totalItems = 10,
                movieCount = 10,
                isLoadingMore = false,
                isRefreshing = false,
            ),
        )
        assertFalse(
            movieGridShouldLoadMore(
                lastVisibleIndex = 5,
                totalItems = 10,
                movieCount = 10,
                isLoadingMore = false,
                isRefreshing = false,
            ),
        )
    }

    @Test
    fun doesNotLoadWhileRefreshingLoadingOrBeforeTheGridHasItems() {
        assertFalse(signal(isRefreshing = true))
        assertFalse(signal(isLoadingMore = true))
        assertFalse(signal(movieCount = 0, totalItems = 0, lastVisibleIndex = null))
        assertFalse(signal(lastVisibleIndex = null))
    }

    private fun signal(
        lastVisibleIndex: Int? = 9,
        totalItems: Int = 10,
        movieCount: Int = 10,
        isLoadingMore: Boolean = false,
        isRefreshing: Boolean = false,
    ): Boolean {
        return movieGridShouldLoadMore(
            lastVisibleIndex = lastVisibleIndex,
            totalItems = totalItems,
            movieCount = movieCount,
            isLoadingMore = isLoadingMore,
            isRefreshing = isRefreshing,
        )
    }
}
