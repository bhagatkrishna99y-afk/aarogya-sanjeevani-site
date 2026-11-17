package com.aarogya.sanjeevani.core.data.auth

import com.aarogya.sanjeevani.core.common.error.AUTH_EMAIL_ALREADY_IN_USE_CODE
import com.aarogya.sanjeevani.core.common.error.AUTH_EMAIL_NOT_CONFIRMED_CODE
import com.aarogya.sanjeevani.core.common.error.AUTH_EMAIL_SEND_RATE_LIMITED_CODE
import com.aarogya.sanjeevani.core.common.error.AUTH_INVALID_CREDENTIALS_CODE
import com.aarogya.sanjeevani.core.common.error.AUTH_INVALID_EMAIL_ADDRESS_CODE
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.common.result.map
import com.aarogya.sanjeevani.core.data.model.UserRole
import com.aarogya.sanjeevani.core.data.profile.ProfileRepository
import com.aarogya.sanjeevani.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val authGateway: AuthGateway,
        private val profileRepository: ProfileRepository,
    ) : AuthRepository {
        override fun observeAuthState(): Flow<AuthState> = authGateway.sessionStatus.map { it.toAuthState() }

        override fun currentUserId(): UserId? = authGateway.currentSessionOrNull()?.user?.id

        override suspend fun signUpPatient(
            fullName: String,
            email: String,
            password: String,
        ): AppResult<SignUpOutcome> =
            safeApiCall {
                authGateway.signUpWithEmail(
                    email = email,
                    password = password,
                    metadata = mapOf("full_name" to fullName, "signup_role" to "user"),
                )
            }.refineAuthError().map {
                val userId = authGateway.currentSessionOrNull()?.user?.id
                if (userId != null) SignUpOutcome.SignedIn(userId) else SignUpOutcome.PendingEmailConfirmation
            }

        override suspend fun signIn(
            email: String,
            password: String,
        ): AppResult<Unit> = safeApiCall { authGateway.signInWithEmail(email, password) }.refineAuthError()

        override suspend fun signInAsPatient(
            email: String,
            password: String,
        ): AppResult<PatientSignInResult> {
            val signInResult = signIn(email, password)
            val userId = currentUserId()
            return when {
                signInResult is AppResult.Failure -> signInResult
                userId == null -> AppError.Unknown().asError()
                else ->
                    when (val profileResult = profileRepository.getProfile(userId)) {
                        is AppResult.Failure -> {
                            runCatching { authGateway.signOut() }
                            profileResult
                        }
                        is AppResult.Success -> {
                            val profile = profileResult.data
                            when {
                                profile.isSuspended -> {
                                    runCatching { authGateway.signOut() }
                                    PatientSignInResult.AccountSuspended.asSuccess()
                                }
                                profile.role != UserRole.USER -> {
                                    runCatching { authGateway.signOut() }
                                    PatientSignInResult.WrongRole(profile.role).asSuccess()
                                }
                                else -> PatientSignInResult.Success(userId).asSuccess()
                            }
                        }
                    }
            }
        }

        override suspend fun signOut(): AppResult<Unit> = safeApiCall { authGateway.signOut() }

        override suspend fun sendPasswordReset(email: String): AppResult<Unit> =
            safeApiCall {
                authGateway.resetPasswordForEmail(email)
            }

        /**
         * Supabase Auth's own errors (wrong password, unconfirmed email,
         * duplicate email on sign-up) are not this backend's AS0xx codes,
         * so the generic mapper in :core:network (scoped to that registry)
         * leaves them as [AppError.Unknown] -- refined here, close to the
         * two places ([signIn], [signUpPatient]) that actually call into
         * [AuthGateway]. [AuthGateway] never throws supabase-kt's own
         * exception types directly (see [AuthGatewayException]), so this
         * only ever needs to know about this module's own small exception
         * hierarchy, not supabase-kt's.
         */
        private fun <T> AppResult<T>.refineAuthError(): AppResult<T> {
            if (this !is AppResult.Failure) return this
            // safeApiCall/toAppError has no AS-code to recognize an
            // AuthGatewayException by, so it always lands here as
            // AppError.Unknown, its cause intact.
            val cause = (error as? AppError.Unknown)?.cause
            val code =
                when (cause) {
                    is AuthGatewayException.InvalidCredentials -> AUTH_INVALID_CREDENTIALS_CODE
                    is AuthGatewayException.EmailNotConfirmed -> AUTH_EMAIL_NOT_CONFIRMED_CODE
                    is AuthGatewayException.EmailAlreadyInUse -> AUTH_EMAIL_ALREADY_IN_USE_CODE
                    is AuthGatewayException.InvalidEmailAddress -> AUTH_INVALID_EMAIL_ADDRESS_CODE
                    is AuthGatewayException.EmailSendRateLimited -> AUTH_EMAIL_SEND_RATE_LIMITED_CODE
                    else -> null
                }
            return if (code == null) {
                this
            } else {
                AppError.Business(code = code, message = cause?.message.orEmpty(), cause = cause).asError()
            }
        }
    }
