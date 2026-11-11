package com.aarogyasanjeevani.doctor.data.repository

import com.aarogyasanjeevani.doctor.data.common.safeCall
import com.aarogyasanjeevani.doctor.data.remote.dto.MyDoctorApplicationDto
import com.aarogyasanjeevani.doctor.data.remote.dto.SubmitDoctorApplicationParams
import com.aarogyasanjeevani.doctor.domain.model.DoctorApplication
import com.aarogyasanjeevani.doctor.domain.model.DoctorStatus
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorApplicationRepository @Inject constructor(
    private val postgrest: Postgrest,
) {
    /** Null means the signed-in user has never applied. */
    suspend fun fetchMyApplication(): Result<DoctorApplication?> = safeCall {
        postgrest.rpc("get_my_doctor_application")
            .decodeList<MyDoctorApplicationDto>()
            .firstOrNull()
            ?.let {
                DoctorApplication(
                    specialization = it.specialization,
                    bio = it.bio,
                    licenseNumber = it.licenseNumber,
                    yearsExperience = it.yearsExperience,
                    status = DoctorStatus.fromWire(it.status),
                    rejectionReason = it.rejectionReason,
                )
            }
    }

    suspend fun submitApplication(
        fullName: String,
        phone: String?,
        specialization: String,
        licenseNumber: String,
        bio: String?,
        yearsExperience: Int,
    ): Result<Unit> = safeCall {
        postgrest.rpc(
            "submit_doctor_application",
            SubmitDoctorApplicationParams(
                fullName = fullName,
                phone = phone,
                specialization = specialization,
                licenseNumber = licenseNumber,
                bio = bio,
                yearsExperience = yearsExperience,
            ),
        )
        Unit
    }
}
