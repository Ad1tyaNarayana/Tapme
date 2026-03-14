package com.nfcupi.pay.ui.screens.receive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcupi.pay.data.PreferencesRepository
import com.nfcupi.pay.data.TransactionFilter
import com.nfcupi.pay.data.TransactionHistoryRepository
import com.nfcupi.pay.data.TransactionRecord
import com.nfcupi.pay.data.TransactionSortOption
import com.nfcupi.pay.data.TransactionStatus
import com.nfcupi.pay.nfc.UpiDeepLinkBuilder
import com.nfcupi.pay.nfc.UpiNfcHceService
import com.nfcupi.pay.util.NfcState
import com.nfcupi.pay.util.getNfcState
import com.nfcupi.pay.util.hasHceSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val AMOUNT_CAP_ERROR_MESSAGE = "Amount cannot exceed 1 Lakh (Rs 1,00,000)"

data class ReceiveUiState(
    val isActive: Boolean = false,
    val amount: String = "",
    val amountErrorMessage: String? = null,
    val upiId: String = "",
    val displayName: String = "",
    val nfcState: NfcState = NfcState.NOT_SUPPORTED,
    val hasHce: Boolean = false,
    val upiUri: String = "",     // Direct UPI URI (used for both NFC and QR)
    val transactionNote: String = "",
    val historyTransactions: List<TransactionRecord> = emptyList(),
    val hasHistory: Boolean = false,
    val latestPendingTransaction: TransactionRecord? = null,
    val historyFilter: TransactionFilter = TransactionFilter.ALL,
    val historySort: TransactionSortOption = TransactionSortOption.LATEST_FIRST,
    val hasNotificationAccess: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefsRepo: PreferencesRepository,
    private val transactionHistoryRepository: TransactionHistoryRepository
) : ViewModel() {

    companion object {
        private const val MAX_AMOUNT = 100000.0
        private val AMOUNT_INPUT_REGEX = Regex("^\\d{0,6}(\\.\\d{0,2})?$")
    }

    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()

    private var allTransactions: List<TransactionRecord> = emptyList()
    private var armedTransaction: TransactionRecord? = null
    private val sessionConsumedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == UpiNfcHceService.ACTION_SESSION_CONSUMED) {
                handleSessionConsumed()
            }
        }
    }
    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                refreshNfcState()
            }
        }
    }

    init {
        val sessionConsumedFilter = IntentFilter(UpiNfcHceService.ACTION_SESSION_CONSUMED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                sessionConsumedReceiver,
                sessionConsumedFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(sessionConsumedReceiver, sessionConsumedFilter)
        }

        val nfcStateFilter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                nfcStateReceiver,
                nfcStateFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(nfcStateReceiver, nfcStateFilter)
        }

        refreshNfcState()
        refreshPermissionState()

        viewModelScope.launch {
            prefsRepo.userProfile.collect { profile ->
                _uiState.update {
                    it.copy(
                        upiId = profile.upiId,
                        displayName = profile.displayName
                    )
                }
            }
        }

        viewModelScope.launch {
            transactionHistoryRepository.transactions.collect { transactions ->
                allTransactions = transactions
                applyHistoryPresentation()
            }
        }
    }

    fun onAmountChange(amount: String) {
        if (amount.isEmpty()) {
            _uiState.update {
                it.copy(
                    amount = amount,
                    amountErrorMessage = null,
                    errorMessage = null,
                    statusMessage = null
                )
            }
            return
        }

        if (!amount.matches(AMOUNT_INPUT_REGEX)) {
            return
        }

        val parsedAmount = amount.toDoubleOrNull()
        if (parsedAmount == null) {
            _uiState.update {
                it.copy(
                    amount = amount,
                    amountErrorMessage = null,
                    errorMessage = null,
                    statusMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                amount = amount,
                amountErrorMessage = if (parsedAmount <= MAX_AMOUNT) null else AMOUNT_CAP_ERROR_MESSAGE,
                errorMessage = null,
                statusMessage = null
            )
        }
    }

    fun onTransactionNoteChange(note: String) {
        _uiState.update { it.copy(transactionNote = note, statusMessage = null) }
    }

    fun startReceiving() {
        val state = _uiState.value

        if (state.upiId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Set your UPI ID in Settings first") }
            return
        }
        if (state.amount.isNotBlank() && state.amount.toDoubleOrNull() == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount") }
            return
        }
        if (!state.amount.isWithinCapIfPresent()) {
            _uiState.update { it.copy(errorMessage = AMOUNT_CAP_ERROR_MESSAGE) }
            return
        }

        val transactionReference = generateTransactionReference()
        val initiatedAt = System.currentTimeMillis()
        val resolvedNote = resolveTransactionNote(
            amount = state.amount,
            displayName = state.displayName,
            upiId = state.upiId,
            enteredNote = state.transactionNote
        )

        val uri = try {
            UpiDeepLinkBuilder.build(
                upiId = state.upiId,
                payeeName = state.displayName,
                transactionReference = transactionReference,
                amount = state.amount.ifBlank { null },
                transactionNote = resolvedNote
            )
        } catch (_: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = "Invalid UPI ID") }
            return
        }

        val tx = TransactionRecord(
            id = transactionReference,
            upiId = state.upiId,
            displayName = state.displayName.ifBlank { state.upiId },
            amount = state.amount,
            note = resolvedNote,
            status = TransactionStatus.PENDING,
            initiatedAt = initiatedAt,
            lastUpdatedAt = initiatedAt
        )
        armedTransaction = tx
        
        viewModelScope.launch {
            transactionHistoryRepository.addInitiatedTransaction(tx)
        }

        if (state.nfcState == NfcState.SUPPORTED_AND_ENABLED) {
            UpiNfcHceService.currentUpiUri = uri
            context.startService(Intent(context, UpiNfcHceService::class.java))
        }
        _uiState.update {
            it.copy(
                isActive = true,
                upiUri = uri,
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun stopReceiving() {
        armedTransaction = null
        UpiNfcHceService.currentUpiUri = ""
        context.stopService(Intent(context, UpiNfcHceService::class.java))
        _uiState.update { it.copy(isActive = false, upiUri = "", statusMessage = null) }
    }

    fun onHistoryFilterChange(filter: TransactionFilter) {
        _uiState.update { it.copy(historyFilter = filter, statusMessage = null, errorMessage = null) }
        applyHistoryPresentation()
    }

    fun onHistorySortChange(sort: TransactionSortOption) {
        _uiState.update { it.copy(historySort = sort, statusMessage = null, errorMessage = null) }
        applyHistoryPresentation()
    }

    fun markTransactionAs(status: TransactionStatus, transactionId: String) {
        viewModelScope.launch {
            transactionHistoryRepository.updateTransactionStatus(transactionId, status)
            _uiState.update {
                it.copy(
                    statusMessage = when (status) {
                        TransactionStatus.SUCCESS -> "Transaction marked successful"
                        TransactionStatus.FAILED -> "Transaction marked failed"
                        TransactionStatus.PENDING -> "Transaction marked pending"
                    },
                    errorMessage = null
                )
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionHistoryRepository.deleteTransaction(transactionId)
            _uiState.update {
                it.copy(
                    statusMessage = "Transaction deleted",
                    errorMessage = null
                )
            }
        }
    }

    fun clearAllTransactions() {
        if (_uiState.value.isActive) {
            _uiState.update {
                it.copy(
                    errorMessage = "Stop the current receive session before clearing history",
                    statusMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            transactionHistoryRepository.clearTransactions()
            _uiState.update {
                it.copy(
                    statusMessage = "Transaction history cleared",
                    errorMessage = null
                )
            }
        }
    }

    fun onAppResumed() {
        refreshMainState()
    }

    fun refreshMainState() {
        refreshNfcState()
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        _uiState.update {
            it.copy(
                hasNotificationAccess = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
            )
        }
    }

    fun refreshNfcState() {
        val latestNfcState = getNfcState(context)
        val latestHceSupport = hasHceSupport(context)

        _uiState.update { state ->
            state.copy(
                nfcState = latestNfcState,
                hasHce = latestHceSupport
            )
        }
    }


    override fun onCleared() {
        context.unregisterReceiver(sessionConsumedReceiver)
        context.unregisterReceiver(nfcStateReceiver)
        stopReceiving()
        super.onCleared()
    }

    private fun handleSessionConsumed() {
        armedTransaction = null

        _uiState.update {
            it.copy(
                isActive = false,
                upiUri = "",
                statusMessage = "Tap complete. Transaction is now being tracked.",
                errorMessage = null
            )
        }
    }

    private fun applyHistoryPresentation() {
        val state = _uiState.value
        val visibleTransactions = allTransactions
            .filterBy(state.historyFilter)
            .sortBy(state.historySort)

        _uiState.update {
            it.copy(
                historyTransactions = visibleTransactions,
                hasHistory = allTransactions.isNotEmpty(),
                latestPendingTransaction = allTransactions
                    .filter { transaction -> transaction.status == TransactionStatus.PENDING }
                    .maxByOrNull { transaction -> transaction.initiatedAt }
            )
        }
    }

    private fun List<TransactionRecord>.filterBy(filter: TransactionFilter): List<TransactionRecord> {
        return when (filter) {
            TransactionFilter.ALL -> this
            TransactionFilter.PENDING -> filter { it.status == TransactionStatus.PENDING }
            TransactionFilter.SUCCESS -> filter { it.status == TransactionStatus.SUCCESS }
            TransactionFilter.FAILED -> filter { it.status == TransactionStatus.FAILED }
        }
    }

    private fun List<TransactionRecord>.sortBy(sort: TransactionSortOption): List<TransactionRecord> {
        return when (sort) {
            TransactionSortOption.LATEST_FIRST -> sortedByDescending { it.initiatedAt }
            TransactionSortOption.OLDEST_FIRST -> sortedBy { it.initiatedAt }
            TransactionSortOption.AMOUNT_HIGH_TO_LOW -> sortedByDescending { it.amount.toDoubleOrNull() ?: -1.0 }
            TransactionSortOption.AMOUNT_LOW_TO_HIGH -> sortedBy { it.amount.toDoubleOrNull() ?: Double.MAX_VALUE }
            TransactionSortOption.STATUS -> sortedWith(
                compareBy<TransactionRecord> { statusPriority(it.status) }
                    .thenByDescending { it.initiatedAt }
            )
        }
    }

    private fun statusPriority(status: TransactionStatus): Int {
        return when (status) {
            TransactionStatus.PENDING -> 0
            TransactionStatus.FAILED -> 1
            TransactionStatus.SUCCESS -> 2
        }
    }

    private fun resolveTransactionNote(
        amount: String,
        displayName: String,
        upiId: String,
        enteredNote: String
    ): String {
        if (enteredNote.isNotBlank()) {
            return enteredNote.trim()
        }

        val receiverName = displayName.ifBlank { upiId }
        return if (amount.isNotBlank()) {
            "Tapme Payment of $amount to $receiverName"
        } else {
            "Tapme Payment to $receiverName"
        }
    }

    private fun generateTransactionReference(): String = "TM${System.currentTimeMillis()}"

    private fun String.isWithinCapIfPresent(): Boolean {
        if (isBlank()) return true
        return isWithinAmountCap(this)
    }

    private fun isWithinAmountCap(amount: String): Boolean {
        return amount.toDoubleOrNull()?.let { it <= MAX_AMOUNT } ?: false
    }
}
