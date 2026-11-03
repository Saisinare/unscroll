package com.unscroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unscroll.block.Signatures
import com.unscroll.data.Config

@Composable
fun ContentScreen(config: Config, ui: UiActions) {
    val active = listOf(config.blockShorts, config.blockReels, config.blockShortForm, config.blockMusic).count { it }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("Content", "Cut the endless scroll")

        SoftCard(color = palette.inverse) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$active", style = MaterialTheme.typography.displayLarge, color = palette.onInverse)
                Text(" / 4 filters on", style = MaterialTheme.typography.titleMedium, color = palette.onInverse.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp))
            }
            GradientBar(active / 4f, track = palette.onInverse.copy(alpha = 0.14f))
        }

        SectionHeader("Short videos")
        ToggleCard(
            "YouTube Shorts", "In the app and youtube.com/shorts", config.blockShorts,
            leading = { AppIcon(Signatures.YOUTUBE, fallback = palette.peach, glyph = "▶") },
        ) { v -> ui.update { it.copy(blockShorts = v) } }
        ToggleCard(
            "Instagram Reels", "Reels tab, shared reels and instagram.com/reels", config.blockReels,
            leading = { AppIcon(Signatures.INSTAGRAM, fallback = palette.lilac, glyph = "◎") },
        ) { v -> ui.update { it.copy(blockReels = v) } }
        ToggleCard(
            "Everything else", "Facebook Reels, Snapchat Spotlight, TikTok, Moj, Josh & more", config.blockShortForm,
            leading = { IconTile(palette.peach) { Icon(Icons.Filled.PlayArrow, null, tint = palette.ink, modifier = Modifier.size(22.dp)) } },
        ) { v -> ui.update { it.copy(blockShortForm = v) } }

        SectionHeader("YouTube")
        ToggleCard(
            "Block songs", "Music & lyric videos, official audio, remixes, YouTube Music", config.blockMusic,
            leading = { IconTile(palette.mint) { Text("♪", style = MaterialTheme.typography.titleLarge, color = palette.ink) } },
        ) { v -> ui.update { it.copy(blockMusic = v) } }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Blocked topics", style = MaterialTheme.typography.titleMedium, color = palette.ink, modifier = Modifier.weight(1f))
                Tag("${config.blockedTopics.size}", palette.cardAlt, palette.ink)
            }
            Hint("Videos whose title or channel mention these are closed. Searches too.")
            Spacer(Modifier.height(14.dp))
            AddField("e.g. cricket, gaming, prank") { topic ->
                val t = topic.lowercase()
                ui.update { c -> if (t in c.blockedTopics) c else c.copy(blockedTopics = c.blockedTopics + t) }
            }
            if (config.blockedTopics.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                RemovableChips(config.blockedTopics) { topic -> ui.update { c -> c.copy(blockedTopics = c.blockedTopics - topic) } }
            }
        }
    }
}
