package com.aarogya.sanjeevani.core.data.auth

/**
 * What actually happened after [AuthRepository.signUpPatient] -- a
 * successful [AppResult.Success] either way, the two cases are both
 * genuine signup outcomes, not errors (client-contract.md §1): whether
 * [SignedIn] or [PendingEmailConfirmation] comes back depends entirely
 * on this Supabase project's own "Confirm email" setting, not on
 * anything the app controls.
 */
sealed interface SignUpOutcome {
    /** No email confirmation required (or it's disabled for this
     *  project) -- the new account already has an active session. */
    data class SignedIn(val userId: String) : SignUpOutcome

    /** Email confirmation is required -- no session yet; the UI should
     *  show a "check your inbox" state rather than routing to signed-in
     *  content. */
    data object PendingEmailConfirmation : SignUpOutcome
}
