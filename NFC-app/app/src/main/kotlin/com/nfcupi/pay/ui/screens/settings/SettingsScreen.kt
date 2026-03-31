package com.nfcupi.pay.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nfcupi.pay.ui.theme.Mono
import com.nfcupi.pay.ui.theme.TapmeBackground
import com.nfcupi.pay.ui.theme.TapmeBorder
import com.nfcupi.pay.ui.theme.TapmeMuted2
import com.nfcupi.pay.ui.theme.TapmeMuted3
import com.nfcupi.pay.ui.theme.TapmeOrange
import com.nfcupi.pay.ui.theme.TapmeSurface2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = TapmeBackground,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TapmeMuted2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "settings",
                        fontFamily = Mono,
                        fontSize = 15.sp,
                        color = TapmeMuted2
                    )
                }
                HorizontalDivider(color = TapmeSurface2, thickness = 1.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "PAYMENT DETAILS",
                fontFamily = Mono,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = TapmeMuted3
            )

            OutlinedTextField(
                value = uiState.upiId,
                onValueChange = viewModel::onUpiIdChange,
                label = { Text("Your UPI ID", fontFamily = Mono, fontSize = 11.sp) },
                placeholder = { Text("yourname@upi", fontFamily = Mono) },
                isError = uiState.upiIdError != null,
                supportingText = uiState.upiIdError?.let { { Text(it, fontFamily = Mono, fontSize = 10.sp) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(4.dp)
            )

            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = { Text("Display Name", fontFamily = Mono, fontSize = 11.sp) },
                placeholder = { Text("Your name or shop name", fontFamily = Mono) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(4.dp)
            )

            Text(
                "This name appears on the payer's UPI confirmation screen.",
                fontFamily = Mono,
                fontSize = 10.sp,
                color = TapmeMuted3,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = viewModel::save,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TapmeOrange,
                    contentColor = TapmeBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (uiState.isSaved) "Saved ✓" else "Save",
                    fontFamily = Mono,
                    fontSize = 14.sp
                )
            }
        }
    }
}
