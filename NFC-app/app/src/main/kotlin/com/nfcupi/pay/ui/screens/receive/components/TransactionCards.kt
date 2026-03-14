package com.nfcupi.pay.ui.screens.receive.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nfcupi.pay.data.TransactionRecord

@Composable
fun TrackingAccessCard(
    onOpenNotificationSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Improve transaction tracking", fontWeight = FontWeight.Bold)
            Text(
                "Give notification access so Tapme can detect successful, failed, and pending UPI payments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "Notification access: Not enabled",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = onOpenNotificationSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                    Text("Enable notifications")
            }
        }
    }
}

@Composable
fun PendingTransactionCard(
    transaction: TransactionRecord,
    onMarkSuccess: () -> Unit,
    onMarkFailed: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Latest pending transaction", fontWeight = FontWeight.Bold)
            TransactionSummary(transaction = transaction)
            Text(
                transaction.statusNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMarkSuccess,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark success")
                }
                OutlinedButton(
                    onClick = onMarkFailed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark failed")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
