package com.adll.de_general.feature.journal.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every case here pins a real instant against a real zone.
 *
 * [NOW] is Thursday 22 October 2026, 08:45 PM in `America/New_York` — checked, not copied. The
 * Stitch mock's own sample date, "Thursday, Oct 24, 2026", is a **Saturday**, which is exactly why
 * nothing in this app prints a weekday it was handed rather than one it derived.
 */
class JournalDatesTest {

    private val newYork = TimeZone.of("America/New_York")
    private val tokyo = TimeZone.of("Asia/Tokyo")

    private val now = 1_792_716_300_000L

    @Test
    fun theFixtureIsTheDayItClaimsToBe() {
        assertEquals("Thursday, Oct 22, 2026", formatDay(now, newYork))
        assertEquals("08:45 PM", formatTime(now, newYork))
    }

    /**
     * The reason [TimeZone] is a parameter everywhere in this file rather than a default read from
     * the device: it changes the answer.
     */
    @Test
    fun theSameInstantIsADifferentDayInADifferentZone() {
        assertEquals(LocalDate(2026, 10, 22), localDateAt(now, newYork))
        assertEquals(LocalDate(2026, 10, 23), localDateAt(now, tokyo))
    }

    @Test
    fun anchoringToTodayIsExactlyNow() {
        assertEquals(now, resolveAnchor(DateAnchor.Today, now, newYork))
    }

    /**
     * Never midnight. A fixed 00:00 would collide for two entries backdated to the same day, would
     * render as "12:00 AM" — a time nobody chose — and would stop Today agreeing with the picker.
     */
    @Test
    fun backdatingKeepsTheTimeOfDayYouSavedAt() {
        val stamped = resolveAnchor(DateAnchor.Yesterday, now, newYork)

        assertEquals(LocalDate(2026, 10, 21), localDateAt(stamped, newYork))
        assertEquals("08:45 PM", formatTime(stamped, newYork))
    }

    @Test
    fun aPickedDateIsStampedAtTheSameTimeOfDay() {
        val stamped = resolveAnchor(DateAnchor.On(LocalDate(2026, 1, 3)), now, newYork)

        assertEquals("Saturday, Jan 03, 2026", formatDay(stamped, newYork))
        assertEquals("08:45 PM", formatTime(stamped, newYork))
    }

    /**
     * 8 March 2026 is the US spring-forward Sunday: 02:00–03:00 local does not exist in New York.
     * Anchoring to it at a time of day inside that gap still files the entry on the date that was
     * picked, which is the only thing the composer promises about backdating.
     */
    @Test
    fun anchoringAcrossASpringForwardStillLandsOnTheChosenDate() {
        val springForward = LocalDate(2026, 3, 8)
        // 02:30 AM on Tuesday 10 March — an hour that exists that day, and not on the 8th.
        val halfPastTwo = 1_773_124_200_000L
        assertEquals("02:30 AM", formatTime(halfPastTwo, newYork))

        val stamped = resolveAnchor(DateAnchor.On(springForward), halfPastTwo, newYork)

        assertEquals(springForward, localDateAt(stamped, newYork))
        // Shifted forward by the length of the gap, because 02:30 is not a time that day.
        assertEquals("03:30 AM", formatTime(stamped, newYork))
    }

    @Test
    fun theFutureIsNotSomewhereAnEntryCanBeFiled() {
        assertTrue(canAnchorTo(LocalDate(2026, 10, 22), now, newYork))
        assertTrue(canAnchorTo(LocalDate(2026, 10, 21), now, newYork))
        assertFalse(canAnchorTo(LocalDate(2026, 10, 23), now, newYork))
    }

    /** Picking today in the picker leaves the Today chip lit, not a third state meaning the same. */
    @Test
    fun aPickedDateCollapsesOntoTheNarrowestAnchor() {
        assertEquals(DateAnchor.Today, anchorFor(LocalDate(2026, 10, 22), now, newYork))
        assertEquals(DateAnchor.Yesterday, anchorFor(LocalDate(2026, 10, 21), now, newYork))
        assertEquals(
            DateAnchor.On(LocalDate(2026, 10, 20)),
            anchorFor(LocalDate(2026, 10, 20), now, newYork),
        )
    }

    @Test
    fun aRowIsLabelledByTheDayItFallsOn() {
        assertEquals(DayBucket.Today, dayBucket(now, now, newYork))
        assertEquals(DayBucket.Yesterday, dayBucket(now - 86_400_000L, now, newYork))
        assertEquals(DayBucket.Other, dayBucket(now - 5 * 86_400_000L, now, newYork))

        assertEquals("Today", formatDayLabel(now, now, newYork))
        assertEquals("Yesterday", formatDayLabel(now - 86_400_000L, now, newYork))
        assertEquals(
            "Saturday, Oct 17, 2026",
            formatDayLabel(now - 5 * 86_400_000L, now, newYork),
        )
    }

