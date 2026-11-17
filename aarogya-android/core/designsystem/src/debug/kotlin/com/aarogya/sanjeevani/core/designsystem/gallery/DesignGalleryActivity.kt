package com.aarogya.sanjeevani.core.designsystem.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.ThemeMode

/** Debug-only entry point (see src/debug/AndroidManifest.xml) that shows
 *  every component this design system exports, in both themes, so a
 *  change here can be checked on-device without wiring it into a real
 *  screen first. */
class DesignGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.System) }
            AarogyaTheme(themeMode = themeMode) {
                DesignGalleryScreen(themeMode = themeMode, onThemeModeChange = { themeMode = it })
            }
        }
    }
}
