@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.alexrincon.movieclient.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

internal fun runViewModelTest(testBody: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        testBody()
    } finally {
        Dispatchers.resetMain()
    }
}
