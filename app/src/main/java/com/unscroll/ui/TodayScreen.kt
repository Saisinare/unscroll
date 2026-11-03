package com.unscroll.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.block.formatDateTime
import com.unscroll.block.formatDuration
import com.unscroll.data.BlockStats
import com.unscroll.data.Config
import com.unscroll.data.InstalledApps
import com.unscroll.data.UsageTracker
import com.unscroll.protect.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private class AppUsage(val pkg: String, val label: String, val ms: Long)
private class TodayStats(val totalMs: Long, val unlocks: Int, val hourly: LongArray, val top: List<AppUsage>)

/** Last loaded stats, shown instantly when returning to the tab while fresh numbers load. */
@Volatile private var lastStats: TodayStats? = null

private fun loadToday(context: Context): TodayStats {
    val apps = InstalledApps.launchable(context).associateBy { it.pkg }
    val today = UsageTracker.today(context) { it in apps }
    val usage = today.perApp.mapNotNull { (pkg, ms) -> apps[pkg]?.let { AppUsage(pkg, it.label, ms) } }
        .filter { it.ms >= 60_000 }
        .sortedByDescending { it.ms }
    return TodayStats(usage.sumOf { it.ms }, today.unlocks, today.hourly, usage.take(10))
}

@Composable
fun TodayScreen(config: Config, onOpenStrict: () -> Unit) {
    val context = LocalContext.current
    // Read synchronously so the setup card is there on the first frame and doesn't push the header off-screen.
    var accessibility by remember { mutableStateOf(Permissions.accessibilityOn(context)) }
    var usageAccess by remember { mutableStateOf(Permissions.usageOn(context)) }
    var refresh by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf(lastStats) }
    val blocked by BlockStats.today.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        accessibility = Permissions.accessibilityOn(context)
        usageAccess = Permissions.usageOn(context)
        BlockStats.refresh()
        refresh++
        onPauseOrDispose { }
    }
    LaunchedEffect(refresh, usageAccess) {
        if (usageAccess) stats = withContext(Dispatchers.IO) { loadToday(context) }.also { lastStats = it }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            ScreenHeader("Today", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))) {
                CircleButton(Icons.Filled.Refresh, "Refresh") { refresh++ }
            }
        }

        if (!accessibility || !usageAccess) {
            item(key = "setup") { SetupCard(context, accessibility, usageAccess) }
        }

        item { HeroCard(stats, blocked) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Unlocks", stats?.unlocks?.toString() ?: "–", "times today", palette.sky, Modifier.weight(1f)) {
                    Icon(Icons.Filled.Lock, null, tint = palette.ink, modifier = Modifier.size(20.dp))
                }
                MetricTile("Blocked", blocked.toString(), "times saved", palette.mint, Modifier.weight(1f)) {
                    Icon(Icons.Filled.Close, null, tint = palette.ink, modifier = Modifier.size(20.dp))
                }
            }
        }

        item { StrictBanner(config, onOpenStrict) }

        stats?.takeIf { it.top.isNotEmpty() }?.let { s ->
            item { SectionHeader("Most used", "${s.top.size} apps") }
            items(s.top, key = { it.pkg }) { app ->
                AppUsageCard(app, config.apps.find { it.pkg == app.pkg }?.dailyLimitMin)
            }
        }
    }
}

@Composable
private fun HeroCard(stats: TodayStats?, blocked: Int) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Screen time", style = MaterialTheme.typography.titleLarge, color = palette.ink, modifier = Modifier.weight(1f))
            Text("$blocked blocked", style = MaterialTheme.typography.titleMedium, color = palette.good)
        }
        Text(stats?.let { formatDuration(it.totalMs) } ?: "–", style = MaterialTheme.typography.displayLarge, color = palette.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            LegendDot(palette.good, "Light")
            LegendDot(palette.warn, "Medium")
            LegendDot(palette.bad, "Heavy")
        }
        UsageWave(stats?.hourly ?: LongArray(24))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM").forEach { Hint(it) }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Hint(label)
    }
}

@Composable
private fun MetricTile(title: String, value: String, caption: String, tint: Color, modifier: Modifier, icon: @Composable () -> Unit) {
    SoftCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink, modifier = Modifier.weight(1f))
            IconTile(tint, 36.dp) { icon() }
        }
        Spacer(Modifier.height(18.dp))
        Text(value, style = MaterialTheme.typography.displaySmall, color = palette.ink)
        Hint(caption)
    }
}

@Composable
private fun StrictBanner(config: Config, onOpen: () -> Unit) {
    val on = config.strictActive()
    SoftCard(color = if (on) palette.inverse else palette.card, padding = 18.dp, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (on) palette.onInverse.copy(alpha = 0.12f) else palette.lilac, 40.dp) {
                Icon(Icons.Filled.Lock, null, tint = if (on) palette.onInverse else palette.ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (on) "Strict mode on" else "Strict mode off",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (on) palette.onInverse else palette.ink,
                )
                Hint(
                    if (on) "Locked until ${formatDateTime(config.strictUntil)}" else "Lock your limits so you can't undo them",
                    color = if (on) palette.onInverse.copy(alpha = 0.6f) else palette.muted,
                )
            }
            if (!on) Tag("Set up", palette.inverse, palette.onInverse)
        }
    }
}

@Composable
private fun AppUsageCard(app: AppUsage, limitMin: Int?) {
    SoftCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.pkg)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Hint(formatDuration(app.ms) + (limitMin?.let { " of ${it}m" } ?: " today"))
            }
            if (limitMin != null) {
                val left = limitMin * 60_000L - app.ms
                if (left > 0) Tag("${formatDuration(left)} left", palette.inverse, palette.onInverse)
                else Tag("Limit hit", palette.bad.copy(alpha = 0.15f), palette.bad)
            }
        }
        if (limitMin != null) {
            val f = app.ms / (limitMin * 60_000f)
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                GradientBar(f, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Text("${(f * 100).toInt().coerceAtMost(999)}%", style = MaterialTheme.typography.titleMedium, color = palette.ink)
            }
        }
    }
}

@Composable
private fun SetupCard(context: Context, accessibility: Boolean, usageAccess: Boolean) {
    SoftCard(color = palette.inverse) {
        Text("Finish setup", style = MaterialTheme.typography.titleLarge, color = palette.onInverse)
        Hint("Two permissions and Unscroll is ready to block.", color = palette.onInverse.copy(alpha = 0.6f))
        Spacer(Modifier.height(14.dp))
        SetupStep(1, "Accessibility service", accessibility) { Permissions.openAccessibility(context) }
        SetupStep(2, "Usage access", usageAccess) { Permissions.openUsageAccess(context) }
        SetupStep(3, "Battery: don't optimise", false, optional = true) { Permissions.openBatterySettings(context) }
        if (!accessibility) {
            Hint(
                "Switch greyed out? App info → ⋮ → Allow restricted settings, then try again.",
                color = palette.onInverse.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SetupStep(n: Int, title: String, done: Boolean, optional: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(PillShape).clickable(enabled = !done, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (done) palette.good else palette.onInverse.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            else Text("$n", color = palette.onInverse, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = palette.onInverse, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (!done) Tag(if (optional) "Open" else "Allow", palette.onInverse, palette.inverse)
    }
}
