package com.aarogya.sanjeevani.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import javax.inject.Singleton

/** The one place anything that needs "now" (a time-of-day greeting, a
 *  cache TTL) should get a [Clock] from -- injecting this instead of
 *  calling [Clock.System] directly is what lets a test substitute a
 *  fixed instant rather than depending on whatever time it happens to
 *  run at. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
