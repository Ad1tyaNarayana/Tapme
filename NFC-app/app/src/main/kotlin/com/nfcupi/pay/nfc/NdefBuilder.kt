package com.nfcupi.pay.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord

object NdefBuilder {
    /**
     * Converts a URI string into a raw NDEF message byte array.
     * A URI record makes Android dispatch ACTION_NDEF_DISCOVERED with the URI in the intent data.
     * Whether a receiving app opens depends on that app declaring NFC tag intent filters for the
     * URI scheme; normal ACTION_VIEW deep-link support alone is not enough.
     */
    fun buildNdefMessage(uri: String): ByteArray {
        if (uri.isBlank()) return byteArrayOf()

        // Passing the upi://pay URI directly. NdefRecord.createUri() handles any URI scheme.
        val record = NdefRecord.createUri(Uri.parse(uri))
        
        return NdefMessage(arrayOf(record)).toByteArray()
    }
}
