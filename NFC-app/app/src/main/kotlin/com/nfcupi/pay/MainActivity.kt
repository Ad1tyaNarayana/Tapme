package com.nfcupi.pay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nfcupi.pay.ui.navigation.AppNavGraph
import com.nfcupi.pay.ui.theme.NfcUpiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NfcUpiTheme { AppNavGraph() } }
    }

    override fun onResume() {
        super.onResume()
        try {
            val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this)
            if (nfcAdapter != null) {
                val cardEmulation = android.nfc.cardemulation.CardEmulation.getInstance(nfcAdapter)
                val componentName = android.content.ComponentName(this, com.nfcupi.pay.nfc.UpiNfcHceService::class.java)
                cardEmulation.setPreferredService(this, componentName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this)
            if (nfcAdapter != null) {
                val cardEmulation = android.nfc.cardemulation.CardEmulation.getInstance(nfcAdapter)
                cardEmulation.unsetPreferredService(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
