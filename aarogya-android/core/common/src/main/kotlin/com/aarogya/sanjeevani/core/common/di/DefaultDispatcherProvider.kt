package com.aarogya.sanjeevani.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/** The real [DispatcherProvider] bound by [DispatchersModule] -- the
 *  actual platform dispatchers, nothing swapped out. */
class DefaultDispatcherProvider
    @Inject
    constructor() : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val io: CoroutineDispatcher = Dispatchers.IO
    }
