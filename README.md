# Gamepad Mapper

Map gamepad buttons to touch gestures on Android — no root required.

## Features

- **Tap** — single tap at a coordinate
- **Hold** — hold touch while button is pressed
- **Swipe** — swipe from point A to B with configurable duration
- **Multi-tap** — auto-clicker at configurable interval
- **Gesture path** — free multi-point gesture
- **Joystick swipe** — analog stick → directional touch

## Setup

1. Install APK from GitHub Actions artifact
2. Open app → tap **Enable Accessibility** → enable "Gamepad Mapper Service"
3. Tap **Start Overlay** → grant overlay permission
4. Create a profile, pick a screenshot as canvas reference
5. Add mappings: tap **+**, press gamepad button to detect, choose action, tap canvas to set coordinates
6. Tap **Activate** on the profile
7. The floating blue dot = service active; tap it to toggle on/off

## Build locally (via Termux)

```bash
cd /storage/emulated/0/Download/GamepadMapper
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Build via GitHub Actions

Push to `main` branch → Actions runs automatically → download APK from Artifacts tab.

## Permissions Required

| Permission | Purpose |
|---|---|
| Accessibility Service | Dispatch gestures + intercept gamepad keys |
| SYSTEM_ALERT_WINDOW | Floating toggle overlay |
| FOREGROUND_SERVICE | Keep overlay running |
| READ_MEDIA_IMAGES | Pick screenshot as canvas |
