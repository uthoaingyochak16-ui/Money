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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItemRow
import com.example.ui.viewmodel.FinanceViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    viewModel: FinanceViewModel,
    onOpenNewLog: (TransactionEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredList by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.filterType.collectAsStateWithLifecycle()

    val dateSubformat = remember { SimpleDateFormat("dd MMMM", Locale.getDefault()) }
    val dateParser = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val numberFormatter = remember { DecimalFormat("#,##0") }

    // Group transactions by dateString
    val grouped = remember(filteredList) {
        filteredList.groupBy { it.dateString }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("logs_screen")
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Transaction Logs",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = {
                    Text(
                        text = "Search note, category, account...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logs_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills: All | Expense | Income | Transfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = "All",
                    isSelected = selectedType == null,
                    onClick = { viewModel.filterType.value = null }
                )
                FilterPill(
                    label = "Expense",
                    isSelected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        viewModel.filterType.value =
                            if (selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                    }
                )
                FilterPill(
                    label = "Income",
                    isSelected = selectedType == TransactionType.INCOME,
                    onClick = {
                        viewModel.filterType.value =
                            if (selectedType == TransactionType.INCOME) null else TransactionType.INCOME
                    }
                )
                FilterPill(
                    label = "Transfer",
                    isSelected = selectedType == TransactionType.TRANSFER,
                    onClick = {
                        viewModel.filterType.value =
                            if (selectedType == TransactionType.TRANSFER) null else TransactionType.TRANSFER
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedType != null) {
                            "No transactions match your search."
                        } else {
                            "No transactions yet."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap + New Log to record your first transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                grouped.forEach { (dateString, txList) ->
                    // Natural Date Group Header
                    val groupTitle = viewModel.getDateGroupHeader(dateString)
                    val parsedDate: Date? = try {
                        dateParser.parse(dateString)
                    } catch (e: Exception) {
                        null
                    }
                    val formattedSubDate = if (parsedDate != null) dateSubformat.format(parsedDate) else dateString

                    val dayExpenseTotal = txList
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }

                    item(key = "header_$dateString") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = groupTitle,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formattedSubDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (dayExpenseTotal > 0) {
                                    Text(
                                        text = "-$currency${numberFormatter.format(dayExpenseTotal)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(
                        count = txList.size,
                        key = { index -> txList[index].id }
                    ) { index ->
                        val tx = txList[index]
                        TransactionItemRow(
                            transaction = tx,
                            currencySymbol = currency,
                            onClick = { onOpenNewLog(tx) }
                        )
                        if (index < txList.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
