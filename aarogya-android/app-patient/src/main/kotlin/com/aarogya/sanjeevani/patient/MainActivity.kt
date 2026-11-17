package com.aarogya.sanjeevani.patient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.patient.appearance.AppearanceViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's only Activity -- everything below this is Compose
 * navigation (see `navigation/PatientNavHost.kt`), never a second
 * Activity/Fragment. [enableEdgeToEdge] is called before [setContent]
 * (after [super.onCreate], per its own documented usage) so the system
 * bars are transparent and [PatientApp]'s own window-insets handling
 * (see that file's doc comment) is actually in control of what padding
 * gets applied where, rather than the system reserving space for them
 * itself. Predictive back (Android 13+) is opted into declaratively, in
 * the manifest (`android:enableOnBackInvokedCallback`) rather than here
 * -- nothing in this app overrides the system back gesture, so
 * Navigation Compose's own default handling is all that's needed.
 *
 * [installSplashScreen] must be called before [super.onCreate] (its own
 * documented requirement) -- it reads this Activity's
 * `Theme.AarogyaPatient.Splash` theme (`AndroidManifest.xml`/`themes.xml`)
 * and switches to `postSplashScreenTheme` once the splash condition
 * clears. No custom `setKeepOnScreenCondition` here: the auth-gate check
 * [AuthGateViewModel] drives is a local, synchronous session read (no
 * network round trip), so there's no real loading gap to bridge --
 * letting the splash dismiss on first draw, the library's own default,
 * is already correct.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearanceViewModel: AppearanceViewModel = hiltViewModel()
            val themeMode by appearanceViewModel.themeMode.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            AarogyaTheme(themeMode = themeMode) {
                PatientAppRoot(windowWidthSizeClass = windowSizeClass.widthSizeClass)
            }
        }
    }
}
