package com.aarogya.sanjeevani.patient.appearance

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.appearance.AppearanceMode
import com.aarogya.sanjeevani.core.designsystem.theme.ThemeMode
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAppearanceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppearanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var appearanceRepository: FakeAppearanceRepository
    private lateinit var viewModel: AppearanceViewModel

    @Before
    fun setUp() {
        appearanceRepository = FakeAppearanceRepository()
        viewModel = AppearanceViewModel(appearanceRepository)
    }

    @Test
    fun `starts at System when the repository has no stored preference yet`() =
        runTest {
            assertEquals(ThemeMode.System, viewModel.themeMode.value)
        }

    @Test
    fun `maps every AppearanceMode to its ThemeMode counterpart`() =
        runTest {
            viewModel.themeMode.test {
                assertEquals(ThemeMode.System, awaitItem())

                appearanceRepository.mode.value = AppearanceMode.DARK
                assertEquals(ThemeMode.Dark, awaitItem())

                appearanceRepository.mode.value = AppearanceMode.LIGHT
                assertEquals(ThemeMode.Light, awaitItem())

                appearanceRepository.mode.value = AppearanceMode.SYSTEM
                assertEquals(ThemeMode.System, awaitItem())
            }
        }
}
