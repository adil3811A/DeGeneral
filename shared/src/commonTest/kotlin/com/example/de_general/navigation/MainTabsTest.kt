package com.example.de_general.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bottom bar renders [MainTab.entries] in declaration order and nothing re-sorts them, so the
 * declaration order *is* the design. These pin it, along with the routes behind each tab — a tab
 * pointing at the wrong destination is the kind of mistake that looks fine until it is shipped.
 *
 * Which tab is *selected* is not tested here: it is resolved against a live `NavDestination`, and
 * a fake one would be testing the fake.
 */
class MainTabsTest {

    @Test
    fun theBarHasThreeTabsInTheDesignsOrder() {
        assertEquals(
            listOf(MainTab.Chat, MainTab.Journal, MainTab.Settings),
            MainTab.entries.toList(),
        )
    }

    @Test
    fun journalSitsInTheMiddle() {
        // It is the main graph's start destination, and the design puts it under the thumb.
        assertEquals(MainTab.Journal, MainTab.entries[MainTab.entries.size / 2])
    }

    @Test
    fun eachTabPointsAtItsOwnDestination() {
        assertEquals(Chat, MainTab.Chat.route)
        assertEquals(Journal, MainTab.Journal.route)
        assertEquals(Settings, MainTab.Settings.route)
    }

    /**
     * The testable half of "the bar hides itself over the composer".
     *
     * [CreateJournal] is a flat sibling of the three tabs rather than a child of [Journal], so
     * `selectedTab` finds no match, returns null and `MainNavBar` fades out. The walk itself needs
     * a live `NavDestination` and is not tested here — what is pinned is the premise it rests on:
     * no tab points at the composer.
     */
    @Test
    fun createJournalIsNotATab() {
        assertTrue(MainTab.entries.none { it.route == CreateJournal })
    }

    @Test
    fun everyTabHasALabelToShowUnderItsIcon() {
        val labels = MainTab.entries.map { it.label }

        assertTrue(labels.none { it.isBlank() })
        assertEquals(labels.size, labels.distinct().size)
    }
}
