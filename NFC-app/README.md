# NFC-app

The Android app for Tapme. It lets a receiver start a UPI payment session from their phone using NFC tag emulation, with QR as a fallback.

## Feature Summary

- saves a receiver profile with UPI ID and display name
- generates a standard `upi://pay` deep link
- broadcasts that deep link through Android Host Card Emulation
- renders the same payment session as a QR code
- stores local transaction history
- supports optional notification-based transaction tracking

## User Flow

1. Open Settings and save a UPI ID plus display name.
2. Enter an optional amount and note on the receive screen.
3. Tap Generate.
4. The app arms an NFC session and shows a QR code.
5. The payer either taps over NFC or scans the QR code.
6. Tapme records the initiated session locally as Pending.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── kotlin/com/nfcupi/pay/
│   ├── MainActivity.kt
│   ├── NfcUpiApp.kt
│   ├── data/
│   │   ├── PreferencesRepository.kt
│   │   └── TransactionHistoryRepository.kt
│   ├── di/
│   │   └── AppModule.kt
│   ├── nfc/
│   │   ├── NdefBuilder.kt
│   │   ├── UpiDeepLinkBuilder.kt
│   │   └── UpiNfcHceService.kt
│   ├── notifications/
│   │   └── TransactionNotificationListenerService.kt
│   ├── ui/
│   │   ├── components/
│   │   │   └── QrCodeImage.kt
│   │   ├── navigation/
│   │   │   └── AppNavGraph.kt
│   │   ├── screens/
│   │   │   ├── receive/
│   │   │   └── settings/
│   │   └── theme/
│   └── util/
│       └── NfcAvailability.kt
└── res/xml/apduservice.xml
```

## Architecture

The app uses MVVM with Hilt, StateFlow, and Jetpack Compose.

```
UI
ReceiveScreen <-> ReceiveViewModel
SettingsScreen <-> SettingsViewModel

Data
PreferencesRepository
TransactionHistoryRepository

NFC
UpiDeepLinkBuilder -> NdefBuilder -> UpiNfcHceService
```

### Important pieces

- `UpiDeepLinkBuilder` creates a standard `upi://pay` URI
- `NdefBuilder` wraps the URI into an NDEF message
- `UpiNfcHceService` exposes that NDEF payload through HCE
- `ReceiveViewModel` manages session arming, transaction creation, and history state
- `SettingsViewModel` validates and saves the receiver profile

## Payment Payload

The app generates a standard NPCI-style deep link:

```
upi://pay
  ?pa=<upi-id>
  &pn=<display-name>
  &am=<amount>
  &cu=INR
  &tn=<note>
  &tr=<transaction-reference>
```

That same payload is used for both NFC and QR.

## Build And Run

### Debug build

```bash
./gradlew assembleDebug
```

### Install on device

```bash
./gradlew installDebug
```

### Release build

```bash
./gradlew assembleRelease
```

## Device Requirements

- Android 7.0 or later
- NFC enabled
- Host Card Emulation support on the receiver device

The app checks availability at runtime and shows warnings when NFC or HCE is unavailable.

## Permissions

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
<uses-feature android:name="android.hardware.nfc.hce" android:required="false" />
```

Notification access is optional. It improves transaction status tracking but is not required to use the app.

## Notes And Limitations

- The app currently uses a direct `upi://pay` NFC payload.
- NFC tap behavior depends on how the payer's phone and installed UPI apps handle NFC-discovered URIs.
- QR fallback is the reliable cross-device fallback and is part of the normal product flow.
- HCE only remains active while the app is foregrounded.

## Contributing To The Android App

Useful areas for contribution:

- device compatibility testing
- better error handling around NFC state and payment handoff
- Compose UI and accessibility improvements
- transaction history and notification parsing improvements
- documentation updates

Before opening a PR, run:

```bash
./gradlew assembleDebug
```
