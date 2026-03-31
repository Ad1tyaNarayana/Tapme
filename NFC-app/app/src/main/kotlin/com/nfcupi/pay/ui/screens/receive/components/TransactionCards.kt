package com.nfcupi.pay.ui.screens.receive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcupi.pay.data.TransactionRecord
import com.nfcupi.pay.ui.theme.Mono
import com.nfcupi.pay.ui.theme.TapmeBorder
import com.nfcupi.pay.ui.theme.TapmeMuted
import com.nfcupi.pay.ui.theme.TapmeMuted2
import com.nfcupi.pay.ui.theme.TapmeMuted3
import com.nfcupi.pay.ui.theme.TapmeOrange
import com.nfcupi.pay.ui.theme.TapmeOrangeBd
import com.nfcupi.pay.ui.theme.TapmeOrangeBg
import com.nfcupi.pay.ui.theme.TapmeSurface2
import com.nfcupi.pay.ui.theme.TapmeText

@Composable
fun TrackingAccessCard(
    onOpenNotificationSettings: () -> Unit
) {
    // No-op: notification access prompt is now inline in ReceiveScreen
}

@Composable
fun PendingTransactionCard(
    transaction: TransactionRecord,
    onMarkSuccess: () -> Unit,
    onMarkFailed: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TapmeOrangeBg, RoundedCornerShape(6.dp))
            .border(1.dp, TapmeOrangeBd, RoundedCornerShape(6.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "PENDING PAYMENT",
            fontFamily = Mono,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = TapmeOrange
        )
        TransactionSummary(transaction = transaction)
        if (transaction.statusNote.isNotBlank()) {
            Text(
                transaction.statusNote,
                fontFamily = Mono,
                fontSize = 10.sp,
                color = TapmeMuted2
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onMarkSuccess) {
                Text("✓ success", fontFamily = Mono, fontSize = 10.sp, color = TapmeOrange)
            }
            TextButton(onClick = onMarkFailed) {
                Text("✗ failed", fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted)
            }
            TextButton(onClick = onDelete) {
                Text("delete", fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted3)
            }
        }
    }
}
