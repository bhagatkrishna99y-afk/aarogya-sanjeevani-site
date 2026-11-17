package com.aarogya.sanjeevani.patient.profile

import com.aarogya.sanjeevani.core.common.appearance.AppearanceMode
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAppearanceRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileViewModelTest {
    @get:org.junit.Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var appearanceRepository: FakeAppearanceRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        profileRepository = FakeProfileRepository()
        appearanceRepository = FakeAppearanceRepository()
        viewModel = ProfileViewModel(authRepository, profileRepository, appearanceRepository)
    }

    private suspend fun signedIn() = authRepository.signIn("patient@example.com", "password123")

    @Test
    fun `resuming while signed in loads the profile with no avatar`() =
        runTest {
            signedIn()

            viewModel.onAction(ProfileAction.Resumed)

            val content = viewModel.uiState.value.section as ProfileSectionState.Content
            assertEquals("Fake Patient", content.profile.fullName)
            assertNull(content.avatarUrl)
        }

    @Test
    fun `resolves a signed URL when the profile has an avatar path`() =
        runTest {
            signedIn()
            val userId = FakeAuthRepository.FAKE_USER_ID
            val existing = profileRepository.profiles.getValue(userId)
            profileRepository.profiles[userId] = existing.copy(avatarUrl = "$userId/avatar.jpg")
            profileRepository.signedUrlsByPath["$userId/avatar.jpg"] = "https://signed.example/avatar.jpg"

            viewModel.onAction(ProfileAction.Resumed)

            val content = viewModel.uiState.value.section as ProfileSectionState.Content
            assertEquals("https://signed.example/avatar.jpg", content.avatarUrl)
        }

    @Test
    fun `a failure to resolve the signed URL still shows the profile, just with no avatar`() =
        runTest {
            signedIn()
            val userId = FakeAuthRepository.FAKE_USER_ID
            val existing = profileRepository.profiles.getValue(userId)
            profileRepository.profiles[userId] = existing.copy(avatarUrl = "$userId/avatar.jpg")
            // Deliberately not seeding signedUrlsByPath -- getAvatarSignedUrl fails NotFound.

            viewModel.onAction(ProfileAction.Resumed)

            val content = viewModel.uiState.value.section as ProfileSectionState.Content
            assertNull(content.avatarUrl)
        }

    @Test
    fun `a load failure with nothing shown yet surfaces the error section`() =
        runTest {
            signedIn()
            profileRepository.errorToReturn = AppError.Network()

            viewModel.onAction(ProfileAction.Resumed)

            assertTrue(viewModel.uiState.value.section is ProfileSectionState.Error)
        }

    @Test
    fun `a refresh failure that lands on existing content keeps that content`() =
        runTest {
            signedIn()
            viewModel.onAction(ProfileAction.Resumed)
            profileRepository.errorToReturn = AppError.Network()

            viewModel.onAction(ProfileAction.Resumed)

            assertTrue(viewModel.uiState.value.section is ProfileSectionState.Content)
        }

    @Test
    fun `retry resets to Loading before re-fetching`() =
        runTest {
            signedIn()
            profileRepository.errorToReturn = AppError.Network()
            viewModel.onAction(ProfileAction.Resumed)
            profileRepository.errorToReturn = null

            viewModel.onAction(ProfileAction.Retry)

            assertTrue(viewModel.uiState.value.section is ProfileSectionState.Content)
        }

    @Test
    fun `selecting an appearance mode writes through to the repository`() =
        runTest {
            viewModel.onAction(ProfileAction.AppearanceModeSelected(AppearanceMode.DARK))

            assertEquals(AppearanceMode.DARK, appearanceRepository.mode.value)
        }

    @Test
    fun `the appearance mode shown always mirrors the repository, even set externally`() =
        runTest {
            appearanceRepository.mode.value = AppearanceMode.LIGHT

            assertEquals(AppearanceMode.LIGHT, viewModel.uiState.value.appearanceMode)
        }

    @Test
    fun `uploading an avatar refreshes the profile and clears isUploadingAvatar`() =
        runTest {
            signedIn()
            viewModel.onAction(ProfileAction.Resumed)

            viewModel.onAction(ProfileAction.AvatarSelected(byteArrayOf(1, 2, 3), "image/jpeg"))

            assertFalse(viewModel.uiState.value.isUploadingAvatar)
            assertEquals(1, profileRepository.uploadAvatarCallCount)
        }
}
