package com.aarogyasanjeevani.doctor.data.repository

import com.aarogyasanjeevani.doctor.data.common.safeCall
import com.aarogyasanjeevani.doctor.data.remote.dto.DoctorServiceDto
import com.aarogyasanjeevani.doctor.data.remote.dto.ServiceDto
import com.aarogyasanjeevani.doctor.data.remote.dto.SetDoctorServicesParams
import com.aarogyasanjeevani.doctor.domain.model.Service
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServicesRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    /** All services a doctor could offer (requires_doctor=true, active). */
    suspend fun fetchBookableServices(): Result<List<Service>> = safeCall {
        postgrest.from("services")
            .select {
                filter {
                    eq("requires_doctor", true)
                    eq("is_active", true)
                }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<ServiceDto>()
            .map { Service(id = it.id, name = it.name, description = it.description, durationMinutes = it.durationMinutes) }
    }

    /** Service ids the signed-in doctor currently offers. */
    suspend fun fetchMySelectedServiceIds(): Result<Set<String>> = safeCall {
        val userId = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")
        postgrest.from("doctor_services")
            .select { filter { eq("doctor_id", userId) } }
            .decodeList<DoctorServiceDto>()
            .map { it.serviceId }
            .toSet()
    }

    suspend fun updateMyServices(serviceIds: Set<String>): Result<Unit> = safeCall {
        postgrest.rpc("set_doctor_services", SetDoctorServicesParams(serviceIds = serviceIds.toList()))
        Unit
    }
}
