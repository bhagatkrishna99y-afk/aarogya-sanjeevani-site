package com.aarogya.sanjeevani.core.data.services

import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.common.result.onSuccess
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

private val CATEGORIES_CACHE_TTL = 5.minutes

/**
 * The unqualified [ServicesRepository] binding -- wraps [remote] with an
 * in-memory, process-lifetime cache for [getServiceCategories] only.
 * Categories are the one piece of catalog data worth caching here: they
 * almost never change, several screens (Home's category row, the
 * Services tab) read them on every visit, and -- unlike a request or a
 * profile -- there's nothing sensitive or per-user about them, so
 * holding a copy in memory (never written to disk) between app
 * launches costs nothing and saves a network round trip. [getServices]
 * is deliberately **not** cached here: it's read far less often (one
 * category at a time, not every screen load) and -- unlike categories --
 * its result genuinely depends on which category was asked for, so
 * caching it well would need a small cache *per [categoryId]*, which
 * isn't worth the complexity until a screen actually needs it.
 *
 * No lock/mutex around the cache fields: a race between two concurrent
 * callers both missing the cache and both re-fetching is harmless (both
 * get a correct, if slightly redundant, answer) -- not worth the extra
 * complexity for what's purely a convenience, not a correctness-critical
 * cache.
 */
class CachingServicesRepository
    @Inject
    constructor(
        @RemoteServicesRepository private val remote: ServicesRepository,
        private val clock: Clock,
    ) : ServicesRepository {
        @Volatile private var cachedCategories: List<ServiceCategory>? = null

        @Volatile private var cachedAt: Instant? = null

        override suspend fun getServiceCategories(): AppResult<List<ServiceCategory>> {
            val categories = cachedCategories
            val fetchedAt = cachedAt
            if (categories != null && fetchedAt != null && clock.now() - fetchedAt < CATEGORIES_CACHE_TTL) {
                return categories.asSuccess()
            }
            return remote.getServiceCategories().onSuccess {
                cachedCategories = it
                cachedAt = clock.now()
            }
        }

        override suspend fun getServices(categoryId: String?): AppResult<List<Service>> = remote.getServices(categoryId)

        // Never cached -- the Service Detail screen's own unavailability
        // check depends on this always reflecting the backend's current
        // answer (see ServicesRepository.getService's doc comment).
        override suspend fun getService(serviceId: String): AppResult<Service> = remote.getService(serviceId)
    }
