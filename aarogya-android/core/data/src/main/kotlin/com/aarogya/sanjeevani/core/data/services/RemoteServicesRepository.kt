package com.aarogya.sanjeevani.core.data.services

import javax.inject.Qualifier

/** Distinguishes the real, network-backed [ServicesRepositoryImpl] from
 *  the unqualified [ServicesRepository] binding ([CachingServicesRepository]),
 *  so the cache can depend on "the thing underneath it" without
 *  injecting itself. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteServicesRepository
