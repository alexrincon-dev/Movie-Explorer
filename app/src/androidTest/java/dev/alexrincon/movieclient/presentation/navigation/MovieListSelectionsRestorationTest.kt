package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.alexrincon.movieclient.domain.model.MovieListType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieListSelectionsRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eachListSelectionSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var selections: MovieListSelections
        restorationTester.setContent {
            selections = rememberMovieListSelections()
        }
        composeRule.runOnIdle {
            selections.select(MovieListType.POPULAR, 8)
            selections.select(MovieListType.TOP_RATED, 21)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle {
            assertEquals(8, selections.selectedId(MovieListType.POPULAR))
            assertEquals(21, selections.selectedId(MovieListType.TOP_RATED))
        }
    }

    @Test
    fun aMissingSelectionStaysMissingAfterRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var selections: MovieListSelections
        restorationTester.setContent {
            selections = rememberMovieListSelections()
        }
        composeRule.runOnIdle {
            selections.select(MovieListType.POPULAR, 8)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle {
            assertEquals(8, selections.selectedId(MovieListType.POPULAR))
            assertNull(selections.selectedId(MovieListType.TOP_RATED))
        }
    }
}
