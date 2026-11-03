package com.unscroll.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.unscroll.data.Config
import com.unscroll.data.ConfigStore
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos

val CardShape = RoundedCornerShape(28.dp)
val TileShape = RoundedCornerShape(14.dp)
val PillShape = RoundedCornerShape(50)

/** Applies config changes and tells the user when strict mode refused one. */
class UiActions(private val snackbar: SnackbarHostState, private val scope: CoroutineScope) {
    fun update(change: (Config) -> Config) {
        if (!ConfigStore.update(change)) message("Strict mode is on. You can add restrictions, but not remove or loosen them.")
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = palette.ink)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.muted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = palette.ink, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.muted) }
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = palette.muted) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = modifier)
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = palette.card,
    padding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(modifier.fillMaxWidth().clip(CardShape).background(color).then(clickable).padding(padding), content = content)
}

@Composable
fun CircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(palette.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = palette.ink, modifier = Modifier.size(22.dp)) }
}

/** Primary action: a black pill (white in dark mode). [light] gives the white variant used on dark cards. */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, light: Boolean = false, height: Dp = 52.dp) {
    val bg = if (light) palette.onInverse else palette.inverse
    val fg = if (light) palette.inverse else palette.onInverse
    Box(
        modifier.height(height).clip(PillShape).background(bg).clickable(onClick = onClick).padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = fg) }
}

@Composable
fun Tag(text: String, bg: Color, fg: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier.clip(PillShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun IconTile(bg: Color, size: Dp = 44.dp, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.size(size).clip(TileShape).background(bg), contentAlignment = Alignment.Center, content = content)
}

/** The app's real launcher icon, or a coloured tile with [glyph] if it isn't installed. */
private val iconCache = LruCache<String, ImageBitmap>(256)
private val missingIcons = ConcurrentHashMap.newKeySet<String>()

@Composable
fun AppIcon(pkg: String, size: Dp = 44.dp, fallback: Color = palette.cardAlt, glyph: String = "•") {
    val context = LocalContext.current
    // Icons decode off the main thread and are cached, so scrolling long app lists stays smooth.
    var bitmap by remember(pkg) { mutableStateOf<ImageBitmap?>(iconCache.get(pkg)) }
    LaunchedEffect(pkg) {
        if (bitmap == null && pkg !in missingIcons) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap() }
                    .onFailure { missingIcons.add(pkg) }
                    .getOrNull()
                    ?.also { iconCache.put(pkg, it) }
            }
        }
    }
    when {
        bitmap != null -> Image(bitmap!!, null, Modifier.size(size).clip(TileShape))
        pkg in missingIcons -> IconTile(fallback, size) { Text(glyph, color = palette.ink, style = MaterialTheme.typography.titleMedium) }
        else -> IconTile(palette.cardAlt, size) {}
    }
}

@Composable
fun UnscrollSwitch(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = palette.inverse, checkedThumbColor = palette.onInverse, checkedBorderColor = palette.inverse,
            uncheckedTrackColor = palette.cardAlt, uncheckedThumbColor = palette.muted, uncheckedBorderColor = palette.line,
        ),
    )
}

@Composable
fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    leading: @Composable () -> Unit,
    onChange: (Boolean) -> Unit,
) {
    SoftCard(padding = 16.dp, onClick = { if (enabled) onChange(!checked) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Hint(subtitle)
            }
            Spacer(Modifier.width(10.dp))
            UnscrollSwitch(checked, enabled, onChange)
        }
    }
}

