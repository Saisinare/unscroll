package com.unscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.unscroll.block.Rules
import com.unscroll.block.Signatures
import com.unscroll.data.Config
import kotlin.math.absoluteValue

private val SUGGESTIONS = listOf("instagram.com", "facebook.com", "x.com", "reddit.com", "youtube.com", "netflix.com", "primevideo.com")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SitesScreen(config: Config, ui: UiActions) {
    val tints = listOf(palette.mint, palette.lilac, palette.peach, palette.sky)
    val add = { site: String ->
        val s = Rules.normalizeSite(site)
        if (s.isNotEmpty()) ui.update { c -> if (s in c.blockedSites) c else c.copy(blockedSites = c.blockedSites + s) }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("Websites", "Blocked in every browser, incognito too")

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sites blocked", style = MaterialTheme.typography.titleLarge, color = palette.ink, modifier = Modifier.weight(1f))
                Tag("${Signatures.URL_BARS.size} browsers", palette.mint, palette.ink)
            }
            Text("${config.blockedSites.size}", style = MaterialTheme.typography.displayLarge, color = palette.ink)
            Hint("Domain (reddit.com), path (youtube.com/shorts) or a keyword without a dot.")
            Spacer(Modifier.height(14.dp))
            AddField("Add a site or keyword", add)

            config.blockedSites.forEachIndexed { i, site ->
                if (i == 0) Spacer(Modifier.height(8.dp)) else HorizontalDivider(color = palette.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val isKeyword = '.' !in site
                    IconTile(tints[site.hashCode().absoluteValue % tints.size], 38.dp) {
                        if (isKeyword) Icon(Icons.Filled.Search, null, tint = palette.ink, modifier = Modifier.size(18.dp))
                        else Text(site.first().uppercase(), style = MaterialTheme.typography.titleMedium, color = palette.ink)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(site, style = MaterialTheme.typography.bodyLarge, color = palette.ink)
                        Hint(if (isKeyword) "Any address containing this" else "Including subdomains")
                    }
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(palette.cardAlt)
                            .clickable { ui.update { c -> c.copy(blockedSites = c.blockedSites - site) } },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Close, "Remove $site", tint = palette.muted, modifier = Modifier.size(16.dp)) }
                }
            }
        }

        val remaining = SUGGESTIONS.filter { it !in config.blockedSites }
        if (remaining.isNotEmpty()) {
            SectionHeader("Quick add")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                remaining.forEach { s ->
                    Text(
                        "+ $s",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.ink,
                        modifier = Modifier.clip(PillShape).background(palette.card).clickable { add(s) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        SectionHeader("Loopholes")
        ToggleCard(
            "Block other browsers", "Closes browsers Unscroll can't read, so there's no way around", config.blockOtherBrowsers,
            leading = { IconTile(palette.sky) { Icon(Icons.Filled.Search, null, tint = palette.ink, modifier = Modifier.size(20.dp)) } },
        ) { v -> ui.update { it.copy(blockOtherBrowsers = v) } }
    }
}
