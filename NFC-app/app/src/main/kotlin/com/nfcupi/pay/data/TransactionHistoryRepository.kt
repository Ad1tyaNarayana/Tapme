package com.nfcupi.pay.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private val Context.transactionHistoryDataStore: DataStore<Preferences> by preferencesDataStore("transaction_history")

enum class TransactionStatus {
    SUCCESS,
    FAILED,
    PENDING
}

enum class TransactionFilter {
    ALL,
    PENDING,
    SUCCESS,
    FAILED
}

enum class TransactionSortOption {
    LATEST_FIRST,
    OLDEST_FIRST,
    AMOUNT_HIGH_TO_LOW,
    AMOUNT_LOW_TO_HIGH,
    STATUS
}

enum class TransactionUpdateSource {
    APP,
    NOTIFICATION,
    MANUAL
}

data class TransactionRecord(
    val id: String,
    val upiId: String,
    val displayName: String,
    val amount: String = "",
    val note: String = "",
    val status: TransactionStatus = TransactionStatus.PENDING,
    val initiatedAt: Long,
    val lastUpdatedAt: Long = initiatedAt,
    val updatedFrom: TransactionUpdateSource = TransactionUpdateSource.APP,
    val statusNote: String = "Awaiting SMS or notification confirmation"
)

private data class TransactionSignal(
    val status: TransactionStatus,
    val amount: Double?,
    val timestamp: Long,
    val source: TransactionUpdateSource,
    val rawText: String,
    val summary: String,
    val isFromUpiApp: Boolean = false
)

private data class SignalApplicationResult(
    val transactions: List<TransactionRecord>,
    val matchedCount: Int
)