/** Equal-width segmented control on a tinted track, for a handful of short options. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(PillShape).background(palette.cardAlt).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(PillShape).background(if (on) palette.inverse else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) palette.onInverse else palette.ink)
            }
        }
    }
}

@Composable
fun SegmentedPills(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (on) palette.onInverse else palette.ink,
                modifier = Modifier.clip(PillShape).background(if (on) palette.inverse else palette.card)
                    .clickable { onSelect(i) }.padding(horizontal = 18.dp, vertical = 11.dp),
            )
        }
    }
}

/** Fill bar whose colour runs green → amber → red across the full track, revealed up to [fraction]. */
@Composable
fun GradientBar(fraction: Float, modifier: Modifier = Modifier, track: Color = palette.line) {
    val colors = listOf(palette.good, palette.warn, palette.bad)
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        clipRect(right = size.width * fraction.coerceIn(0f, 1f)) {
            drawRoundRect(Brush.horizontalGradient(colors), cornerRadius = r)
        }
    }
}

fun heatColor(f: Float, good: Color, warn: Color, bad: Color): Color =
    if (f < 0.5f) lerp(good, warn, f * 2) else lerp(warn, bad, ((f - 0.5f) * 2).coerceAtMost(1f))

/** Today's screen time per hour drawn as a dense row of coloured stripes, like a heat wave. */
@Composable
fun UsageWave(hourly: LongArray, modifier: Modifier = Modifier) {
    val good = palette.good
    val warn = palette.warn
    val bad = palette.bad
    val idle = palette.line
    val nowHour = remember { Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) + it.get(Calendar.MINUTE) / 60f } }
    Canvas(modifier.fillMaxWidth().height(170.dp)) {
        val stripes = 84
        val gap = size.width / stripes
        val peak = maxOf(hourly.maxOrNull() ?: 0L, 15 * 60_000L).toFloat()
        for (i in 0 until stripes) {
            val pos = i / (stripes - 1f) * 23f
            val h0 = pos.toInt().coerceAtMost(23)
            val h1 = (h0 + 1).coerceAtMost(23)
            val t = (1 - cos((pos - h0) * PI).toFloat()) / 2
            val v = hourly[h0] * (1 - t) + hourly[h1] * t
            val height = size.height * (0.06f + 0.94f * (v / peak))
            // Hours with almost no use stay a quiet grey stub so the wave only lights up where time went.
            val color = when {
                pos > nowHour -> idle.copy(alpha = 0.5f)
                v < 60_000f -> idle
                else -> heatColor(v / (45 * 60_000f), good, warn, bad)
            }
            val x = i * gap + gap / 2
            drawLine(color, Offset(x, size.height), Offset(x, size.height - height), strokeWidth = gap * 0.42f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun Ring(fraction: Float, size: Dp, color: Color, track: Color, stroke: Dp = 8.dp, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = stroke.toPx()
            val arc = Size(this.size.width - w, this.size.height - w)
            val topLeft = Offset(w / 2, w / 2)
            drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(w))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Pill-shaped text input with a round add button. */
@Composable
fun AddField(placeholder: String, onAdd: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (input.isNotBlank()) onAdd(input.trim())
        input = ""
    }
    Row(
        Modifier.fillMaxWidth().clip(PillShape).background(palette.cardAlt).padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (input.isEmpty()) Text(placeholder, color = palette.muted, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                input, { input = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.ink),
                cursorBrush = SolidColor(palette.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(palette.inverse).clickable { submit() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Add, "Add", tint = palette.onInverse, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    Row(
        Modifier.fillMaxWidth().clip(PillShape).background(palette.card).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = palette.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = palette.muted, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value, onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.ink),
                cursorBrush = SolidColor(palette.ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemovableChips(items: List<String>, onRemove: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(
                Modifier.clip(PillShape).background(palette.cardAlt).clickable { onRemove(item) }
                    .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item, style = MaterialTheme.typography.labelLarge, color = palette.ink)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Close, "Remove $item", tint = palette.muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)

/** Floating dark tab bar; the selected tab expands into a white pill with its label. */
@Composable
fun FloatingNav(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(34.dp)).background(palette.nav).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            if (i == selected) {
                Row(
                    Modifier.height(52.dp).clip(PillShape).background(Color.White).animateContentSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, null, tint = Color(0xFF111111), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = Color(0xFF111111))
                }
            } else {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { Icon(item.icon, item.label, tint = palette.navIcon, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}
