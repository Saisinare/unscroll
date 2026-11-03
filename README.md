# Unscroll

**Cut the endless scroll on your Android phone.**

Unscroll blocks the parts of your phone that are built to keep you scrolling, like Reels, Shorts and TikTok, without making you give up the apps themselves. You can keep YouTube for the video you came to watch and lose the Shorts feed. If you don't trust yourself to leave the rules alone, strict mode locks them for up to 30 days with no way out early.

It runs entirely on your phone. There's no account, and the app doesn't have internet permission, so what it sees on your screen can't be sent anywhere.

## Features

### Short-form video
- **YouTube Shorts:** in the app and at `youtube.com/shorts`.
- **Instagram Reels:** the Reels tab, shared reels and `instagram.com/reels`.
- **Everything else:** Facebook Reels, Snapchat Spotlight, TikTok, Moj, Josh, Likee, Kwai and similar apps.

### App limits
For any app, you can set:
- a daily time limit
- weekdays when it's blocked all day
- a date and time it's blocked until

### Websites
- Block a domain (`reddit.com`), a path (`youtube.com/shorts`) or a keyword (`casino`).
- Works in Chrome, Firefox, Brave, Edge, Samsung Internet, Opera, DuckDuckGo and other major browsers, including private tabs.
- Optionally block any browser whose address bar Unscroll can't read, so it can't be used as a way around your rules.

### YouTube filters
- **Blocked topics:** closes videos whose title or channel mentions a topic you chose, and blocks searches for it.
- **Block songs:** closes music and lyric videos, official audio and remixes, and blocks YouTube Music.

### Strict mode
- Locks your rules for 1, 3, 7, 14 or 30 days. It can't be ended early, but you can add more time.
- While it's on, you can only tighten your rules, never loosen them.
- Also locks the date and time settings, so moving the clock forward won't end it.

### Tamper protection
- Blocks the screens used to uninstall Unscroll, force stop it, clear its storage or turn off its service.
- Optional device admin adds a second lock, so Android itself refuses the uninstall.

### Today dashboard
See your screen time, an hour-by-hour usage chart, phone unlocks, how many times Unscroll blocked something, and your most-used apps with the time left on their limits.

## Permissions

| Permission | Required | What it's for |
| --- | --- | --- |
| Accessibility service | Yes | Sees which app or website is on screen so it can block it |
| Usage access | Yes | Measures screen time and enforces daily limits |
| Device admin | No | Adds a second lock against uninstalling |
| Battery optimisation off | No | Helps stop Android from killing the service |

An accessibility service can read what's on your screen, which is why the lack of internet permission matters. Your rules are stored only in the app's private storage, and Android backup is turned off.

## Getting started

There are no prebuilt downloads yet, so you'll need to build the app yourself.

**Requirements:** Android 8.0 (API 26) or newer, plus the Android SDK (API 35) and JDK 17.

```bash
git clone https://github.com/Saisinare/unscroll.git
cd unscroll
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then open Unscroll and follow the setup card on the **Today** tab.

If the accessibility switch is greyed out, go to **App info → ⋮ → Allow restricted settings**, then try again. Android does this for apps installed outside an app store.

The `release` build is signed with the debug key so that it installs directly. Swap in a real release key before publishing it anywhere.

## How it works

`BlockerService` is an Android accessibility service that watches the foreground app and its window content. When something matches a rule, it pauses any playing media, goes back or home, and shows a block screen explaining why.

Each surface is detected a little differently:
- **Apps:** by package name.
- **Shorts and Reels:** by the view IDs of their players.
- **Websites:** by reading the browser's address bar.
- **YouTube topics and songs:** by reading the title and channel below the player.

## Project structure

```text
app/src/main/java/com/unscroll/
├── block/    # Accessibility service, block screen, rules and detection signatures
├── data/     # Saved config, strict mode checks, usage and block stats
├── protect/  # Tamper guard, device admin and permission helpers
└── ui/       # Jetpack Compose screens: Today, Apps, Content, Sites, Strict
```

Built with Kotlin, Jetpack Compose and Material 3.

## Limitations

- **It can't stop everything:** safe mode, ADB from a computer or a factory reset can still remove the app.
- **App updates can break detection:** it relies on the internal view IDs of other apps. When a detection stops working, `block/Signatures.kt` is the one place to update.
- **English works best:** tamper protection, the Facebook and Snapchat feed checks and the song filter match English text, so they may miss screens in other languages or on heavily customised phones.
- **Song detection is a best guess:** it looks for phrases like "official video" or "lyrics", so it can miss some songs and catch some videos that aren't songs.

## Contributing

Contributions are welcome, especially fixes for detections broken by app updates. Please read the [contributing guide](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md) first. To report a vulnerability, see the [security policy](SECURITY.md).

## License

Unscroll is released under the [MIT License](LICENSE).
