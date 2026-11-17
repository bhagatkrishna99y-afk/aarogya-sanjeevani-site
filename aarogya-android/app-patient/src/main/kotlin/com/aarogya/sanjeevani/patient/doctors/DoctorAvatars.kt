package com.aarogya.sanjeevani.patient.doctors

import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic

/** [com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository.getAvatarUrls]
 *  resolves by *path* (one batch call can cover several doctors); every
 *  screen that renders a list of [DoctorPublic] wants to look one up by
 *  *doctor id* instead -- this is that one re-keying, shared so each
 *  screen's ViewModel doesn't repeat it. A doctor with no [DoctorPublic.avatarPath]
 *  or whose path didn't resolve is simply absent from the result, same
 *  "decorative, never fails" contract as the repository method itself
 *  -- [DoctorCard] already shows initials for any doctor id missing
 *  here. */
fun avatarUrlsByDoctorId(
    doctors: List<DoctorPublic>,
    urlsByPath: Map<String, String>,
): Map<String, String> =
    doctors
        .mapNotNull { doctor ->
            val path = doctor.avatarPath ?: return@mapNotNull null
            val url = urlsByPath[path] ?: return@mapNotNull null
            doctor.doctorId to url
        }
        .toMap()
