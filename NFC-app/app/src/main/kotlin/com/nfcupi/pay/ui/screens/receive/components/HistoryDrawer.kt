package com.nfcupi.pay.ui.screens.receive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcupi.pay.data.TransactionFilter
import com.nfcupi.pay.data.TransactionSortOption
import com.nfcupi.pay.ui.screens.receive.ReceiveUiState
import com.nfcupi.pay.ui.theme.Mono
import com.nfcupi.pay.ui.theme.TapmeBackground
import com.nfcupi.pay.ui.theme.TapmeBorder
import com.nfcupi.pay.ui.theme.TapmeMuted2
import com.nfcupi.pay.ui.theme.TapmeMuted3
import com.nfcupi.pay.ui.theme.TapmeOrange
import com.nfcupi.pay.ui.theme.TapmeSurface2
import com.nfcupi.pay.ui.theme.TapmeText

@Composable
fun HistoryDrawerContent(
    uiState: ReceiveUiState,
    onClose: () -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onSortChange: (TransactionSortOption) -> Unit,
    onClearAll: () -> Unit,
    onMarkSuccess: (String) -> Unit,
    onMarkFailed: (String) -> Unit,
    onMarkPending: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TapmeBackground)
    ) {
        // ── Header ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "history",
                    fontFamily = Mono,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TapmeText
                )
                if (uiState.historyTransactions.isNotEmpty()) {
                    Text(
                        "${uiState.historyTransactions.size}",
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        color = TapmeMuted3,
                        modifier = Modifier
                            .background(TapmeSurface2, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (uiState.hasHistory) {
                    if (confirmClear) {
                        Text(
                            "confirm?",
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            color = TapmeOrange,
                            modifier = Modifier.clickable { confirmClear = false; onClearAll() }
                        )
                        Text(
                            "cancel",
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            color = TapmeMuted3,
                            modifier = Modifier.clickable { confirmClear = false }
                        )
                    } else {
                        Text(
                            "clear",
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            color = TapmeMuted2,
                            modifier = Modifier.clickable { confirmClear = true }
                        )
                    }
                }
                Text(
                    "×",
                    fontFamily = Mono,
                    fontSize = 20.sp,
                    color = TapmeMuted2,
                    modifier = Modifier.clickable { onClose() }
                )
            }
        }

        HorizontalDivider(color = TapmeBorder, thickness = 1.dp)

        // ── Filter + Sort row ──────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Text(
                    "filter",
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    color = TapmeMuted3,
                    letterSpacing = 1.5.sp
                )
                Text("·", fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted3)

                TransactionFilter.entries.forEachIndexed { i, filter ->
                    val selected = uiState.historyFilter == filter
                    val label = when (filter) {
                        TransactionFilter.ALL     -> "all"
                        TransactionFilter.PENDING -> "pending"
                        TransactionFilter.SUCCESS -> "success"
                        TransactionFilter.FAILED  -> "failed"
                    }
                    Text(
                        label,
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) TapmeOrange else TapmeMuted2,
                        modifier = Modifier
                            .clickable { onFilterChange(filter) }
                            .then(
                                if (selected) Modifier.drawBehind {
                                    drawLine(
                                        color = TapmeOrange,
                                        start = Offset(0f, size.height + 3.dp.toPx()),
                                        end = Offset(size.width, size.height + 3.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                } else Modifier
                            )
                    )
                    if (i < TransactionFilter.entries.size - 1) {
                        Text("·", fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted3)
                    }
                }
            }

            // Sort
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Text(
                    "sort",
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    color = TapmeMuted3,
                    letterSpacing = 1.5.sp
                )
                Text("·", fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted3)

                TransactionSortOption.entries.forEachIndexed { i, sort ->
                    val selected = uiState.historySort == sort
                    val label = when (sort) {
                        TransactionSortOption.LATEST_FIRST      -> "latest"
                        TransactionSortOption.OLDEST_FIRST      -> "oldest"
                        TransactionSortOption.AMOUNT_HIGH_TO_LOW -> "amount ↓"
                        TransactionSortOption.AMOUNT_LOW_TO_HIGH -> "amount ↑"
                        TransactionSortOption.STATUS             -> "status"
                    }
                    Text(
                        label,
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) TapmeOrange else TapmeMuted2,
                        modifier = Modifier
                            .clickable { onSortChange(sort) }
                            .then(
                                if (selected) Modifier.drawBehind {
                                    drawLine(
                                        color = TapmeOrange,
                                        start = Offset(0f, size.height + 3.dp.toPx()),
                                        end = Offset(size.width, size.height + 3.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                } else Modifier
                            )
                    )
                    if (i < TransactionSortOption.entries.size - 1) {
                        Text("·", fontFamily = Mono, fontSize = 10.sp, color = TapmeMuted3)
                    }
                }
            }
        }

        HorizontalDivider(color = TapmeBorder, thickness = 1.dp)

        // ── Transaction List ──────────────────────────────────────────
        if (uiState.historyTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "no transactions yet",
                        fontFamily = Mono,
                        fontSize = 12.sp,
                        color = TapmeMuted3,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "tap generate to start",
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        color = TapmeMuted3,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                items(uiState.historyTransactions, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onMarkSuccess = { onMarkSuccess(transaction.id) },
                        onMarkFailed  = { onMarkFailed(transaction.id) },
                        onMarkPending = { onMarkPending(transaction.id) },
                        onDelete      = { onDelete(transaction.id) }
                    )
                }
            }
        }
    }
}
