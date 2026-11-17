package com.aarogya.sanjeevani.core.common.appearance

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppearanceModule {
    @Binds
    @Singleton
    abstract fun bindAppearanceRepository(impl: DataStoreAppearanceRepository): AppearanceRepository
}
