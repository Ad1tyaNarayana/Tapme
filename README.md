# Tapme

Tapme is an Android app for starting UPI payments with a tap. The receiver's phone emulates an NFC tag, the payer taps their phone, and Android attempts to hand off a native UPI payment flow. The app also shows the same payment session as a QR code and keeps a local transaction history.

This repository is the opensource version of Tapme. The Android app currently matches the private v1.4.0 app behavior.

## What The App Does

- turns an Android phone into a contactless payment initiator using Host Card Emulation (HCE)
- builds a standard `upi://pay` deep link from the receiver's UPI ID, display name, optional amount, and optional note
- publishes that deep link through NFC and also renders it as a QR code fallback
- stores initiated transactions locally and supports optional notification-based status tracking

## Repository Layout

```
Tapme-opensource/
├── NFC-app/       # Android app (Kotlin, Compose, Hilt)
└── LICENSE
```

## How It Works

1. The receiver installs Tapme and saves a UPI ID plus display name in Settings.
2. On the main screen, the receiver optionally enters an amount and note and taps Generate.
3. The app creates a `upi://pay` URI and starts its HCE service.
4. The receiver can either tap the payer's phone over NFC or let the payer scan the on-screen QR code.
5. If the payer's device resolves the NFC payload, Android shows the installed UPI apps. The same payload can always be used through QR.
6. Tapme stores the initiated payment locally as Pending and lets the receiver review or update it later.

## Current State

The Android app in this repo currently uses a direct `upi://pay` payload. That means:

- `NFC-app` is the primary product in active use
- QR fallback remains important because NFC deep-link handling varies across payer devices and UPI apps

## Quick Start

### 1. Build the Android app

```bash
cd NFC-app
./gradlew installDebug
```

### 2. Configure Tapme

Open the app and set:

- your UPI ID, for example `name@upi`
- your display name

### 3. Start a payment session

1. Enter an optional amount and note.
2. Tap Generate.
3. Either tap the payer's phone or show them the QR code.
4. Review the initiated session in the history drawer.

## Setup Notes

- Receiver device: Android 7.0+ with NFC and HCE support
- Payer device: Android device with a UPI app installed
- iOS is not a supported payer NFC target for this flow
- The app works best when kept in the foreground while emulation is active

## Components

### NFC-app

See [NFC-app/README.md](NFC-app/README.md) for app architecture, permissions, build commands, and implementation details.

## Contributing

Contributions are welcome, especially around NFC compatibility, transaction tracking, device testing, and documentation.

### Before opening a PR

1. Keep the change focused.
2. Document any behavior change clearly.
3. Run the relevant checks you can run locally.
4. Update docs if setup, behavior, or architecture changed.

### Suggested local checks

```bash
cd NFC-app
./gradlew assembleDebug
```

### Good contribution areas

- NFC interoperability testing across devices and OEMs
- UPI app compatibility notes
- UI polish and accessibility
- transaction history UX
- documentation and onboarding

## Security And Privacy

- Tapme does not process payments itself. It only prepares a standard UPI deep link and hands off to installed UPI apps.
- Merchant profile data and local transaction history stay on the receiver's device.
- Notification access is optional and only used for local transaction status tracking.

## Limitations

- Direct `upi://pay` over NFC is device- and app-dependent.
- Some payer devices will not surface a chooser from the NFC tap, which is why QR fallback exists.
- HCE stops when the app leaves the foreground or the phone is locked.

## License

This project is released under the license in [LICENSE](LICENSE).
