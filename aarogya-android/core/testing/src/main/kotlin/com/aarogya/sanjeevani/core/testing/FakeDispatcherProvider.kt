package com.aarogya.sanjeevani.core.testing

import com.aarogya.sanjeevani.core.common.di.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** The one [DispatcherProvider] every repository/use-case test should
 *  inject -- all three dispatchers point at the same [TestDispatcher]
 *  so coroutines launched on any of them run synchronously/deterministically
 *  under `runTest`, with no Hilt involved. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeDispatcherProvider(
    dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : DispatcherProvider {
    override val main = dispatcher
    override val default = dispatcher
    override val io = dispatcher
}
