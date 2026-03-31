package com.nfcupi.pay.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nfcupi.pay.data.TransactionHistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var transactionHistoryRepository: TransactionHistoryRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()

        // Collect all available text content that might carry payment information.
        // Different apps use different extras:
        //   EXTRA_TEXT        — standard single-line body  (most apps)
        //   EXTRA_BIG_TEXT    — expanded paragraph body    (BigTextStyle)
        //   EXTRA_SUB_TEXT    — subtitle / secondary line  (some bank apps)
        //   EXTRA_INFO_TEXT   — info badge text            (older apps)
        //   EXTRA_TEXT_LINES  — array of lines             (InboxStyle: SBI, HDFC, etc.)
        val parts = mutableListOf<String>()
        extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
            ?.toString()?.takeIf { it.isNotBlank() }?.let { parts += it }
        extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)
            ?.toString()?.takeIf { it.isNotBlank() }?.let { parts += it }
        extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)
            ?.toString()?.takeIf { it.isNotBlank() }?.let { parts += it }
        extras.getCharSequence(android.app.Notification.EXTRA_INFO_TEXT)
            ?.toString()?.takeIf { it.isNotBlank() }?.let { parts += it }
        @Suppress("UNCHECKED_CAST")
        (extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES))
            ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
            ?.let { parts += it }

        // Deduplicate (BigText is often a superset of Text) while preserving order.
        val text = parts
            .distinct()
            .joinToString(" ")
            .ifBlank { null }

        if (title == null && text == null) return

        serviceScope.launch {
            transactionHistoryRepository.ingestNotificationSignal(
                packageName = sbn.packageName,
                title = title,
                text = text,
                postedAt = sbn.postTime
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
