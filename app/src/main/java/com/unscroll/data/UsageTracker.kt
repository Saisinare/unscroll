package com.unscroll.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

object UsageTracker {
    private const val HOUR_MS = 60 * 60 * 1000L

    class Today(val perApp: Map<String, Long>, val unlocks: Int, val hourly: LongArray)

    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun startOfToday(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * Foreground time per package since midnight, unlock count, and screen time per hour of the day.
     * [countInHourly] picks which packages count towards the hourly totals (e.g. skip the launcher).
     */
    fun today(context: Context, countInHourly: (String) -> Boolean = { true }): Today {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val dayStart = startOfToday()
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(dayStart, now)
        val totals = HashMap<String, Long>()
        val openSince = HashMap<String, Long>()
        val hourly = LongArray(24)
        var unlocks = 0

        fun close(pkg: String, from: Long, to: Long) {
            totals.merge(pkg, to - from, Long::plus)
            if (!countInHourly(pkg)) return
            var s = maxOf(from, dayStart)
            while (s < to) {
                val h = ((s - dayStart) / HOUR_MS).toInt().coerceIn(0, 23)
                val e = minOf(to, dayStart + (h + 1) * HOUR_MS).coerceAtLeast(s + 1)
                hourly[h] += e - s
                s = e
            }
        }

        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openSince[e.packageName] = e.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> openSince.remove(e.packageName)?.let { close(e.packageName, it, e.timeStamp) }
                else -> if (Build.VERSION.SDK_INT >= 28 && e.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) unlocks++
            }
        }
        openSince.forEach { (pkg, since) -> close(pkg, since, now) }
        return Today(totals, unlocks, hourly)
    }
}
