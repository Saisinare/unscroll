package com.unscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unscroll.block.formatDateTime
import com.unscroll.block.formatMinutes
import com.unscroll.data.AppRule
import com.unscroll.data.Config
import com.unscroll.data.InstalledApp
import com.unscroll.data.InstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val DAY_MS = 24 * 60 * 60 * 1000L

private fun AppRule.blockedNow(): Boolean =
    blockedUntil > System.currentTimeMillis() || LocalDate.now().dayOfWeek.value in blockedWeekdays

@Composable
fun AppsScreen(config: Config, ui: UiActions) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(InstalledApps.peek()) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<InstalledApp?>(null) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { InstalledApps.launchable(context) } }

    val rules = config.apps.associateBy { it.pkg }
    val limited = config.apps.count { it.dailyLimitMin != null }
    val blocked = config.apps.count { it.blockedNow() }

    Column(Modifier.padding(horizontal = 16.dp)) {
        ScreenHeader("Apps", "${config.apps.size} with rules")
        SearchField(query, { query = it }, "Search apps")
        SegmentedPills(
            listOf("All", "Limited · $limited", "Blocked · $blocked"),
            filter, { filter = it },
            Modifier.padding(vertical = 12.dp),
        )
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = palette.ink) }
        } else {
            val shown = remember(list, query, filter, rules) {
                list
                    .filter { it.label.contains(query, ignoreCase = true) }
                    .filter {
                        when (filter) {
                            1 -> rules[it.pkg]?.dailyLimitMin != null
                            2 -> rules[it.pkg]?.blockedNow() == true
                            else -> true
                        }
                    }
                    .sortedByDescending { it.pkg in rules }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.pkg }) { app -> AppRow(app, rules[app.pkg]) { editing = app } }
            }
        }
    }

    editing?.let { app ->
        RuleSheet(app, rules[app.pkg], onDismiss = { editing = null }) { rule ->
            ui.update { c -> c.copy(apps = c.apps.filter { it.pkg != app.pkg } + listOfNotNull(rule.takeUnless { it.isEmpty() })) }
            editing = null
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, rule: AppRule?, onClick: () -> Unit) {
    SoftCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.pkg)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Hint(rule?.let(::describe)?.ifEmpty { null } ?: "No limits")
            }
            when {
                rule?.blockedNow() == true -> Tag("Blocked", palette.bad.copy(alpha = 0.15f), palette.bad)
                rule?.dailyLimitMin != null -> Tag("${formatMinutes(rule.dailyLimitMin)}/day", palette.inverse, palette.onInverse)
                else -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.muted)
            }
        }
    }
}

private fun describe(rule: AppRule): String = listOfNotNull(
    rule.blockedUntil.takeIf { it > System.currentTimeMillis() }?.let { "Until ${formatDateTime(it)}" },
    rule.blockedWeekdays.takeIf { it.isNotEmpty() }?.let { days ->
        "Every " + days.sorted().joinToString { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    },
    rule.dailyLimitMin?.let { "${formatMinutes(it)} a day" },
).joinToString(" · ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleSheet(app: InstalledApp, existing: AppRule?, onDismiss: () -> Unit, onSave: (AppRule) -> Unit) {
    val now = System.currentTimeMillis()
    val currentUntil = existing?.blockedUntil?.takeIf { it > now } ?: 0L
    var limit by remember { mutableFloatStateOf((existing?.dailyLimitMin ?: 0).toFloat()) }
    var weekdays by remember { mutableStateOf(existing?.blockedWeekdays ?: emptySet()) }
    // Index into blockOptions; the extra "Unblock" option appears only while a block is running.
    val blockOptions = listOf(0, 1, 3, 7, 14, 30) + if (currentUntil > 0) listOf(-1) else emptyList()
    var blockChoice by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.bg,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.pkg, 52.dp)
                Spacer(Modifier.width(14.dp))
                Text(app.label, style = MaterialTheme.typography.headlineLarge, color = palette.ink)
            }

            SoftCard {
                Text("Daily limit", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Text(
                    if (limit == 0f) "Off" else formatMinutes(limit.toInt()),
                    style = MaterialTheme.typography.displaySmall,
                    color = palette.ink,
                )
                Slider(
                    value = limit,
                    onValueChange = { limit = (it / 5).roundToInt() * 5f },
                    valueRange = 0f..240f,
                    colors = SliderDefaults.colors(thumbColor = palette.ink, activeTrackColor = palette.ink, inactiveTrackColor = palette.line),
                )
                Segmented(
                    listOf("Off", "15m", "30m", "1h", "2h"),
                    selected = listOf(0, 15, 30, 60, 120).indexOf(limit.toInt()),
                    onSelect = { limit = listOf(0, 15, 30, 60, 120)[it].toFloat() },
                )
            }

            SoftCard {
                Text("Block completely", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Hint(if (currentUntil > 0) "Blocked until ${formatDateTime(currentUntil)}" else "Not blocked right now")
                Spacer(Modifier.height(10.dp))
                SegmentedPills(
                    blockOptions.map {
                        when (it) {
                            0 -> if (currentUntil > 0) "Keep" else "Off"
                            -1 -> "Unblock"
                            1 -> "1 day"
                            else -> "$it days"
                        }
                    },
                    blockChoice, { blockChoice = it },
                )
            }

            SoftCard {
                Text("Block every week on", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.entries.forEach { day ->
                        val on = day.value in weekdays
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(if (on) palette.inverse else palette.cardAlt)
                                .clickable { weekdays = if (on) weekdays - day.value else weekdays + day.value },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) palette.onInverse else palette.ink,
                            )
                        }
                    }
                }
            }

            PillButton("Save", onClick = {
                val days = blockOptions[blockChoice]
                val until = when {
                    days == -1 -> 0L
                    days > 0 -> maxOf(currentUntil, System.currentTimeMillis() + days * DAY_MS)
                    else -> currentUntil
                }
                onSave(AppRule(app.pkg, app.label, limit.toInt().takeIf { it > 0 }, weekdays, until))
            }, Modifier.fillMaxWidth().padding(bottom = 16.dp))
        }
    }
}
