package com.nfcupi.pay.nfc

import android.net.Uri

object UpiDeepLinkBuilder {
    /**
     * Base URL of your deployed NFC-redirect server.
     * Deploy NFC-redirect (see NFC-redirect/README.md) and replace this
     * with your own server URL before building the app.
     */
    private const val REDIRECT_BASE_URL = "https://your-redirect-server.example.com/api"

    /**
     * Builds a standard NPCI UPI deep link.
     * Format: upi://pay?pa=<upiId>&pn=<name>&am=<amount>&cu=INR&tn=<note>
     *
     * pa = payee address (UPI ID)
     * pn = payee name (shown on payment screen)
     * am = amount (if null, payer's app shows amount input field)
     * cu = currency (always INR)
     * tn = transaction note
     *
     * All major UPI apps handle this URI scheme natively.
     */
    fun build(
        upiId: String,
        payeeName: String,
        amount: String? = null,
        transactionNote: String? = null
    ): String {
        require(upiId.contains("@")) { "Invalid UPI ID: must contain @" }

        val actualNote = if (!transactionNote.isNullOrBlank()) {
            transactionNote.trim()
        } else {
            if (amount != null) {
                "Tapme Payment of $amount to $payeeName"
            } else {
                "Tapme Payment to $payeeName"
            }
        }

        var uri = "$REDIRECT_BASE_URL?pa=${Uri.encode(upiId.trim(), "@")}" +
            "&pn=${Uri.encode(payeeName.trim().ifBlank { "Pay" })}"

        if (!amount.isNullOrBlank() && amount.toDoubleOrNull() != null) {
            uri += "&am=${Uri.encode(amount.trim())}"
        }

        uri += "&cu=INR&tn=${Uri.encode(actualNote)}"

        return uri
    }
}
