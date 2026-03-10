package com.nfcupi.pay.ui.screens.receive

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nfcupi.pay.R
import com.nfcupi.pay.util.NfcState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Warn if NFC is off or unsupported
            if (uiState.nfcState != NfcState.SUPPORTED_AND_ENABLED) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = when (uiState.nfcState) {
                            NfcState.NOT_SUPPORTED         -> "This device does not support NFC"
                            NfcState.SUPPORTED_BUT_DISABLED -> "NFC is off. Enable it in Settings > Connected devices > NFC"
                            else -> ""
                        },
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Warn if UPI ID not set
            if (uiState.upiId.isBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(
                        "Tap Settings to enter your UPI ID",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Amount input (disabled while active)
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount (optional)") },
                placeholder = { Text("Leave blank — payer enters amount") },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isActive,
                singleLine = true
            )

            // Note input
            OutlinedTextField(
                value = uiState.transactionNote,
                onValueChange = viewModel::onTransactionNoteChange,
                label = { Text("Note (optional)") },
                placeholder = {
                    val defaultNote = if (uiState.amount.isNotBlank()) {
                        "Tapme Payment of ${uiState.amount} to ${uiState.displayName}"
                    } else {
                        "Tapme Payment to ${uiState.displayName}"
                    }
                    Text(defaultNote)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isActive,
                singleLine = true
            )

            if (uiState.upiId.isNotBlank()) {
                Text(
                    "Receiving to: ${uiState.upiId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.isActive) {
                PulseAnimation()
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::deactivateNfc,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Stop") }
            } else {
                Button(
                    onClick = viewModel::activateNfc,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = uiState.nfcState == NfcState.SUPPORTED_AND_ENABLED && uiState.upiId.isNotBlank()
                ) {
                    Text("Activate NFC Tag", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            uiState.statusMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            // Debug: show URI in dev builds (remove before shipping)
            if (uiState.isActive && uiState.upiUri.isNotBlank()) {
                Text(
                    uiState.upiUri,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), CircleShape)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Text("📡", fontSize = 42.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Ready to tap!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Hold your phone near the payer's phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
