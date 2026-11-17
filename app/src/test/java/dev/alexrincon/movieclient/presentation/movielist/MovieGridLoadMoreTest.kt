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

    @Test
    fun nearEndIgnoresLoadingButNeedsMovies() {
        assertTrue(movieGridIsNearEnd(lastVisibleIndex = 6, totalItems = 10, movieCount = 10))
        assertFalse(movieGridIsNearEnd(lastVisibleIndex = 5, totalItems = 10, movieCount = 10))
        assertFalse(movieGridIsNearEnd(lastVisibleIndex = null, totalItems = 10, movieCount = 10))
        assertFalse(movieGridIsNearEnd(lastVisibleIndex = 0, totalItems = 0, movieCount = 0))
    }

    @Test
    fun reachingThePrefetchWindowRequestsAPage() {
        val trigger = MovieGridLoadMoreTrigger()

        assertFalse(trigger.shouldRequest(awayFromEnd))
        assertTrue(trigger.shouldRequest(readyAtEnd))
    }

    @Test
    fun noRequestWhileAPageOrRefreshIsLoading() {
        val trigger = MovieGridLoadMoreTrigger()

        assertFalse(trigger.shouldRequest(loadingAtEnd))
    }

    @Test
    fun successfulPageAllowsTheNextRequestWhileStillAtTheEnd() {
        val trigger = MovieGridLoadMoreTrigger()

        assertTrue(trigger.shouldRequest(readyAtEnd))
        assertFalse(trigger.shouldRequest(loadingAtEnd))
        assertTrue(trigger.shouldRequest(readyAtEnd))
    }

    @Test
    fun refreshEndingAtTheEndRequestsAPage() {
        val trigger = MovieGridLoadMoreTrigger()

        assertFalse(trigger.shouldRequest(loadingAtEnd))
        assertTrue(trigger.shouldRequest(readyAtEnd))
    }

    @Test
    fun failedPageIsNotRequestedAgainWhileTheGridStaysAtTheEnd() {
        val trigger = MovieGridLoadMoreTrigger()

        assertTrue(trigger.shouldRequest(readyAtEnd))
        assertFalse(trigger.shouldRequest(loadingAtEnd))
        assertFalse(trigger.shouldRequest(loadingAtEnd.copy(paginationFailed = true)))
        assertFalse(trigger.shouldRequest(failedAtEnd))
    }

    @Test
    fun failedPageCanBeRequestedOnceAfterScrollingAwayAndBack() {
        val trigger = MovieGridLoadMoreTrigger()
        assertFalse(trigger.shouldRequest(failedAtEnd))

        assertFalse(trigger.shouldRequest(failedAtEnd.copy(isNearEnd = false, shouldLoadMore = false)))
        assertTrue(trigger.shouldRequest(failedAtEnd))

        assertFalse(trigger.shouldRequest(loadingAtEnd))
        assertFalse(trigger.shouldRequest(failedAtEnd))
    }

    @Test
    fun retriedPageThatSucceedsRestoresAutomaticPagination() {
        val trigger = MovieGridLoadMoreTrigger()
        assertFalse(trigger.shouldRequest(failedAtEnd))

        assertFalse(trigger.shouldRequest(loadingAtEnd))
        assertTrue(trigger.shouldRequest(readyAtEnd))
        assertFalse(trigger.shouldRequest(loadingAtEnd))
        assertTrue(trigger.shouldRequest(readyAtEnd))
    }

    @Test
    fun repeatedFailedSignalsAtTheEndAuthorizeOnlyOneRetry() {
        val trigger = MovieGridLoadMoreTrigger()
        assertFalse(trigger.shouldRequest(failedAtEnd))
        assertFalse(trigger.shouldRequest(failedAwayFromEnd))

        assertTrue(trigger.shouldRequest(failedAtEnd))
        assertFalse(trigger.shouldRequest(failedAtEnd))
        assertFalse(trigger.shouldRequest(failedAtEnd))
    }

    @Test
    fun eachScrollAwayAndBackAuthorizesAnotherRetry() {
        val trigger = MovieGridLoadMoreTrigger()
        assertFalse(trigger.shouldRequest(failedAtEnd))

        assertFalse(trigger.shouldRequest(failedAwayFromEnd))
        assertTrue(trigger.shouldRequest(failedAtEnd))
        assertFalse(trigger.shouldRequest(failedAtEnd))

        assertFalse(trigger.shouldRequest(failedAwayFromEnd))
        assertTrue(trigger.shouldRequest(failedAtEnd))
        assertFalse(trigger.shouldRequest(failedAtEnd))
    }

    @Test
    fun refreshClearsTheFailureAndRestoresAutomaticPagination() {
        val trigger = MovieGridLoadMoreTrigger()
        assertFalse(trigger.shouldRequest(failedAtEnd))

        assertFalse(trigger.shouldRequest(refreshingAtEnd))
        assertTrue(trigger.shouldRequest(readyAtEnd))
        assertFalse(trigger.shouldRequest(loadingAtEnd))
        assertTrue(trigger.shouldRequest(readyAtEnd))
    }

    private val readyAtEnd = MovieGridLoadMoreSignal(
        isNearEnd = true,
        shouldLoadMore = true,
        paginationFailed = false,
    )
    private val loadingAtEnd = readyAtEnd.copy(shouldLoadMore = false)
    private val awayFromEnd = readyAtEnd.copy(isNearEnd = false, shouldLoadMore = false)
    private val failedAtEnd = readyAtEnd.copy(paginationFailed = true)
    private val failedAwayFromEnd = awayFromEnd.copy(paginationFailed = true)

    /** The ViewModel clears the pagination failure when a refresh starts. */
    private val refreshingAtEnd = loadingAtEnd

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
