package com.aarogya.sanjeevani.patient.doctors

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeDoctorsRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val DOCTOR_ID = "doc-1"
private val FIXED_NOW = Instant.parse("2026-01-05T00:00:00Z") // a Monday, well inside the clinic's own day in IST.

class AvailabilityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var requestsRepository: FakeRequestsRepository
    private lateinit var doctorsRepository: FakeDoctorsRepository
    private lateinit var viewModel: AvailabilityViewModel
    private lateinit var today: LocalDate

    @Before
    fun setUp() {
        requestsRepository = FakeRequestsRepository()
        doctorsRepository = FakeDoctorsRepository()
        doctorsRepository.doctors += aDoctor()
        val clock =
            object : Clock {
                override fun now() = FIXED_NOW
            }
        viewModel = AvailabilityViewModel(requestsRepository, doctorsRepository, clock)
        today = viewModel.uiState.value.today
    }

    @Test
    fun `rapid date changes -- only the latest date's result is shown, the stale one is dropped`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val dateA = today
            val dateB = today.plus(1, DateTimeUnit.DAY)
            val slotA = anOpenSlot(hour = 9)
            val slotB = anOpenSlot(hour = 15)
            requestsRepository.openSlotsByDate[dateA] = listOf(slotA)
            requestsRepository.openSlotsByDate[dateB] = listOf(slotB)

            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            // Both calls now suspend on a real delay -- long enough that,
            // without flatMapLatest's own cancellation, dateA's slower
            // response could still land after dateB's and clobber it.
            requestsRepository.getOpenSlotsDelayMillis = 1_000
            viewModel.onAction(AvailabilityAction.DateSelected(dateA))
            viewModel.onAction(AvailabilityAction.DateSelected(dateB))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val section = viewModel.uiState.value.slotsSection as SlotsSectionState.Content
            assertEquals(listOf(slotB), section.slots)
            assertEquals(dateB, viewModel.uiState.value.selectedDate)
        }

    @Test
    fun `selecting a slot then changing the date clears the selection`() =
        runTest {
            val slot = anOpenSlot(hour = 9)
            requestsRepository.openSlotsByDate[today] = listOf(slot)
            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))
            viewModel.onAction(AvailabilityAction.SlotSelected(slot))
            assertEquals(slot, viewModel.uiState.value.selectedSlot)

            viewModel.onAction(AvailabilityAction.DateSelected(today.plus(1, DateTimeUnit.DAY)))

            assertNull(viewModel.uiState.value.selectedSlot)
        }

    @Test
    fun `a slot that vanished by the time a silent resume refresh runs is cleared and reported as taken`() =
        runTest {
            val slot = anOpenSlot(hour = 9)
            requestsRepository.openSlotsByDate[today] = listOf(slot)
            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))
            viewModel.onAction(AvailabilityAction.SlotSelected(slot))
            assertEquals(slot, viewModel.uiState.value.selectedSlot)

            // Someone else took it between this screen's first load and now.
            requestsRepository.openSlotsByDate[today] = emptyList()

            viewModel.uiEvent.test {
                viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))
                assertEquals(AvailabilityUiEvent.SlotTaken, awaitItem())
            }
            assertNull(viewModel.uiState.value.selectedSlot)
        }

    @Test
    fun `continuing re-checks the slot and reports it taken if it vanished right before submit`() =
        runTest {
            val slot = anOpenSlot(hour = 9)
            requestsRepository.openSlotsByDate[today] = listOf(slot)
            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))
            viewModel.onAction(AvailabilityAction.SlotSelected(slot))

            requestsRepository.openSlotsByDate[today] = emptyList()

            viewModel.uiEvent.test {
                viewModel.onAction(AvailabilityAction.ContinueClicked)
                assertEquals(AvailabilityUiEvent.SlotTaken, awaitItem())
            }
            assertNull(viewModel.uiState.value.selectedSlot)
            assertFalse(viewModel.uiState.value.isContinuing)
        }

    @Test
    fun `continuing with a slot still available sends its start and duration onward`() =
        runTest {
            val slot = anOpenSlot(hour = 9, durationMinutes = 45)
            requestsRepository.openSlotsByDate[today] = listOf(slot)
            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))
            viewModel.onAction(AvailabilityAction.SlotSelected(slot))

            viewModel.uiEvent.test {
                viewModel.onAction(AvailabilityAction.ContinueClicked)
                val event = awaitItem() as AvailabilityUiEvent.NavigateToRequestForm
                assertEquals(slot.slotStart, event.slotStart)
                assertEquals(45, event.slotDurationMinutes)
            }
        }

    @Test
    fun `rapid double taps on Continue only trigger one slot re-check`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val slot = anOpenSlot(hour = 9)
            requestsRepository.openSlotsByDate[today] = listOf(slot)
            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            viewModel.onAction(AvailabilityAction.SlotSelected(slot))

            requestsRepository.getOpenSlotsDelayMillis = 1_000
            val callsBeforeContinuing = requestsRepository.getOpenSlotsCallCount
            viewModel.onAction(AvailabilityAction.ContinueClicked)
            viewModel.onAction(AvailabilityAction.ContinueClicked)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(callsBeforeContinuing + 1, requestsRepository.getOpenSlotsCallCount)
        }

    @Test
    fun `an empty result for a doctor who stopped accepting requests is DoctorUnavailable, not Empty`() =
        runTest {
            doctorsRepository.doctors[0] = doctorsRepository.doctors[0].copy(acceptingRequests = false)
            requestsRepository.openSlotsByDate[today] = emptyList()

            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))

            assertTrue(viewModel.uiState.value.slotsSection is SlotsSectionState.DoctorUnavailable)
        }

    @Test
    fun `an empty result for a doctor still accepting requests is a plain Empty state`() =
        runTest {
            requestsRepository.openSlotsByDate[today] = emptyList()

            viewModel.onAction(AvailabilityAction.Resumed(DOCTOR_ID))

            assertTrue(viewModel.uiState.value.slotsSection is SlotsSectionState.Empty)
        }

    private fun aDoctor() =
        DoctorPublic(
            doctorId = DOCTOR_ID,
            fullName = "Dr. Asha Rao",
            title = "Dr.",
            specialization = "General Medicine",
            qualifications = "MBBS",
            bio = "A caring general physician.",
            yearsOfExperience = 8,
            consultationModes = listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE),
            acceptingRequests = true,
            avatarPath = null,
        )

    private fun anOpenSlot(
        hour: Int,
        durationMinutes: Int = 30,
    ): OpenSlot {
        val start = today.atStartOfDayIn(TimeZone.UTC) + hour.hours
        return OpenSlot(slotStart = start, slotEnd = start + durationMinutes.minutes)
    }
}
