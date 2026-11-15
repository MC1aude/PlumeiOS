# Plume for iPhone

This repository contains the Android app, an earlier partial Kotlin Multiplatform port,
and a functional native SwiftUI iPhone target in `iosApp`. The iPhone target uses
AVFoundation/MediaPlayer directly and does not depend on the incomplete shared UI.

## Build entirely on GitHub

1. Open Actions → Build iOS.
2. Select the branch containing the iPhone implementation and run the workflow.
3. Wait for simulator tests, the device archive and IPA packaging to pass.
4. Download the **Plume-unsigned-ipa** artifact and extract **Plume-unsigned.ipa**.

The app requires iOS 18.0 or newer, including iPhone 14 Pro on iOS 18.
No signing certificate, Apple password or Mac is needed in GitHub.

## Install from Windows

Use the official Sideloadly download at https://sideloadly.io/.
Install the Apple components listed by Sideloadly, connect and trust the iPhone,
select the extracted IPA in Sideloadly and sign in there with your Apple ID.
If iOS asks, trust the developer profile and enable Developer Mode in Settings.
A free account requires re-signing every seven days; Sideloadly supports automatic
refresh. Use the same Apple ID and bundle identifier when updating to preserve data.
Do not uninstall the app before updating: uninstalling removes its library and downloads.

## Implemented

- SoundCloud feed, search and freshly resolved compatible audio streams.
- Playback, seek, queue, shuffle, repeat, AirPlay, background audio and lock-screen commands.
- Independently persisted saved/downloaded status, playlists and playlist editing.
- Import audio from Files.
- Progressive audio downloads and file sharing.
- Offline HLS using Apple's background asset download service.
- Ukrainian, Russian and English; dark/light appearance.

HLS downloads are system-managed offline packages, not MP3 files, and remain inside
Plume. Use the regular audio download's Share action to export a standalone file.
Unavailable/restricted SoundCloud tracks and unsupported codecs report an error.

## Validation and limits

GitHub Actions runs persistence regression tests and a simulator launch/navigation test,
then builds the actual unsigned arm64 device archive. Test attachments include a library
screenshot. These checks do not prove live SoundCloud playback, offline HLS behavior or
background playback on a physical device: verify those on the target iPhone.

The native screens reproduce Plume's monochrome style and primary workflows. They are
not yet an exact pixel-for-pixel port of every Android screen or setting. Existing Android
sources and the earlier shared Kotlin module are retained, but Android/iOS screens do not
currently share their implementation.

## Local macOS development (optional)

Install Xcode and XcodeGen. From the repository root, prepare the supplied app icon:

```sh
swift iosApp/Tools/PrepareIcon.swift iosApp/Branding/AppIcon.png.base64 iosApp/Plume/Assets.xcassets/AppIcon.appiconset/AppIcon.png
```

Then run `xcodegen generate` inside `iosApp` and open `Plume.xcodeproj`.
Generated projects are not the source of truth; edit `project.yml`.
The original supplied PNG is stored losslessly in `Branding/AppIcon.png.base64`;
the preparation step creates an opaque 1024 x 1024 PNG for the app icon catalog.
GitHub Actions performs this step automatically.
