package com.nfcupi.pay.ui.screens.receive.components

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcupi.pay.data.TransactionRecord
import com.nfcupi.pay.data.TransactionStatus
import java.text.DateFormat
import java.util.Date

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionItem(
    transaction: TransactionRecord,
    onMarkSuccess: () -> Unit,
    onMarkFailed: () -> Unit,
    onMarkPending: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (transaction.status) {
                TransactionStatus.SUCCESS -> MaterialTheme.colorScheme.surfaceVariant
                TransactionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                TransactionStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                StatusBadge(status = transaction.status)
            }
            Spacer(Modifier.height(4.dp))
            TransactionSummary(transaction = transaction)
            if (transaction.statusNote.isNotBlank()) {
                Text(
                    transaction.statusNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (transaction.status != TransactionStatus.SUCCESS) {
                    TextButton(onClick = onMarkSuccess) { Text("Success", fontSize = 12.sp) }
                }
                if (transaction.status != TransactionStatus.FAILED) {
                    TextButton(onClick = onMarkFailed) { Text("Failed", fontSize = 12.sp) }
                }
                if (transaction.status != TransactionStatus.PENDING) {
                    TextButton(onClick = onMarkPending) { Text("Pending", fontSize = 12.sp) }
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TransactionSummary(transaction: TransactionRecord) {
    Column {
        Text(
            "₹${transaction.amount}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        if (transaction.note.isNotBlank()) {
            Text(
                transaction.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Started ${formatTimestamp(transaction.initiatedAt)} • Updated ${formatTimestamp(transaction.lastUpdatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusBadge(status: TransactionStatus) {
    val containerColor = when (status) {
        TransactionStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        TransactionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        TransactionStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (status) {
        TransactionStatus.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        TransactionStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        TransactionStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(color = containerColor, shape = CircleShape) {
        Text(
            text = status.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    return formatter.format(Date(timestamp))
}
