export const config = {
  runtime: "edge",
};

export default function handler(request) {
  // Extract parameters passed in the URL (e.g. ?pa=xxx@banksi&pn=aditya&am=50&tn=nfc)
  const url = new URL(request.url);
  const searchParams = url.searchParams;

  const pa = searchParams.get("pa");
  const pn = searchParams.get("pn");
  const am = searchParams.get("am");
  const cu = searchParams.get("cu");
  const tn = searchParams.get("tn");
  const tr = searchParams.get("tr");

  // We MUST have a payee address (UPI ID). If not, just show a plain error.
  if (!pa) {
    return new Response("Missing payee address (pa) parameter.", {
      status: 400,
    });
  }

  // Construct the deep link ensuring we don't double encode what's already safe
  // Vercel parses the query params into unencoded strings, so we must encode them
  // for the UPI URI format.
  let uri = `upi://pay?pa=${encodeURIComponent(pa).replace("%40", "@")}`;

  if (pn) uri += `&pn=${encodeURIComponent(pn)}`;
  if (am) uri += `&am=${encodeURIComponent(am)}`;
  if (tn) uri += `&tn=${encodeURIComponent(tn)}`;
  if (tr) uri += `&tr=${encodeURIComponent(tr)}`;

  uri += `&cu=${cu || "INR"}`;

  // Send a 302 Found redirect back to the phone's browser
  // The phone browser will immediately attempt to load this "upi://pay..." URL,
  // triggering the Android App Chooser for GPay/Paytm natively!
  return Response.redirect(uri, 302);
}
