# RhoCoo

An open-source Android remote for Roku TVs. Its main feature is a **mini remote in the notification shade**, so you can control the TV without leaving the app you're in.

- **Collapsed notification:** Vol−, Vol+, Left, Play/Pause, Right
- **Expanded notification:** adds Power, Mute, Instant replay, Back, Home, D-pad + OK, Rewind, and Fast forward, plus a gear that opens a small dialog over your current app to switch TVs
- **Stays on top:** the remote is held by a small foreground service, so Android keeps it in the top slot of the shade (shown in RhoCoo purple) instead of letting newer notifications push it down. It does no work in the background.
- **In the app:** the full remote, plus typing into TV search boxes
- **Quick Settings tile:** shows or hides the notification remote

RhoCoo is not affiliated with or endorsed by Roku, Inc. "Roku" is a trademark of Roku, Inc.

## How it works

RhoCoo uses Roku's [External Control Protocol](https://developer.roku.com/docs/developer-program/dev-tools/external-control-api.md): plain HTTP to the TV on port 8060 over your Wi-Fi. There's no account and no cloud service. TVs are found with SSDP. If discovery doesn't work on your network, you can type in the TV's IP address instead.

When you have more than one Roku, choose yours on first launch. RhoCoo remembers it, matched by serial number, so an IP address change won't break it. Use **Change device** to switch to another TV.

### If the TV ignores RhoCoo

Newer Roku software blocks remote apps by default. On the TV, go to **Settings → System → Advanced system settings → Control by mobile apps → Network access** and set it to **Default** (or Permissive).

## Building

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest
```

Requires the Android SDK (compileSdk 37). Min Android 8.0.

## Icons

The source artwork is in `design/` (`RhoCoo_icon_pack.zip`, unpacked in `design/icon_pack/`). The adaptive launcher background is generated from `launcher/rhocoo_1024.png`. The status-bar and themed-icon vectors come from `notification_icon.svg`.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
