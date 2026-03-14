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
        if (sbn.packageName == packageName) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()

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
