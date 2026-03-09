# Tapme

Tapme is a contactless UPI payment initialization system that lets a **receiver** tap their phone to a **payer's** phone to trigger a native UPI payment — no QR code, no data stored, no app to install on the payer's side.

The receiver runs the Tapme Android app. Their phone emulates an NFC tag over Host Card Emulation (HCE). When the payer taps, their phone reads the tag, opens a browser, hits a small redirect server, and Android's intent system pops up the UPI app chooser — GPay, PhonePe, Paytm, or whatever the payer has installed.

---

## Repository Structure

```
Tapme/
├── NFC-app/          # Android app (Kotlin + Jetpack Compose)
└── NFC-redirect/     # Vercel Edge Function — HTTPS → upi:// redirect
```

---

## How It Works — End to End

```
Receiver's phone (Tapme app)          Payer's phone
─────────────────────────────         ──────────────────────────────
1. User enters UPI ID & name
   in Settings.

2. User sets optional amount /
   note, then taps "Activate".

3. UpiNfcHceService starts —
   phone now emulates a Type 4
   NDEF NFC tag over HCE.

4. Receiver holds phone to           5. NFC chip reads the NDEF tag.
   payer's NFC antenna.                 Record contains an HTTPS URL:
                                        https://your-project.vercel.app/api
                                        ?pa=upi@id&pn=Name&am=50&cu=INR&tn=...

                                     6. Android opens the URL in the
                                        default browser (or Chrome).

                                     ──── network request ────────────▶
                                                                   Vercel Edge Function
                                                                   receives GET request,
                                                                   parses query params,
                                                                   returns HTTP 302 →
                                                                   upi://pay?pa=...
                                     ◀─── 302 redirect ──────────────

                                     7. Browser follows the redirect.
                                        Android sees upi:// scheme,
                                        shows the native UPI app
                                        chooser.

                                     8. Payer picks their app (GPay /
                                        PhonePe / Paytm …) and pays.
```

---

## Architecture

### Why HTTPS instead of `upi://` directly in the NFC tag?

A `upi://pay` URI written directly into an NDEF record would only open apps that register **NFC tag intent filters** for that scheme. Most UPI apps handle `upi://` as a standard `ACTION_VIEW` deep link but do **not** expose an NFC tag filter — so the direct approach typically shows nothing on the payer's device.

Encoding an `https://` URL instead sidesteps this: Android always handles HTTPS tags by opening the browser, the browser follows the redirect, and then the `upi://` scheme is dispatched via the normal intent resolver — which _does_ show the app chooser.

```
NDEF tag record (https://)
         │
         ▼
  Android opens browser
         │
         ▼
  Vercel Edge Function
  returns 302 → upi://
         │
         ▼
  Android intent resolver
  shows UPI app chooser
```

### Component Overview

| Component        | Technology                               | Role                                                    |
| ---------------- | ---------------------------------------- | ------------------------------------------------------- |
| **NFC-app**      | Kotlin, Jetpack Compose, Hilt, DataStore | Receiver's Android app; emulates an NFC tag over HCE    |
| **NFC-redirect** | Node.js, Vercel Edge Runtime             | Translates an HTTPS GET into a `upi://pay` 302 redirect |

---

## Components

### NFC-app

The Android application. Full details in [NFC-app/README.md](NFC-app/README.md).

- Stores the receiver's UPI ID and display name using Jetpack DataStore.
- When activated, starts a `HostApduService` that emulates a Type 4 NFC tag using the NFC Forum NDEF APDU handshake.
- The NDEF record contains an HTTPS deep link to the redirect server, parameterised with the UPI ID, name, optional amount, and note.
- Built with Kotlin 2.1, Compose BOM 2025.01, Hilt 2.55, min SDK 24.

### NFC-redirect

The serverless redirect. Full details in [NFC-redirect/README.md](NFC-redirect/README.md).

- A single Vercel Edge Function (`api/index.js`).
- Accepts `pa`, `pn`, `am`, `cu`, `tn`, `tr` query parameters (standard NPCI UPI fields).
- Validates that `pa` (payee address / UPI ID) is present, then constructs and returns a `302 Found` to the assembled `upi://pay` URI.
- Deployed at the URL you configure after following the Quick Start steps.

---

## Prerequisites

| Requirement       | Detail                                                            |
| ----------------- | ----------------------------------------------------------------- |
| Receiver's device | Android 7.0+ (API 24), NFC chip with HCE support                  |
| Payer's device    | Any Android device with a UPI app installed; NFC **not** required |
| Redirect server   | Vercel account (free tier is sufficient)                          |

---

## Quick Start

### 1. Deploy the redirect server

```bash
cd NFC-redirect
npm install -g vercel   # if not already installed
vercel --prod
```

Note the deployment URL (e.g. `https://your-project.vercel.app`).

### 2. Point the Android app at your deployment

In `NFC-app/app/src/main/kotlin/com/nfcupi/pay/nfc/UpiDeepLinkBuilder.kt`, update the base URL:

```kotlin
var uri = "https://<your-deployment>.vercel.app/api?pa=..."
```

### 3. Build and install the Android app

```bash
cd NFC-app
./gradlew installDebug
```

### 4. Use it

1. Open Tapme on the receiver's Android phone.
2. Go to **Settings**, enter your UPI ID (e.g. `yourname@upi`) and display name.
3. Back on the main screen, optionally enter an amount and note.
4. Tap **Activate**.
5. The receiver holds their phone against the payer's phone (back-to-back, near the NFC antenna).
6. The payer's phone shows the UPI app chooser — they pick their app and confirm payment.

---

## Security Notes

- The redirect server never stores or logs UPI IDs or payment details beyond the single request/response cycle.
- No payment data is persisted anywhere in this system; Tapme only constructs and relays standard UPI deep links.
- The `upi://` URI format is defined by NPCI and is the same format used by QR-code-based UPI payments.
- The redirect server validates the presence of `pa` and returns a `400` if it is missing — all other parameters are optional per the UPI spec.

---

## Limitations

- HCE emulation requires the Tapme app to be in the foreground (or set as the preferred service). If the screen locks, the emulated tag stops responding.
- The payer must have at least one UPI-capable app installed; the chooser will be empty otherwise.
- iOS devices cannot read HCE tags emitted by Android (Apple's NFC stack does not support this interaction).

---

## License

This repository is licensed under the GNU Affero General Public License v3.0 or later.

See [LICENSE](LICENSE) for the full text.
