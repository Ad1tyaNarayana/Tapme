package com.nfcupi.pay.ui.screens.receive

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nfcupi.pay.ui.screens.receive.AMOUNT_CAP_ERROR_MESSAGE
import com.nfcupi.pay.data.TransactionStatus
import com.nfcupi.pay.util.NfcState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nfcupi.pay.ui.screens.receive.components.HistoryDrawerContent
import com.nfcupi.pay.ui.screens.receive.components.PendingTransactionCard
import com.nfcupi.pay.ui.screens.receive.components.TrackingAccessCard
import com.nfcupi.pay.ui.screens.receive.components.PulseAnimation
import com.nfcupi.pay.ui.components.QrCodeImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isActive) {
        if (uiState.isActive) {
            delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                HistoryDrawerContent(
                    uiState = uiState,
                    onClose = { coroutineScope.launch { drawerState.close() } },
                    onFilterChange = viewModel::onHistoryFilterChange,
                    onSortChange = viewModel::onHistorySortChange,
                    onClearAll = viewModel::clearAllTransactions,
                    onMarkSuccess = { viewModel.markTransactionAs(TransactionStatus.SUCCESS, it) },
                    onMarkFailed = { viewModel.markTransactionAs(TransactionStatus.FAILED, it) },
                    onMarkPending = { viewModel.markTransactionAs(TransactionStatus.PENDING, it) },
                    onDelete = viewModel::deleteTransaction
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tapme") },
                    navigationIcon = {
                        TextButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Text("History")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isRefreshing = true
                        viewModel.refreshMainState()
                        delay(400)
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    uiState.latestPendingTransaction?.let { transaction ->
                        PendingTransactionCard(
                            transaction = transaction,
                            onMarkSuccess = { viewModel.markTransactionAs(TransactionStatus.SUCCESS, transaction.id) },
                            onMarkFailed = { viewModel.markTransactionAs(TransactionStatus.FAILED, transaction.id) },
                            onDelete = { viewModel.deleteTransaction(transaction.id) }
                        )
                    }

                    if (!uiState.hasNotificationAccess) {
                        TrackingAccessCard(
                            onOpenNotificationSettings = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        )
                    }

                    if (!uiState.isActive) {
                        if (uiState.nfcState != NfcState.SUPPORTED_AND_ENABLED) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Text(
                                    text = when (uiState.nfcState) {
                                        NfcState.NOT_SUPPORTED -> "NFC unavailable — falling back to QR code only"
                                        NfcState.SUPPORTED_BUT_DISABLED -> "NFC is off. Enable it in Settings for tap-to-pay, or use QR code"
                                        else -> ""
                                    },
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (uiState.upiId.isBlank()) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                                Text(
                                    "Tap Settings to enter your UPI ID",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.amount,
                            onValueChange = viewModel::onAmountChange,
                            label = { Text("Amount (optional)") },
                            placeholder = { Text("Leave blank — payer enters amount") },
                            supportingText = {
                                uiState.amountErrorMessage?.let { message ->
                                    Text(
                                        if (message == AMOUNT_CAP_ERROR_MESSAGE) {
                                            "Max 1 Lakh (₹ 1,00,000)"
                                        } else {
                                            message
                                        }
                                    )
                                }
                            },
                            isError = uiState.amountErrorMessage != null,
                            prefix = { Text("₹ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = uiState.transactionNote,
                            onValueChange = viewModel::onTransactionNoteChange,
                            label = { Text("Note (optional)") },
                            placeholder = {
                                val defaultNote = if (uiState.amount.isNotBlank()) {
                                    "Tapme Payment of ${uiState.amount} to ${uiState.displayName.ifBlank { uiState.upiId }}"
                                } else {
                                    "Tapme Payment to ${uiState.displayName.ifBlank { uiState.upiId }}"
                                }
                                Text(defaultNote)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (uiState.upiId.isNotBlank()) {
                            Text(
                                "Receiving to: ${uiState.upiId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (uiState.isActive) {
                        Text("Scan QR Code to Pay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.upiUri.isNotBlank()) {
                            QrCodeImage(
                                content = uiState.upiUri,
                                modifier = Modifier.padding(vertical = 16.dp),
                                size = 600
                            )
                        }
                        
                        if (uiState.nfcState == NfcState.SUPPORTED_AND_ENABLED) {
                            Text("Or tap payer's phone to your device", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            PulseAnimation()
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::stopReceiving,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop")
                        }
                    } else {
                        Button(
                            onClick = viewModel::startReceiving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = uiState.upiId.isNotBlank()
                        ) {
                            Text("Generate", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                }
            }
        }
    }
}

