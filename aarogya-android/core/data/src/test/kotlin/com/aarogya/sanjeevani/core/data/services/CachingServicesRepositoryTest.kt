package com.aarogya.sanjeevani.core.data.services

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.testing.fake.FakeServicesRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

private val START_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

/** A [Clock] a test can move forward by hand, without `delay`/virtual
 *  time -- [CachingServicesRepository] only ever calls [now] to compare
 *  against its own cached timestamp, so this is simpler than wiring a
 *  [kotlinx.coroutines.test.TestCoroutineScheduler] for a class with no
 *  coroutine-level timing of its own. */
private class FakeClock(private var instant: Instant) : Clock {
    override fun now(): Instant = instant

    fun advanceBy(duration: kotlin.time.Duration) {
        instant += duration
    }
}

class CachingServicesRepositoryTest {
    private lateinit var remote: FakeServicesRepository
    private lateinit var clock: FakeClock
    private lateinit var repository: CachingServicesRepository

    @Before
    fun setUp() {
        remote = FakeServicesRepository()
        clock = FakeClock(START_INSTANT)
        repository = CachingServicesRepository(remote, clock)
    }

    @Test
    fun `the first call fetches from remote`() =
        runTest {
            remote.categories += aCategory(id = "c1")

            val result = repository.getServiceCategories()

            assertEquals(listOf("c1"), (result as AppResult.Success).data.map { it.id })
        }

    @Test
    fun `a second call within the TTL returns the cached list without calling remote again`() =
        runTest {
            remote.categories += aCategory(id = "c1")
            repository.getServiceCategories()
            remote.categories += aCategory(id = "c2")

            val result = repository.getServiceCategories()

            val categories = (result as AppResult.Success).data
            assertEquals(listOf("c1"), categories.map { it.id })
        }

    @Test
    fun `a call after the TTL expires re-fetches from remote`() =
        runTest {
            remote.categories += aCategory(id = "c1")
            repository.getServiceCategories()
            remote.categories += aCategory(id = "c2")
            clock.advanceBy(6.minutes)

            val result = repository.getServiceCategories()

            val categories = (result as AppResult.Success).data
            assertEquals(setOf("c1", "c2"), categories.map { it.id }.toSet())
        }

    @Test
    fun `a failed fetch is never cached -- the next call tries remote again`() =
        runTest {
            remote.errorToReturn = AppError.Network()

            val first = repository.getServiceCategories()
            assertTrue(first is AppResult.Failure)

            remote.errorToReturn = null
            remote.categories += aCategory(id = "c1")
            val second = repository.getServiceCategories()

            assertEquals(listOf("c1"), (second as AppResult.Success).data.map { it.id })
        }

    @Test
    fun `getServices is never cached -- it always reaches remote`() =
        runTest {
            remote.services += aService(id = "s1", categoryId = "c1")
            repository.getServices(categoryId = "c1")
            remote.services += aService(id = "s2", categoryId = "c1")

            val result = repository.getServices(categoryId = "c1")

            val services = (result as AppResult.Success).data
            assertEquals(setOf("s1", "s2"), services.map { it.id }.toSet())
        }

    private fun aCategory(id: String) =
        ServiceCategory(
            id = id,
            name = "Category $id",
            slug = id,
            description = null,
            icon = null,
            isActive = true,
            sortOrder = 0,
            createdAt = START_INSTANT,
            updatedAt = START_INSTANT,
        )

    private fun aService(
        id: String,
        categoryId: String,
    ) = Service(
        id = id,
        categoryId = categoryId,
        name = "Service $id",
        slug = id,
        description = null,
        shortDescription = null,
        requiresDoctor = true,
        durationMinutes = 30,
        supportedModes = emptyList(),
        metadata = null,
        isActive = true,
        sortOrder = 0,
        createdAt = START_INSTANT,
        updatedAt = START_INSTANT,
    )
}
