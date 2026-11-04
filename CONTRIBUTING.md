# Contributing to Unscroll

Thanks for helping out. Bug reports, detection fixes, code and docs are all welcome.

By taking part, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md). To report a security problem, follow the [security policy](SECURITY.md) instead of opening a public issue.

## Project principles

Unscroll asks people to trust it with an accessibility service that can read their screen. These rules protect that trust, and pull requests that break them won't be merged:

- **No network access.** Don't add the `INTERNET` permission, or a library that adds it.
- **No tracking.** No analytics, ads, crash reporters or accounts. Data stays on the device.
- **Screen access is only for blocking.** Don't store or log what the accessibility service reads.
- **Strict mode stays strict.** Don't add a way to end it early, loosen a rule while it's on, or get around it.

## Reporting a bug

Search the [existing issues](https://github.com/Saisinare/unscroll/issues) first, then open a new one with:

- what you expected and what happened
- your phone model and Android version
- the Unscroll version
- the version and language of the app involved, if it's a detection problem

Remove personal information from screenshots and logs before posting them.

## Suggesting a feature

Open an issue describing the problem you want to solve. For large changes, please agree on the approach in an issue before you start, so your work doesn't go to waste.

## Fixing a broken detection

This is one of the most useful ways to help. Unscroll recognises other apps by their internal view IDs, and those can change when an app updates. They're all kept in `app/src/main/java/com/unscroll/block/Signatures.kt`.

To find the new ID, open the screen on a connected phone and run:

```bash
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml
```

Look for the `resource-id` values in `window.xml`. **This file contains whatever was on your screen, so don't attach it to an issue or commit it.**

Add new IDs alongside the old ones, because people may still be on older versions of the app.

The default dump filename, `window.xml`, is ignored by Git to help prevent accidental uploads. If you rename a dump, keep it outside the repository. Delete the local copy and the copy on your phone when you're finished:

```bash
adb shell rm /sdcard/window.xml
```

## Making a change

1. Fork the repo and create a branch from `main`.
2. Set up the project using the [getting started steps](README.md#getting-started).
3. Make your change. Keep it focused on one thing.
4. Make sure the app still builds and lint passes:

   ```bash
   ./gradlew assembleDebug
   ./gradlew lint
   ```

5. Test on a real phone or emulator. The project has no automated tests yet, so this is the main check. If you add decision logic to `block/Rules.kt`, unit tests are very welcome.
6. Open a pull request.

### Code style

- Match the code around your change.
- Keep block decisions in `block/Rules.kt` and view IDs and package names in `block/Signatures.kt`.
- Keep the work done in `BlockerService` light, because it runs on every screen change.
- Write short commit messages that say what the commit does, such as `Add Threads feed detection`.

### Pull requests

Your pull request description should say:

- what changed and why
- the phone or emulator and Android version you tested on
- any new permission or dependency, and why it's needed
- screenshots, if the UI changed

Every pull request is reviewed by the maintainer. This is a small project, so a review may take some time, and a change may be declined if it doesn't fit the project.

## License

Unscroll is released under the [MIT License](LICENSE). By submitting a contribution, you agree that it's your own work, or that you have the right to submit it, and that it will be licensed under the same terms.
