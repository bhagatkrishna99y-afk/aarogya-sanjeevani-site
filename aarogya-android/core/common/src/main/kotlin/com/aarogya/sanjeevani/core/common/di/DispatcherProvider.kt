package com.aarogya.sanjeevani.core.common.di

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The one source of `CoroutineDispatcher`s every repository/use-case
 * should inject instead of reaching for `Dispatchers.IO`/`Default`/`Main`
 * directly -- a plain constructor param (not a qualifier annotation),
 * so a unit test can hand a repository a fake implementation of this
 * interface with no Hilt involved at all (see
 * [com.aarogya.sanjeevani.core.testing.FakeDispatcherProvider]).
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}
