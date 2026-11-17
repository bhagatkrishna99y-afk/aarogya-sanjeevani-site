package com.aarogya.sanjeevani.patient.profile

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var viewModel: EditProfileViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        profileRepository = FakeProfileRepository()
        viewModel = EditProfileViewModel(authRepository, profileRepository)
    }

    private suspend fun loaded(
        fullName: String = "Fake Patient",
        phone: String? = null,
    ) {
        authRepository.signIn("patient@example.com", "password123")
        val existing = profileRepository.profiles.getValue(FakeAuthRepository.FAKE_USER_ID)
        profileRepository.profiles[FakeAuthRepository.FAKE_USER_ID] =
            existing.copy(fullName = fullName, phone = phone)
        viewModel.onAction(EditProfileAction.Load)
    }

    // -- Dirty-state tracking --------------------------------------------

    @Test
    fun `right after loading, nothing is dirty and Save is disabled`() =
        runTest {
            loaded(fullName = "Jane Doe", phone = "9876543210")

            assertFalse(viewModel.uiState.value.isDirty)
            assertFalse(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun `changing the full name makes the form dirty and enables Save`() =
        runTest {
            loaded(fullName = "Jane Doe")

            viewModel.onAction(EditProfileAction.FullNameChanged("Jane Smith"))

            assertTrue(viewModel.uiState.value.isDirty)
            assertTrue(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun `changing the phone makes the form dirty`() =
        runTest {
            loaded(fullName = "Jane Doe", phone = "9876543210")

            viewModel.onAction(EditProfileAction.PhoneChanged("9000000000"))

            assertTrue(viewModel.uiState.value.isDirty)
        }

    @Test
    fun `typing a change then retyping the exact original value is clean again`() =
        runTest {
            loaded(fullName = "Jane Doe")

            viewModel.onAction(EditProfileAction.FullNameChanged("Jane Smith"))
            assertTrue(viewModel.uiState.value.isDirty)

            viewModel.onAction(EditProfileAction.FullNameChanged("Jane Doe"))

            assertFalse(viewModel.uiState.value.isDirty)
            assertFalse(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun `a successful save resets the dirty baseline to the new values`() =
        runTest {
            loaded(fullName = "Jane Doe")
            viewModel.onAction(EditProfileAction.FullNameChanged("Jane Smith"))

            viewModel.uiEvent.test {
                viewModel.onAction(EditProfileAction.SaveClicked)
                assertEquals(EditProfileUiEvent.SaveSucceeded, awaitItem())
            }

            assertFalse(viewModel.uiState.value.isDirty)
            assertEquals("Jane Smith", viewModel.uiState.value.fullName)
        }

    // -- Validation ---------------------------------------------------------

    @Test
    fun `a full name under the minimum length is flagged and blocks Save`() =
        runTest {
            loaded(fullName = "Jane Doe")

            viewModel.onAction(EditProfileAction.FullNameChanged("J"))

            assertEquals(EditProfileFieldError.FULL_NAME_TOO_SHORT, viewModel.uiState.value.fieldErrors["fullName"])
            assertFalse(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun `a full name over the maximum length is flagged`() =
        runTest {
            loaded(fullName = "Jane Doe")

            viewModel.onAction(EditProfileAction.FullNameChanged("A".repeat(101)))

            assertEquals(EditProfileFieldError.FULL_NAME_TOO_LONG, viewModel.uiState.value.fieldErrors["fullName"])
        }

    @Test
    fun `a blank phone is valid -- phone is optional`() =
        runTest {
            loaded(fullName = "Jane Doe", phone = "9876543210")

            viewModel.onAction(EditProfileAction.PhoneChanged(""))

            assertFalse(viewModel.uiState.value.fieldErrors.containsKey("phone"))
            assertTrue(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun `a non-blank but malformed phone is flagged and blocks Save`() =
        runTest {
            loaded(fullName = "Jane Doe")

            viewModel.onAction(EditProfileAction.PhoneChanged("abc"))

            assertEquals(EditProfileFieldError.PHONE_INVALID, viewModel.uiState.value.fieldErrors["phone"])
            assertFalse(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun `a well-formed phone passes validation`() =
        runTest {
            loaded(fullName = "Jane Doe")

            viewModel.onAction(EditProfileAction.PhoneChanged("9876543210"))

            assertFalse(viewModel.uiState.value.fieldErrors.containsKey("phone"))
        }

    @Test
    fun `clicking Save while invalid marks every field touched instead of saving`() =
        runTest {
            loaded(fullName = "Jane Doe")
            viewModel.onAction(EditProfileAction.PhoneChanged("abc"))

            viewModel.uiEvent.test {
                viewModel.onAction(EditProfileAction.SaveClicked)
                expectNoEvents()
            }
            assertTrue(viewModel.uiState.value.submitAttempted)
            assertTrue(viewModel.uiState.value.fullNameTouched)
            assertTrue(viewModel.uiState.value.phoneTouched)
            assertEquals(0, profileRepository.updateProfileCallCount)
        }

    @Test
    fun `a failed save keeps everything the patient typed`() =
        runTest {
            loaded(fullName = "Jane Doe")
            viewModel.onAction(EditProfileAction.FullNameChanged("Jane Smith"))
            profileRepository.errorToReturn = AppError.Network()

            viewModel.uiEvent.test {
                viewModel.onAction(EditProfileAction.SaveClicked)
                assertTrue(awaitItem() is EditProfileUiEvent.SaveFailed)
            }

            assertEquals("Jane Smith", viewModel.uiState.value.fullName)
            assertTrue(viewModel.uiState.value.isDirty)
            assertFalse(viewModel.uiState.value.isSaving)
        }
}
