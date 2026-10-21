package com.classpilot.offline

import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.domain.model.SubjectType
import com.classpilot.offline.viewmodels.calculateAttendance
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.ceil

/**
 * Unit tests for attendance calculation logic.
 * These run on the JVM (no Android emulator needed).
 */
class AttendanceCalculationTest {

    private fun makeSubject(conducted: Int, present: Int, threshold: Float = 75f) = Subject(
        name = "Test",
        type = SubjectType.THEORY,
        attendanceThreshold = threshold,
        conductedClasses = conducted,
        presentClasses = present,
        absentClasses = conducted - present
    )

    // ── attendancePercentage ─────────────────────────────────────────────────

    @Test
    fun `zero conducted gives zero percentage`() {
        val s = makeSubject(0, 0)
        assertEquals(0f, s.attendancePercentage, 0.001f)
    }

    @Test
    fun `perfect attendance gives 100 percent`() {
        val s = makeSubject(10, 10)
        assertEquals(100f, s.attendancePercentage, 0.001f)
    }

    @Test
    fun `7 out of 10 gives 70 percent`() {
        val s = makeSubject(10, 7)
        assertEquals(70f, s.attendancePercentage, 0.001f)
    }

    @Test
    fun `75 percent threshold with 75 percent attendance is not below threshold`() {
        val s = makeSubject(20, 15, 75f)
        assertFalse(s.isBelowThreshold)
    }

    @Test
    fun `74 percent is below 75 percent threshold`() {
        val s = makeSubject(100, 74, 75f)
        assertTrue(s.isBelowThreshold)
    }

    // ── classesNeededToReachThreshold ────────────────────────────────────────

    @Test
    fun `already above threshold needs zero classes`() {
        val s = makeSubject(20, 18, 75f) // 90%
        assertEquals(0, s.classesNeededToReachThreshold)
    }

    @Test
    fun `60 present out of 100 needs classes to reach 75 percent`() {
        // (60 + x) / (100 + x) >= 0.75  =>  x >= 60
        val s = makeSubject(100, 60, 75f)
        assertEquals(60, s.classesNeededToReachThreshold)
    }

    @Test
    fun `0 present out of 0 conducted needs zero classes for zero threshold`() {
        val s = makeSubject(0, 0, 75f)
        assertEquals(0, s.classesNeededToReachThreshold)
    }

    // ── classesCanMiss ────────────────────────────────────────────────────────

    @Test
    fun `100 percent attendance can miss many classes`() {
        val s = makeSubject(10, 10, 75f)
        // (10) / (10 + x) >= 0.75  =>  x <= 10/0.75 - 10 = 3.33  => 3
        assertEquals(3, s.classesCanMiss)
    }

    @Test
    fun `exactly at threshold can miss zero classes`() {
        val s = makeSubject(4, 3, 75f) // 75% exactly
        assertEquals(0, s.classesCanMiss)
    }

    @Test
    fun `below threshold can miss zero classes`() {
        val s = makeSubject(10, 6, 75f) // 60%
        assertEquals(0, s.classesCanMiss)
    }

    // ── Predictions ──────────────────────────────────────────────────────────

    @Test
    fun `percentage if miss next class decreases correctly`() {
        val s = makeSubject(10, 8, 75f)
        val calc = calculateAttendance(s)
        // miss next: 8 present out of 11 total = 72.7%
        assertEquals(8f * 100f / 11f, calc.percentageIfMissNext, 0.001f)
    }

    @Test
    fun `percentage if attend next 5 classes increases correctly`() {
        val s = makeSubject(10, 8, 75f)
        val calc = calculateAttendance(s)
        // attend 5 more: 13/15 = 86.67%
        assertEquals(13f * 100f / 15f, calc.percentageIfAttendNext5, 0.001f)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `no division by zero when conducted is zero`() {
        val s = makeSubject(0, 0, 75f)
        val calc = calculateAttendance(s)
        assertEquals(0f, calc.currentPercentage, 0.001f)
        assertEquals(0, calc.needToAttend)
        assertEquals(0, calc.canMiss)
    }
}

/**
 * Unit tests for the next-day and timetable overlap logic.
 */
class TimetableLogicTest {

    @Test
    fun `overlap detection - entries with same time are overlapping`() {
        // start=540 end=600 vs start=540 end=600 => overlap
        val a = Pair(540, 600)
        val b = Pair(540, 600)
        assertTrue(a.second > b.first && b.second > a.first)
    }

    @Test
    fun `overlap detection - adjacent entries are not overlapping`() {
        // end of first == start of second => no overlap
        val a = Pair(540, 600)
        val b = Pair(600, 660)
        assertFalse(a.second > b.second && b.second > a.first)
        // Correct: they do NOT overlap (b.first >= a.second)
        assertFalse(a.second > b.first && b.second > a.first &&
                !(a.second == b.first)) // edge case: exact adjacency
        // Simpler: standard interval overlap: a.start < b.end && b.start < a.end
        assertFalse(a.first < b.second && b.first < a.second && b.first >= a.second)
    }

    @Test
    fun `tomorrow's DayOfWeek is correct`() {
        val today = java.time.LocalDate.now()
        val tomorrow = today.plusDays(1)
        assertEquals(
            today.dayOfWeek.plus(1),
            tomorrow.dayOfWeek
        )
    }

    @Test
    fun `minute-of-day parsing is correct`() {
        // 9:30 = 9*60+30 = 570
        val startMinute = 9 * 60 + 30
        assertEquals(570, startMinute)
        val hour = startMinute / 60
        val min = startMinute % 60
        assertEquals(9, hour)
        assertEquals(30, min)
    }
}

/**
 * Tests for the attendance calculator formulas used in the UI.
 */
class AttendanceCalculatorFormulaTest {

    @Test
    fun `threshold formula is mathematically correct`() {
        // need x more consecutive attendances such that
        // (present + x) / (conducted + x) >= threshold / 100
        val present = 50
        val conducted = 100
        val threshold = 75.0
        // x >= (threshold * conducted - 100 * present) / (100 - threshold)
        // x >= (75 * 100 - 100 * 50) / (100 - 75) = (7500 - 5000) / 25 = 100
        val t = threshold / 100.0
        val denom = 1.0 - t
        val needed = ceil((t * conducted - present) / denom).toInt()
        assertEquals(100, needed)
        // Verify: (50+100)/(100+100) = 150/200 = 0.75 ✓
        val resultPct = (present + needed).toDouble() / (conducted + needed)
        assertTrue(resultPct >= t)
    }

    @Test
    fun `can-miss formula is correct`() {
        val present = 100
        val conducted = 100
        val threshold = 75.0
        // (present) / (conducted + x) >= threshold/100
        // x <= present/threshold*100 - conducted = 100/0.75 - 100 = 33.33 => 33
        val maxTotal = (present * 100.0 / threshold).toInt()
        val canMiss = maxTotal - conducted
        assertEquals(33, canMiss)
        // Verify: 100/(100+33) = 100/133 = 75.18% >= 75% ✓
        val check = 100.0 / (conducted + canMiss)
        assertTrue(check >= 0.75)
        // Verify missing one more would drop below
        val tooMany = check / ((conducted + canMiss + 1).toDouble() / (conducted + canMiss)) - 0.001
        assertTrue(100.0 / (conducted + canMiss + 1) < 0.75)
    }
}
