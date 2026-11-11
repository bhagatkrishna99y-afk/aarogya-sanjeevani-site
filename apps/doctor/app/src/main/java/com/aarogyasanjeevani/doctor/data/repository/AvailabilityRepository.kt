package com.aarogyasanjeevani.doctor.data.repository

import com.aarogyasanjeevani.doctor.data.common.safeCall
import com.aarogyasanjeevani.doctor.data.remote.dto.SetWeeklyAvailabilityParams
import com.aarogyasanjeevani.doctor.data.remote.dto.WeeklyAvailabilityDto
import com.aarogyasanjeevani.doctor.domain.model.DayAvailability
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvailabilityRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun fetchMyWeeklyAvailability(): Result<List<DayAvailability>> = safeCall {
        val userId = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")
        postgrest.from("doctor_weekly_availability")
            .select { filter { eq("doctor_id", userId) } }
            .decodeList<WeeklyAvailabilityDto>()
            .map {
                DayAvailability(
                    dayOfWeek = it.dayOfWeek,
                    startTime = it.startTime.toLocalTime(),
                    endTime = it.endTime.toLocalTime(),
                    slotDurationMinutes = it.slotDurationMinutes,
                )
            }
    }

    suspend fun updateMyWeeklyAvailability(days: List<DayAvailability>): Result<Unit> = safeCall {
        val schedule = buildJsonArray {
            days.forEach { day ->
                add(
                    buildJsonObject {
                        put("day_of_week", JsonPrimitive(day.dayOfWeek))
                        put("start_time", JsonPrimitive(day.startTime.toTimeString()))
                        put("end_time", JsonPrimitive(day.endTime.toTimeString()))
                        put("slot_duration_minutes", JsonPrimitive(day.slotDurationMinutes))
                    },
                )
            }
        }
        postgrest.rpc("set_weekly_availability", SetWeeklyAvailabilityParams(schedule = schedule))
        Unit
    }

    private fun LocalTime.toTimeString(): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:00"

    /** Parses a Postgres "HH:MM:SS" (or "HH:MM") string manually, avoiding any [LocalTime.parse] format assumptions. */
    private fun String.toLocalTime(): LocalTime {
        val parts = split(":")
        return LocalTime(hour = parts[0].toInt(), minute = parts[1].toInt())
    }
}
