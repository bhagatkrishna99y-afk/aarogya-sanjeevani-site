package com.aarogya.sanjeevani.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Swaps `Dispatchers.Main` for a [TestDispatcher] around every test so
 *  `viewModelScope`-launched coroutines run synchronously -- every
 *  ViewModel under test must inject a
 *  [com.aarogya.sanjeevani.core.common.di.DispatcherProvider] (see
 *  :core:common) rather than hardcoding `Dispatchers.IO`/`Main`, or this
 *  rule can't reach them. For a repository-level test with no
 *  `viewModelScope` involved, inject [FakeDispatcherProvider] directly
 *  instead -- this rule is specifically for the `Dispatchers.Main`
 *  swap.
 *
 *  [testDispatcher] is exposed (not just swapped in) so a test whose
 *  ViewModel does something genuinely time-based inside `viewModelScope`
 *  (a `debounce`, e.g.) can run its own `runTest(mainDispatcherRule.testDispatcher)`
 *  and drive that exact scheduler with `advanceTimeBy`/`advanceUntilIdle`
 *  -- `runTest`'s own default scheduler is a *different* instance than
 *  this rule's, so virtual time advanced there would never reach a
 *  `delay` running on `Dispatchers.Main`. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
