package com.aarogya.sanjeevani.core.data.doctors

import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.model.ServiceMode

/**
 * The current (non-legacy) doctor-browsing surface -- `doctors_public`/
 * `doctor_services_public` (client-contract.md §4-5), **not** the
 * `list_approved_doctors`/`get_doctor_public_profile` RPCs, which are
 * explicitly superseded (§"Legacy functions" at the bottom of that
 * doc). The two views have genuinely different shapes (a full profile
 * vs. a doctor-service pairing), so this interface exposes both rather
 * than forcing one artificial "doctor search" method to cover both.
 */
interface DoctorsRepository {
    /** `select * from doctors_public` -- every approved, non-suspended
     *  doctor, unpaginated. Only for a small, fixed-size read (Home's
     *  "featured doctors" row, e.g.) -- the Directory's own browsing
     *  list uses [getDoctorsPage] instead. */
    suspend fun getAllDoctors(): AppResult<List<DoctorPublic>>

    /**
     * One page of `doctors_public`, ordered by [DoctorPublic.fullName]
     * for a stable, deterministic scroll position -- [offset]/[limit]
     * are this method's own pagination window, not forwarded as-is from
     * any single query param. [searchQuery] matches name **or**
     * specialization (case-insensitive substring); [specialization] and
     * [mode] are the directory's own filter chips, each narrowing
     * further when set. Changing any filter should restart pagination
     * from `offset = 0` -- the result set itself changes, so a later
     * page under the old filter would make no sense appended to a new
     * one.
     */
    suspend fun getDoctorsPage(
        offset: Int,
        limit: Int,
        searchQuery: String? = null,
        specialization: String? = null,
        mode: ServiceMode? = null,
    ): AppResult<List<DoctorPublic>>

    /** `select * from doctors_public where doctor_id = ?` -- the first
     *  of the doctor-detail screen's two reads (client-contract.md §5).
     *  [com.aarogya.sanjeevani.core.common.error.AppError.NotFound] means
     *  exactly what it does for [com.aarogya.sanjeevani.core.data.services.ServicesRepository.getService]:
     *  RLS hides a suspended/no-longer-approved doctor exactly like a
     *  deleted one, so this is also how the detail screen detects that
     *  happening after its list was loaded. */
    suspend fun getDoctor(doctorId: String): AppResult<DoctorPublic>

    /** `select * from doctors_public where doctor_id in (?)` -- the full
     *  profiles for a set of ids already known from [getDoctorsForService]
     *  (which only carries the doctor-service pairing's own, narrower
     *  shape), so the Doctors-for-service list can render the same
     *  [DoctorCard][com.aarogya.sanjeevani.patient.doctors.DoctorCard] as
     *  the main directory. */
    suspend fun getDoctorsByIds(doctorIds: List<String>): AppResult<List<DoctorPublic>>

    /** `select * from doctor_services_public where service_id = ?` --
     *  which doctors offer a given service. */
    suspend fun getDoctorsForService(serviceId: String): AppResult<List<DoctorServicePublic>>

    /** `select * from doctor_services_public where doctor_id = ?` --
     *  the second of the doctor-detail screen's two reads. */
    suspend fun getServicesForDoctor(doctorId: String): AppResult<List<DoctorServicePublic>>

    /** `select * from doctor_services_public where doctor_id in (?)` --
     *  the same pairing [getServicesForDoctor] reads, batched for a
     *  whole page of the directory at once (one extra query per page,
     *  not one per doctor) so [DoctorCard][com.aarogya.sanjeevani.patient.doctors.DoctorCard]'s
     *  "a few service chips" has something to show on the un-scoped
     *  directory list, where -- unlike Doctors-for-service -- no single
     *  service id is already known. */
    suspend fun getServicesForDoctors(doctorIds: List<String>): AppResult<List<DoctorServicePublic>>

    /**
     * Resolves a batch of [DoctorPublic.avatarPath] values to short-lived
     * signed URLs in one round trip (the `doctor-avatars` bucket is
     * private -- client-contract.md §4) -- never an [AppResult]: an
     * avatar is decorative (every call site already falls back to
     * initials, see [com.aarogya.sanjeevani.core.designsystem.component.AsAvatar]),
     * so a failure here should never surface as, or block, a load
     * error. A path this couldn't resolve is simply absent from the
     * returned map, keyed by the *path*, not the doctor id (several
     * doctors asked for in one batch, one map).
     */
    suspend fun getAvatarUrls(avatarPaths: List<String>): Map<String, String>
}
