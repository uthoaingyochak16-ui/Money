package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sms
import com.example.ui.components.AiAssistantDialog
import com.example.ui.components.SmsSimulatorDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FinanceViewModel
import java.text.DecimalFormat

@Composable
fun MoreScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val balances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val summary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val numberFormatter = remember { DecimalFormat("#,##0") }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(summary.budgetAmount.toInt().toString()) }

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showSmsSimulatorDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Accounts & Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Accounts section
        item {
            Text(
                text = "Account Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 4.dp)
            ) {
                balances.forEach { (account, balance) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (account == "Bank") Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = account,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "$currency${numberFormatter.format(balance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // AI & Cellular Automation
        item {
            Text(
                text = "AI & Cellular Automation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Default.AutoAwesome,
                title = "Gemini Financial AI Assistant",
                subtitle = "Spending insights, budget advice & smart parsing",
                onClick = { showAiDialog = true }
            )

            SettingsRow(
                icon = Icons.Default.Sms,
                title = "Cellular SMS Auto-Entry",
                subtitle = "Auto-catch bKash, Nagad & Bank SMS messages",
                onClick = { showSmsSimulatorDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Budget & Currency Settings
        item {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Default.AttachMoney,
                title = "Monthly Budget Limit",
                subtitle = "$currency${numberFormatter.format(summary.budgetAmount)}",
                onClick = {
                    budgetInput = summary.budgetAmount.toInt().toString()
                    showBudgetDialog = true
                }
            )

            SettingsRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Currency Symbol",
                subtitle = currency,
                onClick = { showCurrencyDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Data Management (Section 17: Realistic States)
        item {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Default.RestartAlt,
                title = "Load Realistic Starter Data",
                subtitle = "Pre-fill with typical monthly transactions",
                onClick = { showResetConfirmDialog = true }
            )

            SettingsRow(
                icon = Icons.Default.DeleteSweep,
                title = "Clear All Records",
                subtitle = "Reset to first-launch empty state",
                isDestructive = true,
                onClick = { showClearConfirmDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // About
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Money Manager v1.0 • Offline First",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Budget Dialog
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(
                    text = "Set Monthly Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your planned monthly spending limit:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = budgetInput,
                            onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = budgetInput.toDoubleOrNull() ?: 30000.0
                        viewModel.setBudget(amt)
                        showBudgetDialog = false
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Currency Picker Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = {
                Text(
                    text = "Select Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("৳" to "BDT (৳)", "$" to "USD ($)", "€" to "EUR (€)", "£" to "GBP (£)", "₹" to "INR (₹)").forEach { (symbol, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.currencySymbol.value = symbol
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currency == symbol) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currency == symbol) {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Reset Starter Data Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reload Starter Data?") },
            text = { Text("This will reset your transaction records to realistic starter examples matching the design specifications.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToSampleData()
                        showResetConfirmDialog = false
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Reload Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Records?") },
            text = { Text("This will remove all transactions and show the clean empty launch state. You can reload sample data anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAiDialog) {
        AiAssistantDialog(
            viewModel = viewModel,
            onDismiss = { showAiDialog = false }
        )
    }

    if (showSmsSimulatorDialog) {
        SmsSimulatorDialog(
            viewModel = viewModel,
            onDismiss = { showSmsSimulatorDialog = false }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}
