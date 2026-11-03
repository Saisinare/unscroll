package com.unscroll.block

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.unscroll.R
import com.unscroll.data.BlockStats
import com.unscroll.data.Config
import com.unscroll.data.ConfigStore
import com.unscroll.data.UsageTracker
import com.unscroll.protect.TamperGuard
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Watches the foreground app and window content, and blocks apps, short-video feeds, websites,
 * YouTube topics/songs and attempts to disable Unscroll.
 */
class BlockerService : AccessibilityService() {
    // All checks run on this worker thread so usage queries and node walks never stall the UI.
    // State below is only touched from it.
    private val worker = HandlerThread("unscroll-blocker").apply { start() }
    private val handler = Handler(worker.looper)
    private val appLabel by lazy { getString(R.string.app_name) }
    private var foregroundPkg: String? = null
    private var lastBlockAt = 0L
    private val scanPending = AtomicBoolean(false)
    private var usage: Map<String, Long> = emptyMap()
    private var usageAt = 0L
    private var browsers: Set<String> = emptySet()
    private var browsersAt = 0L

    /** Re-checks the current app periodically so time limits trigger while it stays open. */
    private val tick = object : Runnable {
        override fun run() {
            foregroundPkg?.let { checkApp(it, freshUsage = true) }
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val scan = Runnable {
        scanPending.set(false)
        val root = rootInActiveWindow ?: return@Runnable
        val pkg = root.packageName?.toString() ?: return@Runnable
        if (pkg != packageName) scanWindow(pkg, root)
    }

    override fun onServiceConnected() {
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        worker.quitSafely()
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !isTransient(pkg)) {
            handler.post {
                foregroundPkg = pkg
                checkApp(pkg, freshUsage = false)
            }
        }
        // Content events arrive in bursts while scrolling; scan at most once per SCAN_DELAY_MS,
        // and only in apps where some rule could match.
        if (needsScan(pkg, ConfigStore.config) && scanPending.compareAndSet(false, true)) {
            handler.postDelayed(scan, SCAN_DELAY_MS)
        }
    }

    private fun needsScan(pkg: String, cfg: Config): Boolean = when {
        cfg.guardSettings() && TamperGuard.isSystemPackage(pkg) -> true
        pkg in Signatures.URL_BARS -> cfg.blockedSites.isNotEmpty() || cfg.blockShorts || cfg.blockReels || cfg.blockShortForm
        pkg == Signatures.YOUTUBE -> cfg.blockShorts || cfg.blockMusic || cfg.blockedTopics.isNotEmpty()
        pkg == Signatures.INSTAGRAM -> cfg.blockReels
        pkg in Signatures.SHORT_FEED_TITLES -> cfg.blockShortForm
        else -> false
    }

    private fun checkApp(pkg: String, freshUsage: Boolean): Boolean {
        val reason = Rules.appBlockReason(
            ConfigStore.config, pkg,
            usageMs = { usageOf(pkg, freshUsage) },
            isUnreadableBrowser = { it in installedBrowsers() && it !in Signatures.URL_BARS },
        ) ?: return false
        block(reason, GLOBAL_ACTION_HOME)
        return true
    }

    private fun scanWindow(pkg: String, root: AccessibilityNodeInfo) {
        val cfg = ConfigStore.config
        if (cfg.guardSettings() && TamperGuard.isTamperScreen(pkg, root, appLabel, cfg.strictActive())) {
            block("$appLabel protection is on. This setting is locked.", GLOBAL_ACTION_HOME)
            return
        }
        Signatures.URL_BARS[pkg]?.let { ids ->
            val url = root.firstText(ids)
            val rule = url?.let { Rules.blockedSite(it, cfg) }
            if (rule != null) {
                block("$rule is blocked.", GLOBAL_ACTION_BACK)
                return
            }
        }
        when (pkg) {
            Signatures.YOUTUBE -> checkYouTube(root, cfg)
            Signatures.INSTAGRAM -> if (cfg.blockReels && root.hasAny(Signatures.IG_REELS)) {
                block("Instagram Reels are blocked.", GLOBAL_ACTION_BACK)
            }
            else -> Signatures.SHORT_FEED_TITLES[pkg]?.let { title ->
                if (cfg.blockShortForm && hasTopTitle(root, title)) block("$title are blocked.", GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun checkYouTube(root: AccessibilityNodeInfo, cfg: Config) {
        if (cfg.blockShorts && root.hasAny(Signatures.YT_SHORTS)) {
            block("YouTube Shorts are blocked.", GLOBAL_ACTION_BACK)
            return
        }
        if (cfg.blockedTopics.isEmpty() && !cfg.blockMusic) return

        root.firstText(Signatures.YT_SEARCH)?.let { query ->
            Rules.matchTopic(query, cfg.blockedTopics)?.let {
                block("Searching for \"$it\" is blocked.", GLOBAL_ACTION_BACK)
                return
            }
        }

        // On the watch page, the title and channel sit just below the player.
        val player = root.firstBounds(Signatures.YT_PLAYER) ?: return
        val band = resources.displayMetrics.heightPixels * 0.25
        val meta = root.visibleTexts(800)
            .filter { it.bounds.top >= player.bottom - 4 && it.bounds.top < player.bottom + band }
            .joinToString("\n") { it.text }
        if (meta.isBlank()) return
        Rules.matchTopic(meta, cfg.blockedTopics)?.let {
            block("Videos about \"$it\" are blocked.", GLOBAL_ACTION_BACK)
            return
        }
        if (cfg.blockMusic && Rules.looksLikeMusic(meta)) block("Songs are blocked on YouTube.", GLOBAL_ACTION_BACK)
    }

    private fun hasTopTitle(root: AccessibilityNodeInfo, title: String): Boolean {
        val top = resources.displayMetrics.heightPixels * 0.2
        return root.visibleTexts(200).any { it.text.trim().equals(title, ignoreCase = true) && it.bounds.top < top }
    }

    private fun block(reason: String, action: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockAt < BLOCK_COOLDOWN_MS) return
        lastBlockAt = now
        BlockStats.record()
        pauseMedia()
        performGlobalAction(action)
        // Let the back/home action land before the block screen appears on top.
        handler.postDelayed({ BlockActivity.show(this, reason) }, 150)
    }

    private fun pauseMedia() {
        val audio = getSystemService(AudioManager::class.java)
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            audio.dispatchMediaKeyEvent(KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }
    }

    private fun usageOf(pkg: String, fresh: Boolean): Long {
        val now = SystemClock.elapsedRealtime()
        if (fresh || now - usageAt > USAGE_CACHE_MS) {
            usage = UsageTracker.today(this).perApp
            usageAt = now
        }
        return usage[pkg] ?: 0
    }

    private fun installedBrowsers(): Set<String> {
        val now = SystemClock.elapsedRealtime()
        if (browsersAt == 0L || now - browsersAt > BROWSER_CACHE_MS) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
            browsers = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }.toSet() - packageName
            browsersAt = now
        }
        return browsers
    }

    private fun isTransient(pkg: String) =
        pkg == "com.android.systemui" || "inputmethod" in pkg || "keyboard" in pkg

    private companion object {
        const val TICK_MS = 15_000L
        const val SCAN_DELAY_MS = 200L
        const val BLOCK_COOLDOWN_MS = 1_500L
        const val USAGE_CACHE_MS = 10_000L
        const val BROWSER_CACHE_MS = 60_000L
    }
}
