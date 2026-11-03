package com.unscroll.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppRule(
    val pkg: String,
    val label: String,
    /** Minutes of use allowed per day; null means no limit. */
    val dailyLimitMin: Int? = null,
    /** java.time.DayOfWeek values (1 = Monday) on which the app is blocked all day. */
    val blockedWeekdays: Set<Int> = emptySet(),
    /** Epoch millis until which the app is blocked completely. */
    val blockedUntil: Long = 0,
) {
    fun isEmpty(now: Long = System.currentTimeMillis()) =
        dailyLimitMin == null && blockedWeekdays.isEmpty() && blockedUntil <= now
}

@Serializable
data class Config(
    val apps: List<AppRule> = emptyList(),
    val blockShorts: Boolean = false,
    val blockReels: Boolean = false,
    /** TikTok-style apps, Facebook Reels and Snapchat Spotlight. */
    val blockShortForm: Boolean = false,
    val blockMusic: Boolean = false,
    val blockedTopics: List<String> = emptyList(),
    /** Domains ("reddit.com"), paths ("youtube.com/shorts") or bare keywords ("casino"). */
    val blockedSites: List<String> = emptyList(),
    /** Block browsers whose address bar Unscroll can't read, so blocked sites can't be reached through them. */
    val blockOtherBrowsers: Boolean = false,
    val strictUntil: Long = 0,
    val strictStartedAt: Long = 0,
    val uninstallProtection: Boolean = false,
) {
    fun strictActive(now: Long = System.currentTimeMillis()) = strictUntil > now

    /** Whether Settings screens that could disable Unscroll should be blocked. */
    fun guardSettings() = uninstallProtection || strictActive()
}

/** While strict mode runs, a change is allowed only if it keeps or tightens every restriction. */
object StrictGuard {
    fun isTighterOrEqual(old: Config, new: Config): Boolean {
        if (new.strictUntil < old.strictUntil) return false
        val flags = listOf<(Config) -> Boolean>(
            Config::blockShorts, Config::blockReels, Config::blockShortForm, Config::blockMusic,
            Config::blockOtherBrowsers, Config::uninstallProtection,
        )
        if (flags.any { it(old) && !it(new) }) return false
        if (!new.blockedSites.containsAll(old.blockedSites)) return false
        if (!new.blockedTopics.containsAll(old.blockedTopics)) return false

        val now = System.currentTimeMillis()
        val newApps = new.apps.associateBy { it.pkg }
        return old.apps.all { o ->
            val n = newApps[o.pkg] ?: AppRule(o.pkg, o.label)
            (o.dailyLimitMin == null || (n.dailyLimitMin != null && n.dailyLimitMin <= o.dailyLimitMin)) &&
                n.blockedWeekdays.containsAll(o.blockedWeekdays) &&
                maxOf(n.blockedUntil, now) >= maxOf(o.blockedUntil, now)
        }
    }
}

object ConfigStore {
    private const val KEY = "config"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(Config())

    val state: StateFlow<Config> = _state
    val config: Config get() = _state.value

    fun init(context: Context) {
        prefs = context.getSharedPreferences("unscroll", Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { saved ->
            runCatching { json.decodeFromString<Config>(saved) }.onSuccess { _state.value = it }
        }
    }

    /** Applies [change] and returns false if strict mode rejected it. */
    @Synchronized
    fun update(change: (Config) -> Config): Boolean {
        val old = _state.value
        val new = change(old)
        if (old.strictActive() && !StrictGuard.isTighterOrEqual(old, new)) return false
        _state.value = new
        prefs.edit().putString(KEY, json.encodeToString(new)).apply()
        return true
    }
}
