package com.nfcupi.pay.ui.screens.receive

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nfcupi.pay.data.TransactionRecord
import com.nfcupi.pay.data.TransactionStatus
import com.nfcupi.pay.ui.components.QrCodeImage
import com.nfcupi.pay.ui.screens.receive.components.HistoryDrawerContent
import com.nfcupi.pay.ui.screens.receive.components.PulseAnimation
import com.nfcupi.pay.ui.theme.Mono
import com.nfcupi.pay.ui.theme.TapmeBackground
import com.nfcupi.pay.ui.theme.TapmeBorder
import com.nfcupi.pay.ui.theme.TapmeMuted
import com.nfcupi.pay.ui.theme.TapmeMuted2
import com.nfcupi.pay.ui.theme.TapmeMuted3
import com.nfcupi.pay.ui.theme.TapmeOrange
import com.nfcupi.pay.ui.theme.TapmeOrangeBd
import com.nfcupi.pay.ui.theme.TapmeOrangeBg
import com.nfcupi.pay.ui.theme.TapmeSurface
import com.nfcupi.pay.ui.theme.TapmeSurface2
import com.nfcupi.pay.ui.theme.TapmeText
import com.nfcupi.pay.util.NfcState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isRefreshing by remember { mutableStateOf(false) }

    val onRefresh: () -> Unit = {
        if (!isRefreshing) {
            coroutineScope.launch {
                isRefreshing = true
                viewModel.refreshMainState()
                delay(500)
                isRefreshing = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = TapmeBackground) {
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                containerColor = TapmeBackground,
                topBar = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = "History",
                                    tint = TapmeMuted2,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                "tapme",
                                fontFamily = Mono,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TapmeOrange
                            )

                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = TapmeMuted2,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = TapmeSurface2, thickness = 1.dp)
                    }
                }
            ) { padding ->
                if (uiState.isActive) {
                    ActiveScreen(
                        uiState = uiState,
                        onStop = viewModel::stopReceiving,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    InputScreen(
                        uiState = uiState,
                        onAmountChange = viewModel::onAmountChange,
                        onNoteChange = viewModel::onTransactionNoteChange,
                        onGenerate = viewModel::startReceiving,
                        onOpenNotificationSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }
        }
    }
}

// ── Input Screen ─────────────────────────────────────────────────────────────

