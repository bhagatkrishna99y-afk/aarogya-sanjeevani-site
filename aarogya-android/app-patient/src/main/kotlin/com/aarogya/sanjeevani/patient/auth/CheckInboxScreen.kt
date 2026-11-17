package com.aarogya.sanjeevani.patient.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/**
 * Reached only when the Supabase project requires email confirmation
 * (see [com.aarogya.sanjeevani.patient.navigation.CheckInbox]'s doc
 * comment) -- [onBackToLogin] clears this and the sign-up form off the
 * back stack, since there's nothing to come back to here once the user
 * has confirmed (or given up and wants to try a different email via
 * sign-up again from a clean Welcome screen).
 */
@Composable
fun CheckInboxScreen(
    email: String,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(AsTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = Icons.Filled.MarkEmailRead,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Check your inbox",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text =
                    "We've sent a confirmation link to $email. " +
                        "Open it to finish creating your account, then log in.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            AsButton(text = "Back to login", onClick = onBackToLogin)
        }
    }
}