@Singleton
class TransactionHistoryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val transactionsKey = stringPreferencesKey("transactions_json")

    val transactions: Flow<List<TransactionRecord>> = context.transactionHistoryDataStore.data.map { prefs ->
        decodeTransactions(prefs[transactionsKey])
    }

    suspend fun addInitiatedTransaction(transaction: TransactionRecord) {
        updateTransactions { current ->
            listOf(
                transaction.copy(
                    status = TransactionStatus.PENDING,
                    lastUpdatedAt = transaction.initiatedAt,
                    updatedFrom = TransactionUpdateSource.APP,
                    statusNote = "Waiting for payment confirmation"
                )
            ) + current.filterNot { it.id == transaction.id }
        }
    }

    suspend fun updateTransactionStatus(
        id: String,
        status: TransactionStatus,
        statusNote: String = manualStatusNote(status)
    ) {
        updateTransactions { current ->
            current.map { transaction ->
                if (transaction.id != id) {
                    transaction
                } else {
                    transaction.copy(
                        status = status,
                        lastUpdatedAt = System.currentTimeMillis(),
                        updatedFrom = TransactionUpdateSource.MANUAL,
                        statusNote = statusNote
                    )
                }
            }
        }
    }

    suspend fun deleteTransaction(id: String) {
        updateTransactions { current -> current.filterNot { it.id == id } }
    }

    suspend fun clearTransactions() {
        updateTransactions { emptyList() }
    }


    suspend fun ingestNotificationSignal(
        packageName: String,
        title: String?,
        text: String?,
        postedAt: Long
    ) {
        val signal = TransactionSignalParser.fromNotification(
            packageName = packageName,
            title = title,
            text = text,
            timestamp = postedAt
        ) ?: return

        updateTransactions { current ->
            applySignals(current, listOf(signal)).transactions
        }
    }

    private suspend fun updateTransactions(transform: (List<TransactionRecord>) -> List<TransactionRecord>) {
        context.transactionHistoryDataStore.edit { prefs ->
            val current = decodeTransactions(prefs[transactionsKey])
            val updated = transform(current)
                .distinctBy { it.id }
                .sortedByDescending { it.initiatedAt }
                .take(MAX_HISTORY_ITEMS)
            prefs[transactionsKey] = encodeTransactions(updated)
        }
    }

    private fun applySignals(
        current: List<TransactionRecord>,
        signals: List<TransactionSignal>
    ): SignalApplicationResult {
        val mutableTransactions = current.toMutableList()
        var matchedCount = 0

        signals.sortedByDescending { it.timestamp }.forEach { signal ->
            val targetIndex = findMatchingTransactionIndex(mutableTransactions, signal) ?: return@forEach
            val existing = mutableTransactions[targetIndex]
            if (existing.status != TransactionStatus.PENDING) {
                return@forEach
            }

            mutableTransactions[targetIndex] = existing.copy(
                status = signal.status,
                lastUpdatedAt = maxOf(existing.lastUpdatedAt, signal.timestamp),
                updatedFrom = signal.source,
                statusNote = signal.summary
            )
            matchedCount++
        }

        return SignalApplicationResult(
            transactions = mutableTransactions,
            matchedCount = matchedCount
        )
    }

    private fun findMatchingTransactionIndex(
        transactions: List<TransactionRecord>,
        signal: TransactionSignal
    ): Int? {
        // Fast path: if this is from a known UPI app and there is exactly one pending
        // transaction initiated within the last 15 minutes, match it directly. This covers
        // the common scenario where the merchant just tapped and receives a single payment
        // notification without needing amount or UPI ID to appear in the notification text.
        if (signal.isFromUpiApp) {
            val recentPending = transactions.withIndex().filter { (_, tx) ->
                tx.status == TransactionStatus.PENDING &&
                    abs(signal.timestamp - tx.initiatedAt) <= FIFTEEN_MINUTES_MS
            }
            if (recentPending.size == 1) {
                return recentPending.first().index
            }
        }

        var bestIndex: Int? = null
        var bestScore = 0

        transactions.forEachIndexed { index, transaction ->
            if (transaction.status != TransactionStatus.PENDING) {
                return@forEachIndexed
            }
            val score = scoreTransactionMatch(transaction, signal)
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }

        return if (bestScore >= MINIMUM_MATCH_SCORE) bestIndex else null
    }

    private fun scoreTransactionMatch(
        transaction: TransactionRecord,
        signal: TransactionSignal
    ): Int {
        var score = 0
        val signalText = signal.rawText.lowercase(Locale.ROOT)

        // Notifications from known UPI apps are inherently more trustworthy;
        // +2 lets a (UPI app + time match) pair reach the minimum score of 5
        // even when neither amount nor UPI ID appears in the notification text.
        if (signal.isFromUpiApp) {
            score += 2
        }

        val transactionAmount = transaction.amount.toDoubleOrNull()
        if (transactionAmount != null && signal.amount != null && abs(transactionAmount - signal.amount) < 0.01) {
            score += 5
        }

        if (transaction.upiId.isNotBlank() && signalText.contains(transaction.upiId.lowercase(Locale.ROOT))) {
            score += 3
        }

        if (transaction.displayName.isNotBlank() && signalText.contains(transaction.displayName.lowercase(Locale.ROOT))) {
            score += 1
        }

        val ageDifference = abs(signal.timestamp - transaction.initiatedAt)
        if (ageDifference <= FIFTEEN_MINUTES_MS) {
            score += 3
        } else if (ageDifference <= TWO_HOURS_MS) {
            score += 1
        }

        return score
    }

    private fun encodeTransactions(transactions: List<TransactionRecord>): String {
        val jsonArray = JSONArray()
        transactions.forEach { transaction ->
            jsonArray.put(
                JSONObject()
                    .put("id", transaction.id)
                    .put("upiId", transaction.upiId)
                    .put("displayName", transaction.displayName)
                    .put("amount", transaction.amount)
                    .put("note", transaction.note)
                    .put("status", transaction.status.name)
                    .put("initiatedAt", transaction.initiatedAt)
                    .put("lastUpdatedAt", transaction.lastUpdatedAt)
                    .put("updatedFrom", transaction.updatedFrom.name)
                    .put("statusNote", transaction.statusNote)
            )
        }
        return jsonArray.toString()
    }

    private fun decodeTransactions(json: String?): List<TransactionRecord> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            val jsonArray = JSONArray(json)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(index)
                    add(
                        TransactionRecord(
                            id = item.optString("id"),
                            upiId = item.optString("upiId"),
                            displayName = item.optString("displayName"),
                            amount = item.optString("amount"),
                            note = item.optString("note"),
                            status = item.optString("status")
                                .toEnumOrDefault(TransactionStatus.PENDING),
                            initiatedAt = item.optLong("initiatedAt"),
                            lastUpdatedAt = item.optLong("lastUpdatedAt"),
                            updatedFrom = item.optString("updatedFrom")
                                .toEnumOrDefault(TransactionUpdateSource.APP),
                            statusNote = item.optString(
                                "statusNote",
                                "Awaiting SMS or notification confirmation"
                            )
                        )
                    )
                }
            }.filter { it.id.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun manualStatusNote(status: TransactionStatus): String {
        return when (status) {
            TransactionStatus.SUCCESS -> "Marked successful by you"
            TransactionStatus.FAILED -> "Marked failed by you"
            TransactionStatus.PENDING -> "Marked pending by you"
        }
    }

    private fun <T : Enum<T>> String.toEnumOrDefault(defaultValue: T): T {
        return defaultValue.javaClass.enumConstants
            ?.firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: defaultValue
    }

    companion object {
        private const val MAX_HISTORY_ITEMS = 200
        private const val MINIMUM_MATCH_SCORE = 5
        private const val FIFTEEN_MINUTES_MS = 15 * 60 * 1000L
        private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }
}

