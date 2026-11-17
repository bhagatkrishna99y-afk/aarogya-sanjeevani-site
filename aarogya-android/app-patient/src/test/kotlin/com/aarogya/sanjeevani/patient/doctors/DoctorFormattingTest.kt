package com.aarogya.sanjeevani.patient.doctors

import org.junit.Assert.assertEquals
import org.junit.Test

class DoctorFormattingTest {
    @Test
    fun `a title is prepended when the name doesn't already carry one`() {
        assertEquals("Prof. Priya Sharma", doctorDisplayName("Priya Sharma", "Prof."))
    }

    @Test
    fun `a Dr title is not doubled when the name already starts with Dr dot`() {
        assertEquals("Dr. Priya Sharma", doctorDisplayName("Dr. Priya Sharma", "Dr."))
    }

    @Test
    fun `a Dr title with no trailing dot is not doubled either`() {
        assertEquals("Dr. Priya Sharma", doctorDisplayName("Dr. Priya Sharma", "Dr"))
    }

    @Test
    fun `a Prof title is not doubled when the name already starts with Prof`() {
        assertEquals("Prof. Arjun Nair", doctorDisplayName("Prof. Arjun Nair", "Prof."))
    }

    @Test
    fun `no title at all leaves the name exactly as it was`() {
        assertEquals("Priya Sharma", doctorDisplayName("Priya Sharma", null))
    }

    @Test
    fun `a blank title is treated the same as no title`() {
        assertEquals("Priya Sharma", doctorDisplayName("Priya Sharma", "  "))
    }

    @Test
    fun `a name that already starts with Dr is left alone even if title says something else`() {
        assertEquals("Dr. Priya Sharma", doctorDisplayName("Dr. Priya Sharma", "Prof."))
    }
}
