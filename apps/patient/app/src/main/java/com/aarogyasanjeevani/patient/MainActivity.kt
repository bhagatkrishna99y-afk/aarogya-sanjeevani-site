package com.aarogyasanjeevani.patient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aarogyasanjeevani.patient.ui.navigation.PatientApp
import com.aarogyasanjeevani.patient.ui.theme.AarogyaSanjeevaniTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AarogyaSanjeevaniTheme {
                PatientApp()
            }
        }
    }
}
