package com.nfcupi.pay.nfc

import android.net.Uri

object UpiDeepLinkBuilder {
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
        redirectBaseUrl: String,
        upiId: String,
        payeeName: String,
        amount: String? = null,
        transactionNote: String? = null
    ): String {
        require(upiId.contains("@")) { "Invalid UPI ID: must contain @" }
        val normalizedRedirectBaseUrl = normalizeRedirectBaseUrl(redirectBaseUrl)

        val actualNote = if (!transactionNote.isNullOrBlank()) {
            transactionNote.trim()
        } else {
            if (amount != null) {
                "Tapme Payment of $amount to $payeeName"
            } else {
                "Tapme Payment to $payeeName"
            }
        }

        var uri = "$normalizedRedirectBaseUrl?pa=${Uri.encode(upiId.trim(), "@")}" +
            "&pn=${Uri.encode(payeeName.trim().ifBlank { "Pay" })}"

        if (!amount.isNullOrBlank() && amount.toDoubleOrNull() != null) {
            uri += "&am=${Uri.encode(amount.trim())}"
        }

        uri += "&cu=INR&tn=${Uri.encode(actualNote)}"

        return uri
    }

    fun normalizeRedirectBaseUrl(input: String?): String {
        val trimmedInput = input?.trim().orEmpty()
        require(trimmedInput.isNotBlank()) { "Redirect URL is required" }

        val withScheme = if (trimmedInput.contains("://")) {
            trimmedInput
        } else {
            "https://$trimmedInput"
        }

        val parsed = Uri.parse(withScheme)
        require(parsed.scheme == "https" || parsed.scheme == "http") {
            "Redirect URL must use http or https"
        }
        require(!parsed.host.isNullOrBlank()) { "Redirect URL must include a host" }
        require(parsed.query.isNullOrBlank()) { "Redirect URL must not include a query string" }
        require(parsed.fragment.isNullOrBlank()) { "Redirect URL must not include a fragment" }

        val normalizedPath = parsed.path
            ?.trim()
            ?.trimEnd('/')
            ?.ifBlank { "/api" }
            ?: "/api"

        return parsed.buildUpon()
            .encodedQuery(null)
            .fragment(null)
            .path(normalizedPath)
            .build()
            .toString()
            .trimEnd('/')
    }
}
