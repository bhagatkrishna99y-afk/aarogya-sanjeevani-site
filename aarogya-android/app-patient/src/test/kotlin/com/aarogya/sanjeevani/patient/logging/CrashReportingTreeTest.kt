package com.aarogya.sanjeevani.patient.logging

import android.util.Log
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [CrashReportingTree] has no real backend wired in yet (see its own
 * doc comment), so these tests only cover the two safety rules that
 * matter regardless of what eventually replaces `recordNonFatal`:
 * verbose/debug/info never reaches it, and a log call with no
 * [Throwable] is dropped (this tree only ever exists to report
 * exceptions, never a free-text message on its own).
 */
class CrashReportingTreeTest {
    private val tree = CrashReportingTree()

    @Test
    fun `verbose debug and info are not loggable`() {
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.VERBOSE))
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.DEBUG))
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.INFO))
    }

    @Test
    fun `warn and error are loggable`() {
        assertTrue(tree.isLoggable(tag = "Test", priority = Log.WARN))
        assertTrue(tree.isLoggable(tag = "Test", priority = Log.ERROR))
    }

    @Test
    fun `log never throws, with or without a throwable attached`() {
        tree.log(Log.ERROR, "Test", "something failed", RuntimeException("boom"))
        tree.log(Log.ERROR, "Test", "something failed", null)
    }
}
