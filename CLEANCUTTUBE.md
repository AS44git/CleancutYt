# CleancutTube

YouTube with only search and your You page: no Home feed, Shorts, Subscriptions,
Notifications, related videos, autoplay, end-screen suggestions or mixes. It opens on
search, and Home is replaced by the You page wherever YouTube asks for it. Everything is
locked in code: nothing in the app turns it off.

CleancutTube is a fork of [Morphe's patches](https://github.com/MorpheApp/morphe-patches)
(GPLv3), with [morphe-patches-library](https://github.com/MorpheApp/morphe-patches-library)
vendored at v2.1.0-dev.2 so its settings can carry the lock. Not affiliated with YouTube,
Google or Morphe.

## What CleancutTube adds

- `extensions/youtube/.../cleancut/Cleancut.java`: the locked settings and the Home redirect.
- `patches/.../youtube/cleancut/CleancutTubePatch.kt`: Autoplay forced off.
- `morphe-patches-library/.../settings/Setting.java`: `lockTo`, which `get()` answers and
  `save()` can't move.

## Installing

1. Install [MicroG RE](https://github.com/ReVanced/GmsCore/releases) (GmsCore): patched
   YouTube signs in to Google through it.
2. Install the CleancutTube APK. It's its own app (package `app.morphe.android.youtube`),
   so the normal YouTube can stay or be disabled.

## Builds

- **Debug build** (Actions tab): patches the stock YouTube APK attached to the draft release
  `stock-apk`. Signed with a throwaway key: uninstall the previous build first.

Target: YouTube 21.30.216, the APK (not bundle) from APKMirror.

## License

GPLv3 with Morphe's additional section 7 terms. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
