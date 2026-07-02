// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the Play Store install detection and listing links behind the About screen's
 * "leave a review" entry (ENGINEERING_DECISIONS.md ED-31).
 */
class InstallSourceTest {

    @Test
    fun TDD_playStoreInstallerShowsTheReviewEntry() {
        // ED-31: the review entry is shown iff the installing package is the Play Store's.
        assertTrue(isInstalledFromPlayStore("com.android.vending"))
    }

    @Test
    fun TDD_sideloadsAndUnknownInstallersFailClosed() {
        // ED-31: a null installer (sideload, debug build) or a lookup failure hides the entry.
        assertFalse(isInstalledFromPlayStore(null))
    }

    @Test
    fun TDD_otherStoresDoNotShowThePlayReviewEntry() {
        // ED-31: only the Play Store counts — other stores would make the link a dead end.
        assertFalse(isInstalledFromPlayStore("com.amazon.venezia"))
        assertFalse(isInstalledFromPlayStore("org.fdroid.fdroid"))
        assertFalse(isInstalledFromPlayStore(""))
        // Exact match only: near-miss package names must not pass.
        assertFalse(isInstalledFromPlayStore("com.android.vending.evil"))
    }

    @Test
    fun TDD_playListingLinksCarryTheApplicationId() {
        // ED-31: the review entry deep-links to the app's own listing (market:// pinned to the
        // Play Store app), with the public web listing as the fallback.
        assertEquals(
            "market://details?id=com.nudgery.android",
            playStoreListingUri("com.nudgery.android")
        )
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.nudgery.android",
            playStoreListingWebUrl("com.nudgery.android")
        )
    }
}
