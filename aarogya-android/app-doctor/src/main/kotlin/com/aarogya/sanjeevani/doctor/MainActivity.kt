package com.aarogya.sanjeevani.doctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import dagger.hilt.android.AndroidEntryPoint

/** Proves the module wiring works: launches straight to a blank Compose
 *  screen showing this flavor's own app name (see
 *  AndroidApplicationConventionPlugin and this module's build.gradle.kts
 *  for where `app_name` comes from). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AarogyaTheme {
                DoctorAppRoot()
            }
        }
    }
}

@Composable
private fun DoctorAppRoot() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
