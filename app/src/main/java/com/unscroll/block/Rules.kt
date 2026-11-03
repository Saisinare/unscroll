package com.unscroll.block

import com.unscroll.data.Config
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Pure decision logic: given the config and what is on screen, should it be blocked and why. */
object Rules {
    fun appBlockReason(
        cfg: Config,
        pkg: String,
        usageMs: () -> Long,
        isUnreadableBrowser: (String) -> Boolean,
    ): String? {
        val now = System.currentTimeMillis()
        cfg.apps.find { it.pkg == pkg }?.let { rule ->
            if (rule.blockedUntil > now) return "${rule.label} is blocked until ${formatDateTime(rule.blockedUntil)}."
            val today = LocalDate.now().dayOfWeek
            if (today.value in rule.blockedWeekdays) {
                return "${rule.label} is blocked on ${today.getDisplayName(TextStyle.FULL, Locale.getDefault())}s."
            }
            rule.dailyLimitMin?.let { limit ->
                if (usageMs() >= limit * 60_000L) return "You've reached today's ${formatMinutes(limit)} limit for ${rule.label}."
            }
        }
        if (cfg.blockShortForm && pkg in Signatures.SHORT_VIDEO_APPS) return "Short-form video apps are blocked."
        if (cfg.blockMusic && pkg in Signatures.MUSIC_APPS) return "Songs are blocked."
        if (cfg.blockOtherBrowsers && isUnreadableBrowser(pkg)) {
            return "Unscroll can't check websites in this browser, so it's blocked. Use Chrome, Firefox, Brave, Edge or Samsung Internet."
        }
        return null
    }

    /** Returns the rule that blocks [url], if any. */
    fun blockedSite(url: String, cfg: Config): String? {
        val rules = cfg.blockedSites +
            (if (cfg.blockShorts) Signatures.SHORTS_URLS else emptyList()) +
            (if (cfg.blockReels) Signatures.REELS_URLS else emptyList()) +
            (if (cfg.blockShortForm) Signatures.SHORT_FORM_URLS else emptyList())
        return rules.firstOrNull { siteMatches(url, it) }
    }

    fun siteMatches(url: String, rule: String): Boolean {
        val u = normalizeSite(url)
        val r = normalizeSite(rule)
        if (r.isEmpty()) return false
        if ('.' !in r) return r in u
        val uHost = u.substringBefore('/')
        val rHost = r.substringBefore('/')
        val hostMatches = uHost == rHost || uHost.endsWith(".$rHost")
        return hostMatches && u.substringAfter('/', "").startsWith(r.substringAfter('/', ""))
    }

    fun normalizeSite(s: String) = s.trim().lowercase()
        .removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')

    /** Returns the first blocked topic mentioned in [text] as a whole word or phrase. */
    fun matchTopic(text: String, topics: List<String>): String? {
        val haystack = text.lowercase()
        return topics.firstOrNull { topic ->
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(topic.lowercase())}(?![\\p{L}\\p{N}])").containsMatchIn(haystack)
        }
    }

    fun looksLikeMusic(text: String): Boolean {
        val t = text.lowercase()
        return Signatures.MUSIC_HINTS.any { it in t }
    }
}

fun formatMinutes(min: Int): String = when {
    min < 60 -> "${min}m"
    min % 60 == 0 -> "${min / 60}h"
    else -> "${min / 60}h ${min % 60}m"
}

fun formatDuration(ms: Long): String = formatMinutes((ms / 60_000).toInt())

fun formatDateTime(ms: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")
        .format(java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()))
