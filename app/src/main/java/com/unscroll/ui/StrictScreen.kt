package com.unscroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.unscroll.block.formatDateTime
import com.unscroll.data.Config
import com.unscroll.protect.Permissions

private const val DAY_MS = 24 * 60 * 60 * 1000L
private val DAY_OPTIONS = listOf(1, 3, 7, 14, 30)

private fun remaining(ms: Long): String {
    val h = (ms / 3_600_000).coerceAtLeast(0)
    return if (h >= 24) "${h / 24}d ${h % 24}h" else "${h}h ${(ms / 60_000) % 60}m"
}

@Composable
fun StrictScreen(config: Config, ui: UiActions) {
    val context = LocalContext.current
    var adminOn by remember { mutableStateOf(false) }
    var accessibilityOn by remember { mutableStateOf(false) }
    var dayIndex by remember { mutableIntStateOf(0) }
    var confirming by remember { mutableStateOf(false) }
    val strict = config.strictActive()

    LifecycleResumeEffect(Unit) {
        adminOn = Permissions.adminOn(context)
        accessibilityOn = Permissions.accessibilityOn(context)
        onPauseOrDispose { }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("Strict mode", "Lock your limits in")

        if (strict) ActiveCard(config, ui) else SetupCard(dayIndex, { dayIndex = it }) {
            if (accessibilityOn) confirming = true else ui.message("Turn on the accessibility service first (Today tab).")
        }

        SectionHeader("Protection")
        ToggleCard(
            "Prevent uninstall",
            "Blocks uninstalling, Force stop and switching off the service. Always on in strict mode.",
            config.uninstallProtection,
            enabled = !(strict && config.uninstallProtection),
            leading = { IconTile(palette.lilac) { Icon(Icons.Filled.Lock, null, tint = palette.ink, modifier = Modifier.size(20.dp)) } },
        ) { v ->
            if (v && !accessibilityOn) ui.message("Turn on the accessibility service first (Today tab).")
            else ui.update { it.copy(uninstallProtection = v) }
        }
        SoftCard(padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(palette.sky) { Icon(Icons.Filled.Settings, null, tint = palette.ink, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Device admin", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                    Hint(if (adminOn) "Android itself refuses to uninstall Unscroll" else "A second lock against uninstalling")
                }
                Spacer(Modifier.width(10.dp))
                if (adminOn) Tag("Active", palette.good.copy(alpha = 0.18f), palette.good)
                else PillButton("Activate", { Permissions.requestAdmin(context) }, height = 40.dp)
            }
        }
        Hint(
            "Safe mode, ADB from a computer or a factory reset can still remove any app. Unscroll can't stop those.",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
    }

    if (confirming) {
        val until = System.currentTimeMillis() + DAY_OPTIONS[dayIndex] * DAY_MS
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = palette.card,
            title = { Text("Lock everything?") },
            text = { Text("Your limits stay locked until ${formatDateTime(until)}. There's no way to end it early.") },
            confirmButton = {
                TextButton(onClick = {
                    ui.update { it.copy(strictUntil = until, strictStartedAt = System.currentTimeMillis()) }
                    confirming = false
                }) { Text("Lock it", color = palette.ink) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel", color = palette.muted) } },
        )
    }
}

@Composable
private fun ActiveCard(config: Config, ui: UiActions) {
    val now = System.currentTimeMillis()
    val total = (config.strictUntil - config.strictStartedAt).takeIf { config.strictStartedAt > 0 && it > 0 }
    val left = config.strictUntil - now
    val fraction = total?.let { left.toFloat() / it } ?: 1f
    val fg = palette.onInverse

    SoftCard(color = palette.inverse) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Hint("Locked for", color = fg.copy(alpha = 0.6f))
                Text(remaining(left), style = MaterialTheme.typography.displaySmall, color = fg)
                Hint("until ${formatDateTime(config.strictUntil)}", color = fg.copy(alpha = 0.6f))
            }
            Ring(fraction, 96.dp, palette.good, fg.copy(alpha = 0.14f)) {
                Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = fg)
            }
        }
        Spacer(Modifier.height(18.dp))
        Hint("Add more time", color = fg.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 3, 7).forEach { d ->
                PillButton("+$d day${if (d > 1) "s" else ""}", { ui.update { it.copy(strictUntil = it.strictUntil + d * DAY_MS) } },
                    light = true, height = 40.dp)
            }
        }
    }
}

@Composable
private fun SetupCard(dayIndex: Int, onDay: (Int) -> Unit, onStart: () -> Unit) {
    SoftCard {
        Text("Lock for", style = MaterialTheme.typography.titleMedium, color = palette.ink)
        Row(verticalAlignment = Alignment.Bottom) {
            val d = DAY_OPTIONS[dayIndex]
            Text("$d", style = MaterialTheme.typography.displayLarge, color = palette.ink)
            Text(if (d == 1) " day" else " days", style = MaterialTheme.typography.titleLarge, color = palette.muted,
                modifier = Modifier.padding(bottom = 10.dp))
        }
        Segmented(DAY_OPTIONS.map { "${it}d" }, dayIndex, onDay)
        Spacer(Modifier.height(16.dp))
        listOf(
            "Limits can't be removed or loosened",
            "Content & site blocking stay on",
            "No uninstall, force stop or clock changes",
        ).forEach { line ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, null, tint = palette.good, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(line, style = MaterialTheme.typography.bodyMedium, color = palette.ink)
            }
        }
        Spacer(Modifier.height(16.dp))
        PillButton("Start strict mode", onStart, Modifier.fillMaxWidth())
    }
}
