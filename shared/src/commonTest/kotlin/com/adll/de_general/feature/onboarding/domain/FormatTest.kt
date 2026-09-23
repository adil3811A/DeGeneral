package com.adll.de_general.feature.onboarding.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {

    @Test
    fun bytesUseDecimalUnitsSoTheyMatchEveryOtherFigureTheUserSees() {
        // The model file as Hugging Face reports it.
        assertEquals("806 MB", formatBytes(806_058_240L))
        assertEquals("1.0 GB", formatBytes(1_000_000_000L))
        assertEquals("8.0 GB", formatBytes(8_000_000_000L))
        assertEquals("512 B", formatBytes(512L))
        assertEquals("2 kB", formatBytes(2_048L))
    }

    @Test
    fun speedsRoundToSomethingReadable() {
        assertEquals("—", formatSpeed(0.0))
        assertEquals("12.4 MB/s", formatSpeed(12_400_000.0))
        assertEquals("850 kB/s", formatSpeed(850_000.0))
    }

    @Test
    fun remainingTimeIsDeliberatelyCoarse() {
        assertEquals("a few seconds", formatRemaining(3))
        assertEquals("~35s", formatRemaining(37))
        assertEquals("~2 min", formatRemaining(100))
        assertEquals("~1h 10m", formatRemaining(4_200))
        assertEquals("—", formatRemaining(-1))
    }

    @Test
    fun progressCannotProduceNaNOrRunPastTheEnd() {
        assertEquals(0f, DownloadProgress(0, 0, 0.0).fraction)
        assertEquals(1f, DownloadProgress(200, 100, 0.0).fraction)
        assertEquals(50, DownloadProgress(50, 100, 0.0).percent)
    }

    @Test
    fun etaNeedsBothARateAndAKnownTotal() {
        assertEquals(null, DownloadProgress(10, 100, 0.0).secondsRemaining)
        assertEquals(null, DownloadProgress(10, 0, 5.0).secondsRemaining)
        assertEquals(18L, DownloadProgress(10, 100, 5.0).secondsRemaining)
        assertEquals(0L, DownloadProgress(100, 100, 5.0).secondsRemaining)
    }
}
