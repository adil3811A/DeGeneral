package com.example.de_general.feature.journal.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * When an entry happened, and how that reads.
 *
 * Every function here takes both the [TimeZone] and the current instant explicitly, and neither has
 * a default. That is the whole design: a test can pin "yesterday" without waiting for midnight, and
 * can prove that one instant is two different days in two different zones. A hidden
 * `Clock.System.now()` or `TimeZone.currentSystemDefault()` would make both untestable and would
 * put a wall clock in common code, which `AppContainer` already refuses to do.
 *
 * No Compose, no Room. `commonTest` covers it on every target.
 */

/**
 * English day and month names, spelled out rather than taken from a locale-aware formatter.
 *
 * The design specifies a fixed string ("Thursday, Oct 22, 2026"). A locale-aware format would
 * silently stop matching it on the first phone set to another language, and the result would be a
 * screen nobody here can reproduce. When this app is localised, that is a deliberate piece of work
 * with translated strings beside it, not a formatter quietly changing its mind.
 */
private val DayFormat = LocalDate.Format {
    dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
    chars(", ")
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day()
    chars(", ")
    year()
}

/** "08:45 PM". Zero-padded, so a column of times does not jitter. */
private val TimeFormat = LocalTime.Format {
    amPmHour()
    char(':')
    minute()
    char(' ')
    amPmMarker("AM", "PM")
}

/** Which day an entry is filed under, as far as the composer is concerned. */
sealed interface DateAnchor {

    /** Now. The default, and the only anchor most entries will ever have. */
    data object Today : DateAnchor

    /** The evening someone did not get round to writing. */
    data object Yesterday : DateAnchor

    /** Anything else, from the date picker. */
    data class On(val date: LocalDate) : DateAnchor
}

/** How a stored timestamp is labelled in a list. */
enum class DayBucket {
    Today,
    Yesterday,

    /**
     * Neither — print the date. Covers a timestamp in the future as well as one in the past: the
     * app cannot create one, but a device whose clock moved backwards can leave one behind, and
     * "Earlier" would be a claim about it that is not true.
     */
    Other,
}

/** The calendar date [millis] falls on in [zone]. */
fun localDateAt(millis: Long, zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date

/** Which date [anchor] means, given what day it is in [zone]. */
fun anchorDate(anchor: DateAnchor, nowMillis: Long, zone: TimeZone): LocalDate =
    when (anchor) {
        DateAnchor.Today -> localDateAt(nowMillis, zone)
        DateAnchor.Yesterday -> localDateAt(nowMillis, zone).minus(DatePeriod(days = 1))
        is DateAnchor.On -> anchor.date
    }

/**
 * The instant to stamp an entry with: the anchored **date**, at the **time of day it is now**.
 *
 * Never midnight, and the three reasons are all things someone would otherwise hit on a device:
 *
 *  - `observeAll()` is `ORDER BY timestamp DESC` with no tiebreak, so two entries backdated to the
 *    same day would collide at 00:00 and SQLite could return them in either order, differently
 *    between reads.
 *  - The screen prints the time, and 00:00 renders "12:00 AM" — a time nobody chose, presented as
 *    if they had.
 *  - It makes `resolveAnchor(On(today))` exactly equal to [nowMillis], so the picker and the Today
 *    chip cannot disagree about what "today" means.
 *
 * On a spring-forward day the chosen local time may not exist; `toInstant` resolves it forward by
 * the size of the gap, which keeps the entry on the date the person picked.
 */
fun resolveAnchor(anchor: DateAnchor, nowMillis: Long, zone: TimeZone): Long {
    val timeOfDay = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).time
    return LocalDateTime(anchorDate(anchor, nowMillis, zone), timeOfDay)
        .toInstant(zone)
        .toEpochMilliseconds()
}

/** Backdating only. There is no honest reason to file an entry on a day that has not happened. */
fun canAnchorTo(date: LocalDate, nowMillis: Long, zone: TimeZone): Boolean =
    date <= localDateAt(nowMillis, zone)

/**
 * [date] as the narrowest anchor that describes it.
 *
 * Picking today's date in the picker leaves the Today chip selected rather than lighting up a third
 * "custom" state that means the same thing.
 */
fun anchorFor(date: LocalDate, nowMillis: Long, zone: TimeZone): DateAnchor {
    val today = localDateAt(nowMillis, zone)
    return when (date) {
        today -> DateAnchor.Today
        today.minus(DatePeriod(days = 1)) -> DateAnchor.Yesterday
        else -> DateAnchor.On(date)
    }
}

/** Which bucket [millis] falls into, relative to [nowMillis], in [zone]. */
fun dayBucket(millis: Long, nowMillis: Long, zone: TimeZone): DayBucket {
    val day = localDateAt(millis, zone)
    val today = localDateAt(nowMillis, zone)
    return when (day) {
        today -> DayBucket.Today
        today.minus(DatePeriod(days = 1)) -> DayBucket.Yesterday
        else -> DayBucket.Other
    }
}

/** "Thursday, Oct 22, 2026". */
fun formatDay(millis: Long, zone: TimeZone): String =
    DayFormat.format(localDateAt(millis, zone))

/** "Thursday, Oct 22, 2026", for a date that has no instant yet. */
fun formatDay(date: LocalDate): String = DayFormat.format(date)

/** "08:45 PM". */
fun formatTime(millis: Long, zone: TimeZone): String =
    TimeFormat.format(Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).time)

/** "Today", "Yesterday", or the full date. What a list row says. */
fun formatDayLabel(millis: Long, nowMillis: Long, zone: TimeZone): String =
    when (dayBucket(millis, nowMillis, zone)) {
        DayBucket.Today -> "Today"
        DayBucket.Yesterday -> "Yesterday"
        DayBucket.Other -> formatDay(millis, zone)
    }

/** "Thursday, Oct 22, 2026 · 08:45 PM". The date card's headline. */
fun formatStamp(millis: Long, zone: TimeZone): String =
    "${formatDay(millis, zone)} · ${formatTime(millis, zone)}"

/**
 * A Material `DatePickerState.selectedDateMillis` as a calendar date.
 *
 * That value is **UTC midnight of the chosen day**, not a local instant. Reading it in the device's
 * zone would land on the previous day for anyone west of Greenwich — so it is read in
 * [TimeZone.UTC] and only the date is kept.
 */
fun pickerMillisToDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

/** The inverse of [pickerMillisToDate], for seeding the picker with the current anchor. */
fun dateToPickerMillis(date: LocalDate): Long =
    LocalDateTime(date, LocalTime(0, 0)).toInstant(TimeZone.UTC).toEpochMilliseconds()

/**
 * Whether the picker should let [pickerMillis] be chosen — the picker's own UTC-midnight
 * convention, converted once here so the screen never does that arithmetic itself.
 */
fun canPickMillis(pickerMillis: Long, nowMillis: Long, zone: TimeZone): Boolean =
    canAnchorTo(pickerMillisToDate(pickerMillis), nowMillis, zone)
