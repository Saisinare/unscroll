package com.unscroll.block

/**
 * View IDs and package names used to recognise what is on screen. Apps rename their views from
 * time to time, so this is the one place to update when a detection stops working.
 */
object Signatures {
    const val YOUTUBE = "com.google.android.youtube"
    const val INSTAGRAM = "com.instagram.android"
    const val FACEBOOK = "com.facebook.katana"
    const val SNAPCHAT = "com.snapchat.android"

    val YT_SHORTS = ids(YOUTUBE, "reel_recycler", "reel_player_page_container", "reel_watch_player", "reel_progress_bar")
    val YT_PLAYER = ids(YOUTUBE, "watch_player", "player_fragment_container")
    val YT_SEARCH = ids(YOUTUBE, "search_edit_text", "search_query")
    val IG_REELS = ids(INSTAGRAM, "clips_viewer_view_pager", "clips_viewer_container", "clips_video_container", "root_clips_layout")

    /** Titles shown at the top of the full-screen short-video feed inside these apps. */
    val SHORT_FEED_TITLES = mapOf(FACEBOOK to "Reels", SNAPCHAT to "Spotlight")

    val SHORT_VIDEO_APPS = setOf(
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme", // TikTok, Douyin
        "in.mohalla.video", "com.eterno.shortvideos", "io.chingari.app", "com.next.innovation.takatak", // Moj, Josh, Chingari, MX TakaTak
        "video.like", "co.triller.droid", "com.kwai.video", "com.kwai.bulldog", // Likee, Triller, Kwai, Snack Video
    )

    val MUSIC_APPS = setOf("com.google.android.apps.youtube.music")

    /** Address-bar view IDs per browser. These are readable in incognito/private tabs too. */
    val URL_BARS: Map<String, List<String>> = buildMap {
        listOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
            "com.brave.browser", "com.microsoft.emmx", "com.kiwibrowser.browser", "com.vivaldi.browser",
        ).forEach { put(it, ids(it, "url_bar")) }
        listOf("org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix")
            .forEach { put(it, ids(it, "mozac_browser_toolbar_url_view", "url_bar_title")) }
        put("org.mozilla.focus", ids("org.mozilla.focus", "mozac_browser_toolbar_url_view", "display_url"))
        put("com.sec.android.app.sbrowser", ids("com.sec.android.app.sbrowser", "location_bar_edit_text", "custom_tab_toolbar_url_bar_text"))
        put("com.opera.browser", ids("com.opera.browser", "url_field"))
        put("com.opera.mini.native", ids("com.opera.mini.native", "url_field"))
        put("com.duckduckgo.mobile.android", ids("com.duckduckgo.mobile.android", "omnibarTextInput"))
        put("com.mi.globalbrowser", ids("com.mi.globalbrowser", "url"))
    }

    val SHORTS_URLS = listOf("youtube.com/shorts")
    val REELS_URLS = listOf("instagram.com/reels", "instagram.com/reel")
    val SHORT_FORM_URLS = listOf("tiktok.com", "facebook.com/reel", "snapchat.com/spotlight")

    /** Phrases that mark a YouTube video as a song (checked against the title and channel name). */
    val MUSIC_HINTS = listOf(
        "official video", "official music video", "music video", "official audio", "lyric video", "lyrics",
        "lyrical", "full song", "video song", "audio song", "jukebox", "karaoke", "remix", "slowed", "reverb",
        "vevo", "- topic", " ft.", " feat.", "#song", "t-series", "sony music", "zee music", "saregama",
        "tips official", "yrf music", "speed records",
    )

    private fun ids(pkg: String, vararg names: String) = names.map { "$pkg:id/$it" }
}
