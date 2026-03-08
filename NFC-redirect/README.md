# NFC-redirect

A minimal Vercel Edge Function that bridges NFC-triggered HTTPS requests to the native Android UPI deep-link scheme.

---

## Why This Exists

Android's NFC stack dispatches NDEF URI records differently depending on the URI scheme:

- A `upi://pay` URI in an NFC tag is handled via `ACTION_NDEF_DISCOVERED`. Only apps that explicitly declare an NFC tag intent filter for that scheme will respond — most UPI apps do **not** register one.
- An `https://` URI in an NFC tag is always handled by opening the browser.

This server exploits that difference: the NFC tag contains an HTTPS URL, Android opens it in a browser, and this function returns a `302 Found` redirect to the `upi://pay` URI. The browser follows the redirect, Android sees the `upi://` scheme, and the native UPI app chooser (GPay, PhonePe, Paytm, etc.) appears — exactly as if the user had scanned a QR code.

---

## How It Works

```
Payer's browser         NFC-redirect (Vercel Edge)
──────────────          ────────────────────────────────────────
GET /api
  ?pa=name@upi
  &pn=Display+Name      Parse query params
  &am=67                Validate pa is present
  &cu=INR               Build: upi://pay?pa=name@upi&pn=...&am=67&cu=INR&tn=...
  &tn=Payment+note  ──▶ Return HTTP 302 → upi://pay?...
                    ◀──
  Follows redirect
  Browser hands upi://
  to Android intent
  resolver ─────────▶   Native UPI app chooser opens
```

---

## API

### `GET /api`

These are based on the UPI deep link parameters.

| Parameter | Required | Description                                   |
| --------- | -------- | --------------------------------------------- |
| `pa`      | **Yes**  | Payee UPI ID (e.g. `yourname@upi`)            |
| `pn`      | No       | Payee display name                            |
| `am`      | No       | Amount in INR (decimal, e.g. `50` or `49.99`) |
| `cu`      | No       | Currency code — defaults to `INR`             |
| `tn`      | No       | Transaction note                              |
| `tr`      | No       | Transaction reference ID                      |

**Success:** `302 Found` with `Location: upi://pay?...`

**Error:** `400 Bad Request` — plain text `Missing payee address (pa) parameter.`

### Example

```
GET https://your-redirect-server.example.com/api?pa=aditya%40upi&pn=Aditya&am=100&tn=Lunch
```

Redirects to:

```
upi://pay?pa=aditya@upi&pn=Aditya&am=100&tn=Lunch&cu=INR
```

---

## Project Structure

```
NFC-redirect/
├── api/
│   └── index.js    # The Edge Function
└── package.json
```

### `api/index.js`

A single default-export handler using the Vercel Edge Runtime (`runtime: "edge"`). It runs at the edge (close to the user) with sub-millisecond cold starts and no Node.js process overhead.

```js
export const config = { runtime: "edge" };

export default function handler(request) {
  // 1. Parse query params from the incoming URL
  // 2. Validate pa is present
  // 3. Assemble upi://pay URI
  // 4. Return Response.redirect(uri, 302)
}
```

---

## Deployment

### Prerequisites

- [Vercel CLI](https://vercel.com/docs/cli) installed (`npm install -g vercel`)
- A Vercel account (free tier is sufficient)

### Deploy

```bash
cd NFC-redirect
vercel --prod
```

Vercel automatically detects the `api/` directory and deploys `index.js` as a serverless edge function at `<your-deployment-url>/api`.

### Local Development

```bash
vercel dev
```

Then test with:

```bash
curl -v "http://localhost:3000/api?pa=test@upi&pn=Test&am=10"
```

---

## Encoding Notes

- The `@` character in UPI IDs is left unencoded in the final `upi://` URI (e.g. `pa=aditya@upi`). The function strips the `%40` encoding produced by `encodeURIComponent` for `@` specifically, matching the format expected by UPI apps.
- All other parameters are passed through `encodeURIComponent` to prevent injection into the URI.

---

## Security

- No data is persisted. The function is stateless and processes one request at a time at the edge.
- The `pa` parameter is validated for presence only. UPI ID format validation is left to the receiving UPI app.
- The function does not make any outbound network requests — it only reads the incoming URL and returns a redirect.
