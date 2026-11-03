package com.unscroll.data

import android.content.Context
import android.content.Intent
import android.os.SystemClock

data class InstalledApp(val pkg: String, val label: String)

object InstalledApps {
    private const val CACHE_MS = 60_000L

    @Volatile private var cached: List<InstalledApp>? = null
    @Volatile private var cachedAt = 0L

    /** The last loaded list, even if stale, so screens can draw immediately. */
    fun peek(): List<InstalledApp>? = cached

    /** Launchable apps sorted by name. Loading labels is slow, so results are reused for a minute. */
    fun launchable(context: Context): List<InstalledApp> {
        cached?.takeIf { SystemClock.elapsedRealtime() - cachedAt < CACHE_MS }?.let { return it }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != context.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
            .also {
                cached = it
                cachedAt = SystemClock.elapsedRealtime()
            }
    }
}
