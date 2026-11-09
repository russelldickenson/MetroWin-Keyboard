<h1 align="center">MetroWin Keyboard (Phayz fork)</h1>

<p align="center">
  <b>The Windows Phone keyboard, on Android, with a few more looks and fonts.</b><br>
  Flat Metro keys, big light type, swipe typing, clipboard history, voice typing.<br>
  Free software. No ads, no analytics, no trackers: what you type stays on your phone.
</p>

<p align="center">
  <a href="LICENSE"><img alt="License: GPL-3.0-or-later" src="https://img.shields.io/badge/license-GPL--3.0--or--later-1ba1e2"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-1ba1e2">
  <img alt="Layouts: 17" src="https://img.shields.io/badge/layouts-17-1ba1e2">
</p>

<p align="center"><b>English</b> · <a href="README.it.md">Italiano</a></p>

> **This is a fork.** It is based on [MetroWin Keyboard](https://github.com/developerkenshi-coder/MetroWin-Keyboard) by developerkenshi-coder, whose work this project builds on and whose design it keeps. It has its own application id (`com.phayz.keyboard`), so it installs next to the original and never replaces it. It is not affiliated with or endorsed by the original project. See [What is different](#what-is-different) for exactly what was removed and added.

<p align="center"><img src="docs/screenshots/looks.png" width="92%" alt="The three original looks: Classic, Modern Glass and Modern Droid"></p>

<p align="center">
  <img src="docs/screenshots/home.png" width="22%" alt="The home page with the calibration tile">
  <img src="docs/screenshots/typing.png" width="22%" alt="The keyboard">
  <img src="docs/screenshots/calibration.png" width="22%" alt="Calibration result">
  <img src="docs/screenshots/settings.png" width="22%" alt="Settings">
</p>

<p align="center"><sub>The screenshots are those of the original project; they do not yet show this fork's additions.</sub></p>

## What is different

Compared with the original MetroWin Keyboard (version 26.1.2S):

**Added**
- **Key font and weight** (*Settings → Appearance*): the key letters can use the system font, Open Sans, Roboto or Atkinson Hyperlegible, at weights 200, 300, 400 or 500. Only the weights that look different for the chosen font are listed. The fonts are bundled variable fonts under the SIL OFL.
- **iPhone look**: pale grey (or dark) keyboard, white rounded keys with a thin shadow, darker special keys, "space" and "return" labels, dividers between the suggestions and a white balloon over the pressed key.
- **Modern Slate look**: dark grey keys with light grey symbols on a light grey surround (a dark one in dark mode), soft shadows and a light blue action key.
- **A new identity**: application id `com.phayz.keyboard`, so it coexists with the original.

**Changed**
- The globe key is **hidden by default** (*Settings → Size and layout → Show the globe key* brings it back).
- The update check points at placeholder repository values (`Repo.OWNER` and `Repo.NAME` in [`UpdateChecker.kt`](app/src/main/java/com/metrowin/keyboard/update/UpdateChecker.kt)) until this fork publishes its own releases, so it finds nothing for now.
- Release signing is read from `keystore.properties`; [`keystore.properties.example`](keystore.properties.example) shows the values to fill in.

**Removed**
- **GIF**: the GIF panel, its search, the sources (GIPHY, Tenor, Klipy, Imgur, a folder, your own service) and their settings.
- **Translation**: the translate panel, its services (Google, Google Cloud, DeepL, LibreTranslate, MyMemory) and their settings. As a result the `INTERNET` permission is used only by the update check.
- **MetroWin Launcher integration**: the theme sync receiver, the launcher theme file import, the *MetroWin Launcher* settings screen, its tutorial page, and the special handling of the launcher's search box.
- **Other projects**: the entries for MetroWin Launcher and for the original author's other apps in *About → Other projects*.

Everything else is as in the original: the typing engine, smart correction, calibration, clipboard, emoji, voice typing, backup, the 17 layouts and the original three looks.

## Install

There are no releases of this fork yet. Build it from source (below), then open the APK on your phone (allow your browser or file manager to install unknown apps).

1. Open **MetroWin Keyboard**. A guided set-up turns the keyboard on and selects it, noticing by itself when each step is done.
2. A tutorial is in the app and inside the keyboard (the three dots, then **?**).

Android 8.0 (API 26) or newer.

## Features

**Typing**
- Swipe typing, hold a key for accents, digits and symbols (hold time in ms, 200 by default), double space for a full stop, automatic capitals, slide on the space bar to move the cursor.
- A preview of the key you press.
- Correction that **Backspace undoes** and learns from (smart: it weighs where your finger landed, the words around it and what you undid before; it offers a missing space), accent fixing, completion, next-word and phrase prediction, offensive words blocked (plus your own list).
- Learns from you, **on the phone only**; never in password fields; exportable and erasable.

**Tools**, one tap from the bar: the three dots open the toolbox, and a **quick button** goes straight to the clipboard (or any tool you choose), with a dot when something new was copied.
- **Clipboard** history with a limit you choose, pinned items and **pictures**.
- **Emoji** and **classic emoticons** (`:-)`, kaomoji), with skin tones.
- **Voice typing**, also continuous transcription, optionally on-device only.

**Calibration**: type a few sentences and the keyboard learns where *your* fingers land (the offset, the curve of your thumb, how far your taps spread), corrects your touches without moving the keys, makes them taller or the gaps smaller if you still miss, and puts each fix to work between one sentence and the next so you see it improve. Accented letters are part of the test. Nothing is kept until you apply it.

**Smart correction**: a small model that runs on the phone fixes typos from where your finger landed, how likely each slip is on the keyboard, how common the word is, the word before it and the corrections you undid. Two equally likely fixes are shown, not applied. A missing space is offered.

**Backup**: one file with everything the app keeps (settings, learned words, dictionaries, clipboard); restore it on another phone or after reinstalling with another signature.

**Languages**: 17 layouts included (English, Italiano, Español, Français, Deutsch, Português, Nederlands, Polski, Türkçe, Русский, Українська, Ελληνικά, Čeština, Svenska, Română, العربية, עברית). Any other layout is a small text file ([docs/LAYOUTS.md](docs/LAYOUTS.md)); any dictionary can be imported (word lists, frequency lists, Hunspell `.dic`). Only small starter word lists are bundled (en, it, es, fr, de, pt), so import a full dictionary for the best predictions.

**Looks**: *Classic* (flat Windows Phone), *Modern Glass*, *Modern Droid* (Material You colours on Android 12+), *iPhone* and *Modern Slate*; dark / light / system, 20 Windows Phone colours or your own, pure black, a photo behind the keys (its colour becomes the accent), and, optionally, a keyboard that follows the colour of the app under it. Key letters in the font and weight you choose.

**Size**: a live preview, key height (portrait and landscape), keyboard width with left / centre / right position, key spacing, number row on or off, and an automatic calibration that learns from a few typed sentences where your fingers land.

**Feedback**: a Windows Phone style click (synthesised, nothing of Microsoft's inside), Android's sound or your own file; vibration with adjustable strength.

## Privacy and permissions

No analytics, ads or trackers. The full list is in [PRIVACY.md](PRIVACY.md).

| Permission | Why |
| --- | --- |
| `RECORD_AUDIO` | Voice typing only; asked when you tap the microphone |
| `INTERNET` | Only for the update check, which asks the repository set in `UpdateChecker.kt` every 12 hours (can be switched off) |
| `REQUEST_INSTALL_PACKAGES` | Only to open the installer on a new version you chose to download in *Settings → Updates* |
| `RECEIVE_BOOT_COMPLETED` | Keeps the 12-hour update check after a restart |
| `VIBRATE` | Key vibration |

Backups and device transfer are disabled for the app's data. The colour service is off until you turn it on in the accessibility settings, reads no text and sends nothing.

## Build from source

JDK 17 or newer and the Android SDK (platform 35).

```sh
git clone <the address of this repository>
cd <the cloned folder>
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # engine, keyboard and tutorial tests on the JVM
./gradlew assembleRelease        # needs keystore.properties (copy keystore.properties.example); -PsignWithDebugKey=true for a test build
```

## Contributing

Bug reports and ideas are welcome in the issue tracker. See [CONTRIBUTING.md](CONTRIBUTING.md), which includes the checklist for adding a feature (and its tutorial page). Security issues: [SECURITY.md](SECURITY.md). Changes to the parts this fork shares with the original may also be worth sending to the [original project](https://github.com/developerkenshi-coder/MetroWin-Keyboard).

## License and credits

GNU General Public License v3.0 or later ([LICENSE](LICENSE)). The original MetroWin Keyboard is by developerkenshi-coder; this fork keeps its licence and its history. Fonts are Open Sans, Roboto and Atkinson Hyperlegible Next (SIL Open Font License 1.1); see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

This is an independent project. It is not affiliated with or endorsed by Microsoft, Apple or Google; Windows Phone is a trademark of Microsoft Corporation, and the iPhone look only imitates the appearance of a keyboard, with no Apple code or fonts.
