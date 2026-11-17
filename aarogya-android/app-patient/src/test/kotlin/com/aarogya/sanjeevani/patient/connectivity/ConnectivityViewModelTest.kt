package com.aarogya.sanjeevani.patient.connectivity

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeConnectivityObserver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConnectivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var connectivityObserver: FakeConnectivityObserver
    private lateinit var viewModel: ConnectivityViewModel

    @Before
    fun setUp() {
        connectivityObserver = FakeConnectivityObserver()
        viewModel = ConnectivityViewModel(connectivityObserver)
    }

    @Test
    fun `starts online, so isOffline is false`() =
        runTest {
            assertFalse(viewModel.isOffline.value)
        }

    @Test
    fun `isOffline inverts the observer -- disconnected shows the banner, reconnecting hides it`() =
        runTest {
            viewModel.isOffline.test {
                assertFalse(awaitItem())

                connectivityObserver.setConnected(false)
                assertTrue(awaitItem())

                connectivityObserver.setConnected(true)
                assertFalse(awaitItem())
            }
        }
}
