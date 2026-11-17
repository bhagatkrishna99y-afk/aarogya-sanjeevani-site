package com.aarogya.sanjeevani.patient.doctors

/** Matches a name that already opens with a courtesy title ("Dr",
 *  "Dr.", "Prof", "Prof.") followed by a space -- this is what seed/real
 *  data's [com.aarogya.sanjeevani.core.data.doctors.DoctorPublic.fullName]
 *  already looks like ("Dr. Priya Sharma"), independently of whatever
 *  [com.aarogya.sanjeevani.core.data.doctors.DoctorPublic.title] also
 *  holds for the same doctor. */
private val EXISTING_TITLE_PREFIX = Regex("""^(dr|prof)\.?\s+""", RegexOption.IGNORE_CASE)

/**
 * The one place a doctor's title and full name are combined for
 * display -- [com.aarogya.sanjeevani.core.data.doctors.DoctorPublic.title]
 * and [com.aarogya.sanjeevani.core.data.doctors.DoctorPublic.fullName]
 * are two independent columns, and real/seed data populates both ("Dr."
 * and "Dr. Priya Sharma"), so blindly prepending one onto the other
 * doubles it ("Dr. Dr. Priya Sharma"). Never prepends anything the name
 * already appears to carry, regardless of what [title] itself says.
 */
fun doctorDisplayName(
    fullName: String,
    title: String?,
): String {
    val trimmedTitle = title?.trim().orEmpty()
    if (trimmedTitle.isEmpty() || EXISTING_TITLE_PREFIX.containsMatchIn(fullName)) return fullName
    return "$trimmedTitle $fullName"
}
