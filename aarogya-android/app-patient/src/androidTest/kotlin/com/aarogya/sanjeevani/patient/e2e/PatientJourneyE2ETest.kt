package com.aarogya.sanjeevani.patient.e2e

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.aarogya.sanjeevani.core.common.di.DefaultDispatcherProvider
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepositoryImpl
import com.aarogya.sanjeevani.core.data.auth.RealAuthGateway
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepositoryImpl
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.profile.ProfileRepositoryImpl
import com.aarogya.sanjeevani.core.data.requests.NewServiceRequest
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import com.aarogya.sanjeevani.core.data.requests.RequestsRepositoryImpl
import com.aarogya.sanjeevani.core.data.services.ServicesRepositoryImpl
import com.aarogya.sanjeevani.core.network.EncryptedSessionManager
import com.aarogya.sanjeevani.core.network.SupabaseConfig
import com.aarogya.sanjeevani.patient.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val SEEDED_PATIENT_EMAIL = "patient1@aarogya.test"
private const val SEEDED_PATIENT_PASSWORD = "DevPass123!"
private const val BOOKING_WINDOW_DAYS = 14

/**
 * A small, real end-to-end suite -- no fakes anywhere in this file --
 * against whatever backend `BuildConfig.SUPABASE_URL` (the `dev` flavor)
 * actually points at. It exercises the same repository classes production
 * code uses, constructed by hand exactly like `NetworkModule`/`AuthModule`
 * would (this module has no Hilt test runner set up, and didn't need one
 * for every other androidTest file, which all drive a real ViewModel
 * against fakes instead -- this is the one place that needs the *real*
 * [SupabaseClient]).
 *
 * **This class needs infrastructure this repo's CI doesn't have and this
 * project's own development machine didn't have either when this was
 * written**: a reachable Supabase project with `backend/supabase/seed/seed.sql`
 * applied. Two ways to get there:
 *
 * 1. `cd backend && supabase start` (needs Docker), then either
 *    `adb reverse tcp:54321 tcp:54321` (phone reaches the host's own
 *    `localhost`) or point `SUPABASE_URL_DEV` in `local.properties` at
 *    your machine's LAN IP instead of `localhost` -- a physical device
 *    can never resolve `localhost` as "this computer."
 * 2. A real hosted dev Supabase project with the same seed data applied,
 *    with `local.properties`'s `SUPABASE_URL_DEV`/`SUPABASE_ANON_KEY_DEV`
 *    pointed at it.
 *
 * Either way, rebuild (`./gradlew :app-patient:installDevDebug`) before
 * running this -- `SUPABASE_URL_DEV` is baked into `BuildConfig` at
 * build time, not read at runtime. Run only this class
 * (`connectedDevDebugAndroidTest --tests "*.e2e.*"`), never as part of a
 * full androidTest sweep that's also exercising a real backend's rate
 * limits/seed data from other suites.
 */
