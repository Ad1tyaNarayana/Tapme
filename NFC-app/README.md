# NFC-app

The Android application for Tapme. It turns the receiver's phone into a contactless NFC POS terminal by emulating a physical NFC tag using Android's Host Card Emulation (HCE) API.

---

## What It Does

1. The receiver enters their UPI ID and display name in **Settings**.
2. On the main screen they optionally set an amount and transaction note, then tap **Activate**.
3. The app starts a background `HostApduService` that makes the phone respond to NFC readers exactly like a Type 4 NDEF tag.
4. The NDEF record contains an HTTPS URL pointing to the Tapme redirect server, with the UPI ID and payment details encoded as query parameters.
5. When the payer taps their phone, it reads the tag, opens the URL in a browser, and the redirect server returns a `upi://pay` deep link — triggering the native UPI app chooser.

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── kotlin/com/nfcupi/pay/
│   ├── MainActivity.kt              # Entry point; manages preferred HCE service
│   ├── NfcUpiApp.kt                 # Hilt application class
│   ├── data/
│   │   └── PreferencesRepository.kt # DataStore-backed UPI ID & name persistence
│   ├── di/
│   │   └── AppModule.kt             # Hilt module — provides NfcAdapter
│   ├── nfc/
│   │   ├── NdefBuilder.kt           # Converts a URI string → raw NDEF byte array
│   │   ├── UpiDeepLinkBuilder.kt    # Assembles the HTTPS redirect URL
│   │   └── UpiNfcHceService.kt      # Core HCE service; handles APDU handshake
│   ├── ui/
│   │   ├── navigation/
│   │   │   └── AppNavGraph.kt       # Compose Navigation graph
│   │   ├── screens/
│   │   │   ├── receive/
│   │   │   │   ├── ReceiveScreen.kt     # Main screen (activate / amount / note)
│   │   │   │   └── ReceiveViewModel.kt  # UI state, HCE lifecycle
│   │   │   └── settings/
│   │   │       ├── SettingsScreen.kt    # UPI ID & display name form
│   │   │       └── SettingsViewModel.kt # Validates and saves profile
│   │   └── theme/                   # Material 3 theme
│   └── util/
│       └── NfcAvailability.kt       # NFC / HCE capability checks
└── res/
    └── xml/
        └── apduservice.xml          # Declares the NDEF AID for HCE routing
```

---

## Architecture

The app follows **MVVM** with unidirectional data flow, backed by Hilt for dependency injection.

```
┌─────────────────────────────────────────────────────┐
│                      UI Layer                        │
│  ReceiveScreen ◄──► ReceiveViewModel                │
│  SettingsScreen ◄──► SettingsViewModel              │
└────────────────────────┬────────────────────────────┘
                         │ StateFlow<UiState>
┌────────────────────────▼────────────────────────────┐
│                    Data Layer                        │
│  PreferencesRepository (Jetpack DataStore)          │
│  Persists: upi_id, display_name                     │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                     NFC Layer                        │
│  UpiDeepLinkBuilder   builds https:// redirect URL  │
│  NdefBuilder          encodes URL → NDEF bytes       │
│  UpiNfcHceService     HostApduService (HCE)          │
│    └─ handles APDU sequence per NFC Forum Type 4 Tag │
└─────────────────────────────────────────────────────┘
```

### Key Components

#### `UpiNfcHceService`

The heart of the app. Extends `HostApduService` and implements the NFC Forum Type 4 Tag APDU handshake:

| Step | Command                                     | Response                             |
| ---- | ------------------------------------------- | ------------------------------------ |
| 1    | SELECT AID (`D2760000850101`)               | `90 00` (OK)                         |
| 2    | SELECT FILE — Capability Container (`E103`) | `90 00`                              |
| 3    | READ BINARY — CC                            | Returns 15-byte capability container |
| 4    | SELECT FILE — NDEF (`E104`)                 | `90 00`                              |
| 5    | READ BINARY — NDEF                          | Returns 2-byte length + NDEF message |

The NDEF message is a single URI record containing the HTTPS redirect URL, built fresh each time the service starts.

#### `UpiDeepLinkBuilder`

Assembles the URL sent to the redirect server:

```
https://your-project.vercel.app/api
  ?pa=<upi-id>
  &pn=<display-name>
  &am=<amount>        (optional)
  &cu=INR
  &tn=<note>
```

#### `NdefBuilder`

Wraps the URL in a standard `NdefRecord.createUri()` record and serialises the `NdefMessage` to a raw byte array ready for HCE transmission.

#### `PreferencesRepository`

Wraps Jetpack DataStore. Exposes a `Flow<UserProfile>` and a `suspend fun saveProfile(profile)`. Both the Receive and Settings ViewModels inject this repository via Hilt.

#### `MainActivity`

Calls `CardEmulation.setPreferredService()` in `onResume` and `unsetPreferredService()` in `onPause` to ensure Tapme's HCE service takes priority over other card emulation apps while the screen is on.

---

## Tech Stack

| Library                     | Version    | Purpose                        |
| --------------------------- | ---------- | ------------------------------ |
| Kotlin                      | 2.1.10     | Language                       |
| Jetpack Compose BOM         | 2025.01.01 | UI toolkit                     |
| Material 3                  | via BOM    | Design system                  |
| Navigation Compose          | 2.8.6      | Screen navigation              |
| Hilt                        | 2.55       | Dependency injection           |
| Hilt Navigation Compose     | 1.2.0      | ViewModel injection in Compose |
| Lifecycle ViewModel Compose | 2.8.7      | `collectAsStateWithLifecycle`  |
| DataStore Preferences       | —          | Persistent key-value storage   |
| Android Gradle Plugin       | 8.13.2     | Build tooling                  |

**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 35 (Android 15)

---

## Building

**Debug build:**

```bash
./gradlew assembleDebug
```

**Install on connected device:**

```bash
./gradlew installDebug
```

**Release build** (requires a signing config in `local.properties` or `build.gradle.kts`):

```bash
./gradlew assembleRelease
```

---

## Device Requirements

- Android 7.0 or later (API 24+)
- NFC chip with **Host Card Emulation (HCE)** support
  - Most mid-range and flagship Android phones since ~2015 qualify
  - Budget/entry-level devices occasionally omit HCE even when they have NFC
- NFC must be enabled in system settings

The app checks NFC availability at runtime and shows a banner if NFC is off or unsupported.

---

## Permissions

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
<uses-feature android:name="android.hardware.nfc.hce" android:required="false" />
```

Both NFC features are marked `required="false"` so the app can be installed on non-NFC devices; it degrades gracefully with an in-app warning.

---

## Notes & Limitations

- The HCE service is only active while `MainActivity` is in the foreground. Locking the screen or switching apps stops emulation.
- The emulated tag uses the standard NDEF AID (`D276000085010100`), which all modern Android NFC readers recognise.
- Because the payload is an HTTPS URL (not a raw `upi://` URI), the payer's phone opens a browser to follow the redirect — there is a brief ~100–300 ms network round-trip before the UPI chooser appears.
