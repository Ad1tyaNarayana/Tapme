package com.nfcupi.pay.nfc

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

/**
 * Core of the app. Makes the phone respond to NFC readers like a physical NDEF tag.
 *
 * When active, a payer tapping their phone triggers this APDU handshake sequence:
 * 1. Reader sends SELECT AID -> we confirm we are an NDEF tag app
 * 2. Reader sends SELECT CC FILE -> we confirm
 * 3. Reader sends READ BINARY for CC -> we return capability container (describes our tag)
 * 4. Reader sends SELECT NDEF FILE -> we confirm
 * 5. Reader sends READ BINARY for NDEF -> we return NDEF message (contains UPI URI)
 *
 * The payer's OS reads the URI record and dispatches an NFC tag intent based on the
 * first NDEF record. Only apps that handle the matching NFC intent will open; many
 * UPI apps handle upi:// deep links from ACTION_VIEW but do not register for NFC tag
 * discovery, so opening an arbitrary UPI app directly is not guaranteed.
 */
class UpiNfcHceService : HostApduService() {

    companion object {
        private const val TAG = "UpiNfcHceService"

        // Standard NDEF Tag Application AID
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        private val SELECT_OK_SW   = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val UNKNOWN_CMD_SW = byteArrayOf(0x00.toByte(), 0x00.toByte())
        private val FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // File IDs for NDEF tag structure (Type 4 Tag spec)
        private val CC_FILE_ID   = byteArrayOf(0xE1.toByte(), 0x03.toByte()) // Capability Container
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte()) // NDEF data

        // Set this before starting the service. Thread-safe via @Volatile.
        @Volatile
        var currentUpiUri: String = ""
    }

    private var selectedFileId: ByteArray? = null
    private var ndefMessageBytes: ByteArray = byteArrayOf()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ndefMessageBytes = NdefBuilder.buildNdefMessage(currentUpiUri)
        Log.d(TAG, "HCE ready. URI: $currentUpiUri | NDEF bytes: ${ndefMessageBytes.size}")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "APDU: ${apdu.toHex()}")

        return when {
            isSelectAid(apdu) -> {
                selectedFileId = null
                Log.d(TAG, "-> SELECT AID OK")
                SELECT_OK_SW
            }
            isSelectFile(apdu, CC_FILE_ID) -> {
                selectedFileId = CC_FILE_ID
                Log.d(TAG, "-> SELECT CC FILE OK")
                SELECT_OK_SW
            }
            isSelectFile(apdu, NDEF_FILE_ID) -> {
                selectedFileId = NDEF_FILE_ID
                Log.d(TAG, "-> SELECT NDEF FILE OK")
                SELECT_OK_SW
            }
            isReadBinary(apdu) -> handleReadBinary(apdu)
            else -> {
                Log.w(TAG, "-> Unknown APDU: ${apdu.toHex()}")
                UNKNOWN_CMD_SW
            }
        }
    }

    private fun handleReadBinary(apdu: ByteArray): ByteArray {
        val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
        val length = apdu[4].toInt() and 0xFF

        return when {
            selectedFileId.contentEq(CC_FILE_ID) -> {
                buildCapabilityContainer(ndefMessageBytes.size).slice(offset, length)
            }
            selectedFileId.contentEq(NDEF_FILE_ID) -> {
                // Prefix NDEF message with 2-byte length (Type 4 Tag spec)
                val len = ndefMessageBytes.size
                val withLen = byteArrayOf(
                    ((len shr 8) and 0xFF).toByte(),
                    (len and 0xFF).toByte()
                ) + ndefMessageBytes
                withLen.slice(offset, length)
            }
            else -> FILE_NOT_FOUND
        }
    }

    private fun buildCapabilityContainer(ndefSize: Int): ByteArray {
        val maxSize = ((ndefSize + 2 + 0xFF) and 0xFF00)
        return byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0x3B,
            0x00, 0x34,
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            ((maxSize shr 8) and 0xFF).toByte(),
            (maxSize and 0xFF).toByte(),
            0x00,           // Read access: open
            0xFF.toByte()   // Write access: none
        )
    }

    override fun onDeactivated(reason: Int) {
        selectedFileId = null
        Log.d(TAG, "HCE deactivated (reason=$reason)")
    }

    // APDU parsing helpers
    private fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < 6) return false
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte() || apdu[2] != 0x04.toByte()) return false
        val aidLen = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + aidLen) return false
        return apdu.slice(5 until 5 + aidLen).toByteArray().contentEquals(NDEF_AID)
    }

    private fun isSelectFile(apdu: ByteArray, fileId: ByteArray): Boolean {
        if (apdu.size < 7) return false
        if (apdu[1] != 0xA4.toByte() || apdu[2] != 0x00.toByte()) return false
        return apdu.slice(5 until 7).toByteArray().contentEquals(fileId)
    }

    private fun isReadBinary(apdu: ByteArray) = apdu.size >= 5 && apdu[1] == 0xB0.toByte()

    private fun ByteArray.slice(offset: Int, length: Int): ByteArray {
        val end = minOf(offset + length, this.size)
        return if (offset >= this.size) FILE_NOT_FOUND
        else this.copyOfRange(offset, end) + SELECT_OK_SW
    }

    private fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }
    private fun ByteArray?.contentEq(other: ByteArray?) =
        this != null && other != null && this.contentEquals(other)
}
