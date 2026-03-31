package com.nfcupi.pay.ui.screens.receive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcupi.pay.data.TransactionRecord
import com.nfcupi.pay.data.TransactionStatus
import com.nfcupi.pay.ui.theme.Mono
import com.nfcupi.pay.ui.theme.TapmeBorder
import com.nfcupi.pay.ui.theme.TapmeError
import com.nfcupi.pay.ui.theme.TapmeMuted2
import com.nfcupi.pay.ui.theme.TapmeMuted3
import com.nfcupi.pay.ui.theme.TapmeOrange
import com.nfcupi.pay.ui.theme.TapmeText
import java.text.DateFormat
import java.util.Date

private val ColorSuccess = Color(0xFF4ADE80)

@Composable
fun TransactionItem(
    transaction: TransactionRecord,
    onMarkSuccess: () -> Unit,
    onMarkFailed: () -> Unit,
    onMarkPending: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = when (transaction.status) {
        TransactionStatus.SUCCESS -> ColorSuccess
        TransactionStatus.FAILED  -> TapmeError
        TransactionStatus.PENDING -> TapmeOrange
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(accentColor.copy(alpha = 0.55f))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Amount + status tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "₹${transaction.amount.ifBlank { "—" }}",
                    fontFamily = Mono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TapmeText
                )
                StatusTag(transaction.status)
            }
            // Note
            if (transaction.note.isNotBlank()) {
                Text(
                    transaction.note,
                    fontFamily = Mono,
                    fontSize = 10.sp,
                    color = TapmeMuted2,
                    lineHeight = 15.sp
                )
            }
            // Timestamp
            Text(
                formatTimestamp(transaction.initiatedAt),
                fontFamily = Mono,
                fontSize = 9.sp,
                color = TapmeMuted3
            )
            // Status note
            if (transaction.statusNote.isNotBlank()) {
                Text(
                    transaction.statusNote,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    color = TapmeMuted3,
                    lineHeight = 14.sp
                )
            }
            // Actions
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (transaction.status != TransactionStatus.SUCCESS) {
                    Text(
                        "success",
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        color = ColorSuccess,
                        modifier = Modifier.clickable { onMarkSuccess() }
                    )
                }
                if (transaction.status != TransactionStatus.FAILED) {
                    Text(
                        "failed",
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        color = TapmeError,
                        modifier = Modifier.clickable { onMarkFailed() }
                    )
                }
                if (transaction.status != TransactionStatus.PENDING) {
                    Text(
                        "pending",
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        color = TapmeOrange,
                        modifier = Modifier.clickable { onMarkPending() }
                    )
                }
                Text(
                    "delete",
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    color = TapmeMuted3,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
    HorizontalDivider(color = TapmeBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@Composable
fun TransactionSummary(transaction: TransactionRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "₹${transaction.amount.ifBlank { "—" }}",
            fontFamily = Mono,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TapmeText
        )
        if (transaction.note.isNotBlank()) {
            Text(transaction.note, fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted2)
        }
        Text(
            "started ${formatTimestamp(transaction.initiatedAt)}",
            fontFamily = Mono,
            fontSize = 9.sp,
            color = TapmeMuted3
        )
    }
}

@Composable
fun StatusTag(status: TransactionStatus) {
    val (color, label) = when (status) {
        TransactionStatus.SUCCESS -> Pair(ColorSuccess, "success")
        TransactionStatus.FAILED  -> Pair(TapmeError,   "failed")
        TransactionStatus.PENDING -> Pair(TapmeOrange,  "pending")
    }
    Text(
        "[$label]",
        fontFamily = Mono,
        fontSize = 9.sp,
        color = color.copy(alpha = 0.85f)
    )
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    return formatter.format(Date(timestamp))
}
