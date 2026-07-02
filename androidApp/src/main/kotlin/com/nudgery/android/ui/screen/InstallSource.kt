// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

private const val TAG = "InstallSource"

/** The Play Store app's package name, as reported by the installer lookup for Play installs. */
private const val PLAY_STORE_PACKAGE = "com.android.vending"

/**
 * ED-31: the About screen's "leave a review" entry is shown iff the app was installed by the Play
 * Store. Exact match, failing closed — a null installer (sideload, debug build, lookup error) or
 * any other store hides the entry rather than offering a dead-end link.
 */
internal fun isInstalledFromPlayStore(installerPackageName: String?): Boolean =
    installerPackageName == PLAY_STORE_PACKAGE

/**
 * The package that installed this app, or null when unknown (sideload, debug install, or lookup
 * failure). Uses the modern install-source API on API 30+ and the deprecated single-method lookup
 * below that (minSdk 26).
 */
internal fun installerPackageName(context: Context): String? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }
} catch (e: Exception) {
    // A failed lookup (e.g. our own package name not found — never expected) just means we can't
    // prove a Play install, so the review entry stays hidden (ED-31 fails closed).
    Log.w(TAG, "Install source lookup failed; treating install source as unknown", e)
    null
}

/** Deep link to this app's Play Store listing, handled by the Play Store app. */
internal fun playStoreListingUri(applicationId: String): String =
    "market://details?id=$applicationId"

/** The public web URL of this app's Play Store listing — the fallback when the app link fails. */
internal fun playStoreListingWebUrl(applicationId: String): String =
    "https://play.google.com/store/apps/details?id=$applicationId"

/**
 * Opens this app's Play Store listing so the user can leave a review (ED-31): first the market://
 * deep link pinned to the Play Store app, then the web listing if that can't resolve (e.g. the
 * store app was disabled since the install-source check).
 */
internal fun openPlayStoreListing(context: Context, applicationId: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(playStoreListingUri(applicationId)))
                .setPackage(PLAY_STORE_PACKAGE)
        )
        Log.i(TAG, "Opened Play Store listing for review")
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "Play Store app unavailable; falling back to web listing", e)
        openUrl(context, playStoreListingWebUrl(applicationId))
    }
}

/** Opens [url] in the user's browser; logs instead of crashing if no browser exists. */
internal fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        Log.i(TAG, "Opened external link: $url")
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No activity can open $url", e)
    }
}
