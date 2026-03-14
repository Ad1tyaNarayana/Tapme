package com.nfcupi.pay.nfc

import android.net.Uri

object UpiDeepLinkBuilder {
    /**
     * Builds a standard NPCI UPI deep link.
     * Format: upi://pay?pa=<upiId>&pn=<name>&am=<amount>&cu=INR&tn=<note>&tr=<ref>
     *
     * pa = payee address (UPI ID)
     * pn = payee name (shown on payment screen)
     * am = amount (if null, payer's app shows amount input field)
     * cu = currency (always INR)
     * tn = transaction note
     * tr = transaction reference (ties the UPI transaction to our internal record)
     */
    fun build(
        upiId: String,
        payeeName: String,
        transactionReference: String,
        amount: String? = null,
        transactionNote: String? = null
    ): String {
        require(upiId.contains("@")) { "Invalid UPI ID: must contain @" }
        val resolvedPayeeName = payeeName.trim().ifBlank { "Pay" }

        val actualNote = if (!transactionNote.isNullOrBlank()) {
            transactionNote.trim()
        } else {
            if (amount != null) {
                "Tapme Payment of $amount to $resolvedPayeeName"
            } else {
                "Tapme Payment to $resolvedPayeeName"
            }
        }

        val encodedParams = mutableListOf(
            "pa=${Uri.encode(upiId.trim(), "@")}",
            "pn=${Uri.encode(resolvedPayeeName)}"
        )

        if (!amount.isNullOrBlank() && amount.toDoubleOrNull() != null) {
            encodedParams += "am=${Uri.encode(amount.trim())}"
        }

        encodedParams += "cu=INR"
        encodedParams += "tn=${Uri.encode(actualNote)}"
        encodedParams += "tr=${Uri.encode(transactionReference)}"

        return "upi://pay?${encodedParams.joinToString("&")}"
    }
}
