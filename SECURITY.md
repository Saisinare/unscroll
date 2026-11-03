# Security Policy

Unscroll uses an accessibility service that can read what's on the screen, so security problems are taken seriously.

## Reporting a vulnerability

**Please don't open a public issue.** [Report it privately on GitHub](https://github.com/Saisinare/unscroll/security/advisories/new) instead, and include:

- what the problem is and what an attacker could do with it
- steps to reproduce it
- your phone model and Android version, and the Unscroll version or commit

Don't include real personal data from your phone in the report.

This project has one maintainer, and fixes are made on a best-effort basis, so there's no guaranteed response time. Please give the maintainer a reasonable chance to fix the problem before you share it publicly.

## Supported versions

Only the latest code on `main` gets security fixes.

## What counts as a security problem

- Anything that lets on-screen content or usage data leave the device
- Another app being able to read Unscroll's data or change its rules
- Another app being able to misuse Unscroll's accessibility service or device admin

## What doesn't count

Please open a [regular issue](https://github.com/Saisinare/unscroll/issues) for these:

- Ways around a block, or a detection that has stopped working
- Removing the app through safe mode, ADB or a factory reset, which the app can't prevent and the README already lists as limitations
