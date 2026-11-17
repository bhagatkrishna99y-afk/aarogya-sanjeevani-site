package com.aarogya.sanjeevani.patient.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Shown for exactly as long as [com.aarogya.sanjeevani.core.data.auth.AuthState]
 * is [com.aarogya.sanjeevani.core.data.auth.AuthState.Loading] -- the
 * auth gate itself (`AuthGateEffect.kt`) decides when to navigate away
 * from this destination, this composable has no opinion on auth at all.
 */
@Composable
fun SplashScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Loading Aarogya Sanjeevani" },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}
