package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.appearance.AppearanceMode
import com.aarogya.sanjeevani.core.common.appearance.AppearanceRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [AppearanceRepository]. */
class FakeAppearanceRepository : AppearanceRepository {
    val mode = MutableStateFlow(AppearanceMode.SYSTEM)

    override fun observeAppearanceMode() = mode

    override suspend fun setAppearanceMode(mode: AppearanceMode) {
        this.mode.value = mode
    }
}