@Composable
private fun InputScreen(
    uiState: ReceiveUiState,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 112.dp)
        ) {
            // NFC / UPI warning banners
            if (uiState.nfcState == NfcState.SUPPORTED_BUT_DISABLED) {
                Spacer(Modifier.height(14.dp))
                InlineBanner("NFC is off — enable it for tap-to-pay, or QR still works")
            }
            if (uiState.upiId.isBlank()) {
                Spacer(Modifier.height(14.dp))
                InlineBanner("→ Open Settings and add your UPI ID to get started")
            }
            if (!uiState.hasNotificationAccess) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TapmeOrangeBg, RoundedCornerShape(4.dp))
                        .border(1.dp, TapmeOrangeBd, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Enable notifications for auto-tracking",
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        color = TapmeOrange,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onOpenNotificationSettings,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp, vertical = 2.dp
                        )
                    ) {
                        Text("Enable", fontFamily = Mono, fontSize = 10.sp, color = TapmeOrange)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "AMOUNT",
                fontFamily = Mono,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = TapmeMuted3
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "₹",
                    fontSize = 22.sp,
                    color = TapmeMuted2,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = uiState.amount,
                    onValueChange = onAmountChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(TapmeOrange),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 64.sp,
                        lineHeight = 72.sp,
                        letterSpacing = (-2).sp,
                        color = TapmeText
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.amount.isEmpty()) {
                                Text(
                                    "0",
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 64.sp,
                                    lineHeight = 72.sp,
                                    letterSpacing = (-2).sp,
                                    color = TapmeMuted3
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            uiState.amountErrorMessage?.let {
                Text(
                    "max ₹1,00,000",
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    color = Color(0xFFEF4444)
                )
            }

            HorizontalDivider(color = TapmeBorder, thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))

            BasicTextField(
                value = uiState.transactionNote,
                onValueChange = onNoteChange,
                singleLine = true,
                cursorBrush = SolidColor(TapmeOrange),
                textStyle = TextStyle(
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = TapmeMuted
                ),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(vertical = 14.dp)) {
                        if (uiState.transactionNote.isEmpty()) {
                            Text(
                                "add a note...",
                                fontFamily = Mono,
                                fontSize = 12.sp,
                                color = TapmeMuted3
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = TapmeSurface2, thickness = 1.dp)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("split", "request", "tip").forEach { label ->
                    val isActive = uiState.transactionNote == label
                    Text(
                        label,
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        color = if (isActive) TapmeOrange else TapmeMuted,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                when {
                                    isActive -> onNoteChange("")
                                    uiState.transactionNote.isEmpty() -> onNoteChange(label)
                                }
                            }
                            .background(
                                if (isActive) TapmeOrangeBg else TapmeSurface2,
                                RoundedCornerShape(3.dp)
                            )
                            .border(
                                1.dp,
                                if (isActive) TapmeOrangeBd else TapmeBorder,
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (uiState.historyTransactions.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "RECENT",
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = TapmeMuted3
                )
                HorizontalDivider(
                    color = TapmeSurface2,
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                uiState.historyTransactions.take(3).forEach { tx ->
                    RecentRow(tx)
                    HorizontalDivider(color = TapmeSurface2.copy(alpha = 0.6f), thickness = 0.5.dp)
                }
            }

            uiState.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    fontFamily = Mono,
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(28.dp))
        }

        Button(
            onClick = onGenerate,
            enabled = uiState.upiId.isNotBlank() && uiState.amountErrorMessage == null,
            colors = ButtonDefaults.buttonColors(
                containerColor = TapmeOrange,
                contentColor = TapmeBackground,
                disabledContainerColor = TapmeOrange.copy(alpha = 0.25f),
                disabledContentColor = TapmeBackground.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .height(56.dp)
        ) {
            Text(
                "Generate →",
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

// ── Active Screen (QR + NFC) ─────────────────────────────────────────────────

@Composable
private fun ActiveScreen(
    uiState: ReceiveUiState,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // Amount display
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "₹ ${uiState.amount.ifBlank { "—" }}",
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-1.5).sp,
                color = TapmeText
            )
            if (uiState.transactionNote.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    uiState.transactionNote,
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    color = TapmeMuted2
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = TapmeSurface2, thickness = 1.dp)
        Spacer(Modifier.height(24.dp))

        // QR Code — light on dark to match design
        if (uiState.upiUri.isNotBlank()) {
            QrCodeImage(
                content = uiState.upiUri,
                foreground = Color(0xFFF5F5F0),
                background = Color(0xFF0F0F0D),
                size = 560,
                modifier = Modifier.size(280.dp)
            )
        }

        // NFC section
        if (uiState.nfcState == NfcState.SUPPORTED_AND_ENABLED) {
            Spacer(Modifier.height(32.dp))

            // "or tap phones" divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = TapmeBorder)
                Text("or tap phones", fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted2)
                HorizontalDivider(modifier = Modifier.weight(1f), color = TapmeBorder)
            }

            Spacer(Modifier.height(28.dp))

            PulseAnimation()

            Spacer(Modifier.height(16.dp))

            Text(
                "opens their UPI app ↗",
                fontFamily = Mono,
                fontSize = 10.sp,
                color = TapmeMuted2
            )
        }

        Spacer(Modifier.height(32.dp))

        // Stop button — minimal, unobtrusive
        TextButton(onClick = onStop) {
            Text(
                "stop ×",
                fontFamily = Mono,
                fontSize = 11.sp,
                color = TapmeMuted2
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun InlineBanner(message: String) {
    Text(
        message,
        fontFamily = Mono,
        fontSize = 10.sp,
        color = TapmeOrange,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(TapmeOrangeBg, RoundedCornerShape(4.dp))
            .border(1.dp, TapmeOrangeBd, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun RecentRow(tx: TransactionRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            tx.note.ifBlank { "tapme payment" }.take(28),
            fontFamily = Mono,
            fontSize = 11.sp,
            color = TapmeMuted2,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
        if (tx.amount.isNotBlank()) {
            Text(
                "₹${tx.amount.toDoubleOrNull()?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) } ?: tx.amount}",
                fontFamily = Mono,
                fontSize = 11.sp,
                color = TapmeMuted2
            )
        }
    }
}

