package com.nfcupi.pay.util

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter

enum class NfcState {
    SUPPORTED_AND_ENABLED,
    SUPPORTED_BUT_DISABLED,
    NOT_SUPPORTED
}

fun getNfcState(context: Context): NfcState {
    val hasNfc = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    if (!hasNfc) return NfcState.NOT_SUPPORTED
    val adapter = NfcAdapter.getDefaultAdapter(context)
    return if (adapter?.isEnabled == true) NfcState.SUPPORTED_AND_ENABLED
    else NfcState.SUPPORTED_BUT_DISABLED
}

fun hasHceSupport(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