private object TransactionSignalParser {
    // Package names of well-known Indian UPI and banking apps.
    // Notifications from these apps are given extra weight in match scoring
    // and trigger the single-pending fast-path.
    private val KNOWN_UPI_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                         // PhonePe
        "net.one97.paytm",                         // Paytm
        "in.org.npci.upiapp",                      // BHIM
        "in.amazon.mShop.android.shopping",        // Amazon Pay
        "com.axis.mobile",                         // Axis Mobile Banking
        "com.csam.icici.bank.imobile",             // ICICI iMobile
        "com.sbi.SBIFreedomPlus",                  // SBI YONO
        "com.snapwork.hdfc",                       // HDFC MobileBanking
        "com.kotak.mobile.kotak811",               // Kotak 811
        "com.freecharge.android",                  // Freecharge
        "com.mobikwik_new",                        // MobiKwik
        "com.dreamplug.androidapp",                // CRED
        "in.juspay.hyperpaysdk",                   // JusPay
    )

    private val amountRegex = Regex("(?:₹|rs\\.?|inr)\\s*([0-9]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val successKeywords = listOf(
        "received",
        "credited",
        "successful",
        "successfully",
        "payment received",
        "collect request completed",
        "money received",
        "amount received",
        "transaction complete",
        "transfer complete",
        "payment complete",
        "payment done",
        "payment success",
        "transfer successful",
        "sent successfully",
        "transaction successful"
    )
    private val failedKeywords = listOf(
        "failed",
        "failure",
        "declined",
        "unsuccessful",
        "reversed",
        "cancelled",
        "timed out",
        "timeout",
        "transaction failed",
        "payment failed",
        "could not process",
        "not processed",
        "transaction declined"
    )
    private val pendingKeywords = listOf(
        "pending",
        "processing",
        "in progress",
        "initiated",
        "awaiting"
    )

    fun fromNotification(
        packageName: String,
        title: String?,
        text: String?,
        timestamp: Long
    ): TransactionSignal? {
        val content = listOfNotNull(title, text)
            .joinToString(" ")
            .trim()
        if (content.isBlank()) {
            return null
        }

        return parseSignal(
            content = content,
            timestamp = timestamp,
            source = TransactionUpdateSource.NOTIFICATION,
            summaryPrefix = "Notification from $packageName",
            isFromUpiApp = packageName in KNOWN_UPI_PACKAGES
        )
    }

    private fun parseSignal(
        content: String,
        timestamp: Long,
        source: TransactionUpdateSource,
        summaryPrefix: String,
        isFromUpiApp: Boolean = false
    ): TransactionSignal? {
        val normalizedContent = content.trim().replace("\n", " ")
        if (normalizedContent.isBlank()) {
            return null
        }

        val status = resolveStatus(normalizedContent) ?: return null
        val amount = amountRegex.find(normalizedContent)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()

        return TransactionSignal(
            status = status,
            amount = amount,
            timestamp = timestamp,
            source = source,
            rawText = normalizedContent,
            summary = "$summaryPrefix: ${normalizedContent.take(140)}",
            isFromUpiApp = isFromUpiApp
        )
    }

    private fun resolveStatus(content: String): TransactionStatus? {
        val normalizedContent = content.lowercase(Locale.ROOT)

        if (failedKeywords.any(normalizedContent::contains)) {
            return TransactionStatus.FAILED
        }
        if (successKeywords.any(normalizedContent::contains)) {
            return TransactionStatus.SUCCESS
        }
        if (pendingKeywords.any(normalizedContent::contains)) {
            return TransactionStatus.PENDING
        }
        return null
    }
}
