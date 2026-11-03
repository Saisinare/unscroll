package com.unscroll.protect

import android.view.accessibility.AccessibilityNodeInfo
import com.unscroll.block.visibleTexts

/**
 * Recognises system screens that could switch Unscroll off: its App info page (Uninstall, Force stop,
 * Clear storage), the uninstall dialog, device-admin deactivation, its accessibility toggle and usage
 * access. In strict mode it also blocks the date & time settings, so the clock can't be moved forward.
 */
object TamperGuard {
    private val SYSTEM_PACKAGE_HINTS = listOf("settings", "packageinstaller", "permissioncontroller", "securitycenter", "safecenter")

    private val DANGER_BUTTONS = setOf(
        "uninstall", "force stop", "deactivate", "deactivate this device admin app", "deactivate & uninstall",
        "clear storage", "clear data", "disable", "turn off", "permit usage access", "allow usage tracking",
    )

    private val CLOCK_SETTINGS = listOf(
        "set time automatically", "automatic date & time", "automatic date and time", "use network-provided time", "set date",
    )

    fun isSystemPackage(pkg: String) = SYSTEM_PACKAGE_HINTS.any { it in pkg }

    fun isTamperScreen(pkg: String, root: AccessibilityNodeInfo, appLabel: String, strict: Boolean): Boolean {
        if (!isSystemPackage(pkg)) return false
        val texts = root.visibleTexts(300).map { it.text.trim().lowercase() }
        if (strict && texts.any { t -> CLOCK_SETTINGS.any { t.startsWith(it) } }) return true

        val label = appLabel.lowercase()
        if (texts.none { label in it }) return false
        if ("packageinstaller" in pkg) return true
        return texts.any { it in DANGER_BUTTONS || it.startsWith("use $label") || it.startsWith("stop $label") }
    }
}
