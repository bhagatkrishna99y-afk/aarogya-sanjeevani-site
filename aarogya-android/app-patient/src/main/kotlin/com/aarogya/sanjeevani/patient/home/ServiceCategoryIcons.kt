package com.aarogya.sanjeevani.patient.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps a [com.aarogya.sanjeevani.core.data.services.ServiceCategory.icon]
 * key to a Material icon. That column is free-text (an admin can set it
 * to anything via `admin_upsert_service_category` -- client-contract.md
 * §"Create/edit a category"), so this is necessarily a best-effort
 * mapping of the keys the seed data actually uses today
 * (`backend/supabase/seed/seed.sql`), not an exhaustive enum -- any
 * other key (including `null`, for a category with none set) falls
 * back to [Icons.Filled.MedicalServices], the same icon this app's own
 * bottom nav already uses for "Services" in general.
 */
fun iconForCategory(iconKey: String?): ImageVector =
    when (iconKey) {
        "stethoscope" -> Icons.Filled.MedicalServices
        "user-md" -> Icons.Filled.LocalHospital
        "flask" -> Icons.Filled.Biotech
        "home" -> Icons.Filled.Home
        "heart-pulse" -> Icons.Filled.MonitorHeart
        else -> Icons.Filled.MedicalServices
    }