    /** A clock that moved backwards leaves a future stamp behind. "Earlier" would not be true. */
    @Test
    fun aStampInTheFutureIsNotCalledEarlier() {
        assertEquals(DayBucket.Other, dayBucket(now + 5 * 86_400_000L, now, newYork))
    }

    @Test
    fun theDateCardPrintsTheDayAndTheTime() {
        assertEquals("Thursday, Oct 22, 2026 · 08:45 PM", formatStamp(now, newYork))
    }

    /**
     * The picker's `selectedDateMillis` is **UTC midnight**, not a local instant. Read in New York
     * it would be the previous day, which is the bug this conversion exists to prevent.
     */
    @Test
    fun aPickedMillisIsReadInUtcAndNotTheDevicesZone() {
        val utcMidnight = dateToPickerMillis(LocalDate(2026, 10, 22))

        assertEquals(LocalDate(2026, 10, 22), pickerMillisToDate(utcMidnight))
        assertEquals(LocalDate(2026, 10, 21), localDateAt(utcMidnight, newYork))
    }

    @Test
    fun thePickerRefusesTheSameDaysCanAnchorToDoes() {
        assertTrue(canPickMillis(dateToPickerMillis(LocalDate(2026, 10, 22)), now, newYork))
        assertFalse(canPickMillis(dateToPickerMillis(LocalDate(2026, 10, 23)), now, newYork))
    }

    // --- entriesOn: the archive's date filter ---------------------------------------------------

    private val minute = 60_000L

    /** Thursday 22 Oct, 11:59 PM New York — still today there, already Friday in Tokyo. */
    private val lateToday = now + (3 * 60 + 14) * minute

    /** Thursday 22 Oct, 12:01 AM New York. */
    private val earlyToday = now - (20 * 60 + 44) * minute

    /** Wednesday 21 Oct, 11:59 PM New York — two minutes before [earlyToday], a different day. */
    private val lateYesterday = now - (20 * 60 + 46) * minute

    private val older = resolveAnchor(DateAnchor.On(LocalDate(2026, 1, 3)), now, newYork)

    /** Newest first, as the DAO returns them. */
    private val archive = listOf(
        JournalEntry(id = 5, rawText = "late", timestamp = lateToday),
        JournalEntry(id = 4, rawText = "now", timestamp = now),
        JournalEntry(id = 3, rawText = "early", timestamp = earlyToday),
        JournalEntry(id = 2, rawText = "yesterday", timestamp = lateYesterday),
        JournalEntry(id = 1, rawText = "january", timestamp = older),
    )

    private fun ids(entries: List<JournalEntry>) = entries.map { it.id }

    @Test
    fun noFilterIsEverythingInOrder() {
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), ids(entriesOn(archive, null, now, newYork)))
    }

    @Test
    fun todayKeepsTheWholeDayAndNothingElse() {
        assertEquals(listOf(5L, 4L, 3L), ids(entriesOn(archive, DateAnchor.Today, now, newYork)))
    }

    /** 11:59 PM and 12:01 AM are two minutes apart and on two different days. */
    @Test
    fun midnightSplitsTheDays() {
        assertEquals(listOf(2L), ids(entriesOn(archive, DateAnchor.Yesterday, now, newYork)))
    }

    @Test
    fun aPickedDateKeepsThatDay() {
        val picked = DateAnchor.On(LocalDate(2026, 1, 3))
        assertEquals(listOf(1L), ids(entriesOn(archive, picked, now, newYork)))
    }

    @Test
    fun aDayWithNothingOnItIsEmpty() {
        val picked = DateAnchor.On(LocalDate(2026, 6, 1))
        assertTrue(entriesOn(archive, picked, now, newYork).isEmpty())
    }

    /**
     * The same entries, filtered in Tokyo, fall on different days — because they *are* on
     * different days there. In Tokyo it is already Friday 23rd.
     */
    @Test
    fun theFilterUsesTheZonesCalendar() {
        assertEquals(listOf(5L, 4L), ids(entriesOn(archive, DateAnchor.Today, now, tokyo)))
        assertEquals(listOf(3L, 2L), ids(entriesOn(archive, DateAnchor.Yesterday, now, tokyo)))
    }

    @Test
    fun theShortDayDropsTheWeekday() {
        assertEquals("Oct 22, 2026", formatShortDay(LocalDate(2026, 10, 22)))
        assertEquals("Jan 3, 2026", formatShortDay(LocalDate(2026, 1, 3)))
    }
}
