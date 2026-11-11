package com.aarogyasanjeevani.doctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aarogyasanjeevani.doctor.ui.navigation.DoctorApp
import com.aarogyasanjeevani.doctor.ui.theme.AarogyaSanjeevaniTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AarogyaSanjeevaniTheme {
                DoctorApp()
            }
        }
    }
}
