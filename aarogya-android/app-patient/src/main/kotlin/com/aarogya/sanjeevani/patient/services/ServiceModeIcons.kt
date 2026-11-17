package com.aarogya.sanjeevani.patient.services

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.aarogya.sanjeevani.core.data.model.ServiceMode

/** A short label + icon for each [ServiceMode] -- used on both the
 *  Services list row (icon only, the label is its content description)
 *  and the Service Detail screen (icon + label together). */
fun ServiceMode.icon(): ImageVector =
    when (this) {
        ServiceMode.IN_PERSON -> Icons.Filled.LocationOn
        ServiceMode.HOME_VISIT -> Icons.Filled.Home
        ServiceMode.ONLINE -> Icons.Filled.Videocam
    }

fun ServiceMode.label(): String =
    when (this) {
        ServiceMode.IN_PERSON -> "In person"
        ServiceMode.HOME_VISIT -> "Home visit"
        ServiceMode.ONLINE -> "Online"
    }
