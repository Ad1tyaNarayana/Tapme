package com.nfcupi.pay.ui.screens.receive

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcupi.pay.data.PreferencesRepository
import com.nfcupi.pay.nfc.UpiDeepLinkBuilder
import com.nfcupi.pay.nfc.UpiNfcHceService
import com.nfcupi.pay.util.NfcState
import com.nfcupi.pay.util.getNfcState
import com.nfcupi.pay.util.hasHceSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiveUiState(
    val isActive: Boolean = false,
    val amount: String = "",
    val upiId: String = "",
    val displayName: String = "",
    val redirectBaseUrl: String = "",
    val nfcState: NfcState = NfcState.NOT_SUPPORTED,
    val hasHce: Boolean = false,
    val upiUri: String = "",
    val transactionNote: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                nfcState = getNfcState(context),
                hasHce   = hasHceSupport(context)
            )
        }
        viewModelScope.launch {
            prefsRepo.userProfile.collect { profile ->
                _uiState.update {
                    it.copy(
                        upiId = profile.upiId,
                        displayName = profile.displayName,
                        redirectBaseUrl = profile.redirectBaseUrl
                    )
                }
            }
        }
    }

    fun onAmountChange(amount: String) {
        // Validate: digits only, max 6 digits, optional 2 decimal places
        if (amount.isEmpty() || amount.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?\$"))) {
            _uiState.update { it.copy(amount = amount, errorMessage = null) }
        }
    }

    fun onTransactionNoteChange(note: String) {
        _uiState.update { it.copy(transactionNote = note) }
    }

    fun activateNfc() {
        val state = _uiState.value

        if (state.upiId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Set your UPI ID in Settings first") }
            return
        }
        if (state.nfcState != NfcState.SUPPORTED_AND_ENABLED) {
            _uiState.update { it.copy(errorMessage = "Enable NFC in Settings -> Connected devices -> NFC") }
            return
        }

        val uri = try {
            UpiDeepLinkBuilder.build(
                redirectBaseUrl = state.redirectBaseUrl,
                upiId = state.upiId,
                payeeName = state.displayName,
                amount = state.amount.ifBlank { null },
                transactionNote = state.transactionNote.ifBlank { null }
            )
        } catch (_: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = "Configure your redirect site URL in Settings") }
            return
        }

        UpiNfcHceService.currentUpiUri = uri
        context.startService(Intent(context, UpiNfcHceService::class.java))
        _uiState.update { it.copy(isActive = true, upiUri = uri, errorMessage = null) }
    }

    fun deactivateNfc() {
        context.stopService(Intent(context, UpiNfcHceService::class.java))
        UpiNfcHceService.currentUpiUri = ""
        _uiState.update { it.copy(isActive = false, upiUri = "") }
    }

    override fun onCleared() {
        super.onCleared()
        deactivateNfc()
    }
}
