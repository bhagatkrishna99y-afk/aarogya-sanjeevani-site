package com.aarogyasanjeevani.patient.data.repository

import com.aarogyasanjeevani.patient.data.common.safeCall
import com.aarogyasanjeevani.patient.data.remote.dto.DoctorPublicDto
import com.aarogyasanjeevani.patient.data.remote.dto.GetAvailableSlotsParams
import com.aarogyasanjeevani.patient.data.remote.dto.ListApprovedDoctorsParams
import com.aarogyasanjeevani.patient.data.remote.dto.SlotDto
import com.aarogyasanjeevani.patient.domain.model.AvailableSlot
import com.aarogyasanjeevani.patient.domain.model.Doctor
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorRepository @Inject constructor(
    private val postgrest: Postgrest,
) {
    /** Approved doctors offering [serviceId] -- only safe, public columns (never license/admin fields). */
    suspend fun fetchDoctorsForService(serviceId: String): Result<List<Doctor>> = safeCall {
        postgrest.rpc("list_approved_doctors", ListApprovedDoctorsParams(serviceId = serviceId))
            .decodeList<DoctorPublicDto>()
            .map { it.toDomain() }
    }

    suspend fun fetchDoctorProfile(doctorId: String): Result<Doctor> = safeCall {
        postgrest.rpc("get_doctor_public_profile", mapOf("p_doctor_id" to doctorId))
            .decodeList<DoctorPublicDto>()
            .first()
            .toDomain()
    }

    /** Open slots for [doctorId]/[serviceId] on [date], computed server-side (backend is the source of truth). */
    suspend fun fetchAvailableSlots(doctorId: String, serviceId: String, date: LocalDate): Result<List<AvailableSlot>> =
        safeCall {
            postgrest.rpc(
                "get_available_slots",
                GetAvailableSlotsParams(doctorId = doctorId, serviceId = serviceId, date = date.toString()),
            ).decodeList<SlotDto>().map {
                AvailableSlot(start = Instant.parse(it.slotStart), end = Instant.parse(it.slotEnd))
            }
        }

    private fun DoctorPublicDto.toDomain() = Doctor(
        id = doctorId,
        fullName = fullName,
        specialization = specialization,
        bio = bio,
        yearsExperience = yearsExperience,
    )
}