class PatientJourneyE2ETest {
    private lateinit var client: SupabaseClient
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var requestsRepository: RequestsRepositoryImpl

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val sessionManager = EncryptedSessionManager(context, DefaultDispatcherProvider())
        val config = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }

        client =
            createSupabaseClient(supabaseUrl = config.url, supabaseKey = config.anonKey) {
                defaultSerializer = KotlinXSerializer(json)
                install(Auth) { this.sessionManager = sessionManager }
                install(Postgrest)
                install(Storage)
            }

        val profileRepository = ProfileRepositoryImpl(client)
        authRepository = AuthRepositoryImpl(RealAuthGateway(client), profileRepository)
        requestsRepository = RequestsRepositoryImpl(client)
    }

    @Test
    fun signingIn_withTheSeededPatientAccount_succeeds() =
        runBlocking {
            val result = authRepository.signInAsPatient(SEEDED_PATIENT_EMAIL, SEEDED_PATIENT_PASSWORD)

            assertTrue(
                "Expected a successful sign-in for the seeded patient -- see this class's own " +
                    "doc comment if this fails with a network error (no reachable backend configured).",
                result is AppResult.Success,
            )
        }

    @Test
    fun theSeededApprovedDoctor_hasOpenSlotsSomewhereInTheBookingWindow() =
        runBlocking {
            authRepository.signInAsPatient(SEEDED_PATIENT_EMAIL, SEEDED_PATIENT_PASSWORD)
            val doctorsRepository = DoctorsRepositoryImpl(client)
            val doctors = (doctorsRepository.getAllDoctors() as AppResult.Success).data
            // Any one accepting doctor proves the claim this test makes --
            // .single() assumed the seed data had exactly one, which no
            // longer held once a second accepting doctor was seeded.
            val approvedDoctor = doctors.first { it.doctorId.isNotBlank() && it.acceptingRequests }

            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val foundOpenSlot =
                (0 until BOOKING_WINDOW_DAYS).any { daysAhead ->
                    val date = today.plus(datePeriodOfDays(daysAhead))
                    val slots = requestsRepository.getOpenSlots(approvedDoctor.doctorId, date)
                    (slots as? AppResult.Success<List<OpenSlot>>)?.data?.isNotEmpty() == true
                }

            assertTrue("Expected at least one open slot for the seeded doctor within the booking window", foundOpenSlot)
        }

    @Test
    fun creatingAndThenCancellingARequest_reflectsBothStepsInMyRequests() =
        runBlocking {
            authRepository.signInAsPatient(SEEDED_PATIENT_EMAIL, SEEDED_PATIENT_PASSWORD)
            val doctorsRepository = DoctorsRepositoryImpl(client)
            val doctor =
                (doctorsRepository.getAllDoctors() as AppResult.Success).data
                    .first { it.acceptingRequests }
            val service =
                (ServicesRepositoryImpl(client).getServices(categoryId = null) as AppResult.Success).data
                    .first { it.requiresDoctor }

            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            var slot: OpenSlot? = null
            for (daysAhead in 0 until BOOKING_WINDOW_DAYS) {
                val date = today.plus(datePeriodOfDays(daysAhead))
                val result = requestsRepository.getOpenSlots(doctor.doctorId, date)
                val openSlots = (result as? AppResult.Success<List<OpenSlot>>)?.data
                if (!openSlots.isNullOrEmpty()) {
                    slot = openSlots.first()
                    break
                }
            }
            val foundSlot =
                slot ?: error(
                    "No open slot found for the seeded doctor in the booking window -- is the seed data fresh?",
                )

            // getServices() only orders by sort_order, which resets to 1
            // within every category -- so the first requires-a-doctor
            // service isn't deterministically "General Physician
            // Consultation" (in_person+online); it could just as easily
            // be one that's home_visit-only. Hardcoding mode = ONLINE and
            // address = null made the backend legitimately reject the
            // request whenever that tie broke the other way (AS011:
            // home_visit requires a non-null address).
            val mode = service.supportedModes.first()
            val createResult =
                requestsRepository.createRequest(
                    NewServiceRequest(
                        serviceId = service.id,
                        doctorId = doctor.doctorId,
                        slotStart = foundSlot.slotStart,
                        mode = mode,
                        notes = "Created by PatientJourneyE2ETest -- safe to ignore/delete.",
                        contactPhone = "9876543210",
                        address = if (mode == ServiceMode.HOME_VISIT) "123 Test Street, Test City" else null,
                    ),
                )
            val requestId = (createResult as AppResult.Success).data

            val myRequests = (requestsRepository.getMyRequestsAsPatient() as AppResult.Success).data
            assertTrue(myRequests.any { it.id == requestId })

            val cancelResult =
                requestsRepository.transitionStatus(requestId, RequestStatus.CANCELLED, "E2E test cleanup")
            assertTrue(cancelResult is AppResult.Success)

            val afterCancel = (requestsRepository.getMyRequestsAsPatient() as AppResult.Success).data
            assertEquals(RequestStatus.CANCELLED, afterCancel.single { it.id == requestId }.status)
        }
}

/** [kotlinx.datetime.LocalDate] has no direct "+N days" without pulling
 *  in a full [kotlinx.datetime.DatePeriod] -- this is that, named for
 *  what it's actually used for here. */
private fun datePeriodOfDays(days: Int) = kotlinx.datetime.DatePeriod(days = days)
