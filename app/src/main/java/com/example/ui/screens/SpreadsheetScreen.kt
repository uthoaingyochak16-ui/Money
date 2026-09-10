package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.TransferBlueDark
import com.example.ui.theme.TransferBlueLight
import com.example.ui.viewmodel.FinanceViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SortField {
    SL,
    DATE,
    AMOUNT,
    CATEGORY
}

enum class SortOrder {
    ASC,
    DESC
}

data class MonthItem(
    val key: String, // "yyyy-MM" or "ALL"
    val displayName: String, // "September 2026"
    val count: Int,
    val income: Double,
    val expense: Double,
    val net: Double,
    val isCurrentMonth: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetScreen(
    viewModel: FinanceViewModel,
    onOpenNewLog: (TransactionEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTx by viewModel.allTransactions.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val numberFormatter = remember { DecimalFormat("#,##0") }

    val monthYearFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val monthDisplayFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentMonthKey = remember { monthYearFormat.format(Date()) }

    // Selected month state: null means Month List view is shown first!
    var selectedMonthKey by remember { mutableStateOf<String?>(null) }

    // Search query within the open sheet
    var sheetSearchQuery by remember { mutableStateOf("") }
    var sortField by remember { mutableStateOf(SortField.SL) }
    var sortOrder by remember { mutableStateOf(SortOrder.ASC) }

    // Build distinct month items from all transactions
    val monthItems = remember(allTx) {
        val keys = allTx.map { it.dateString.take(7) }.toMutableSet()
        if (currentMonthKey.isNotBlank()) {
            keys.add(currentMonthKey)
        }

        keys.filter { it.length == 7 }
            .sortedDescending()
            .map { key ->
                val monthTx = allTx.filter { it.dateString.startsWith(key) }
                val inc = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val exp = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                val formattedDisplay = try {
                    val date = monthYearFormat.parse(key)
                    if (date != null) monthDisplayFormat.format(date) else key
                } catch (e: Exception) {
                    key
                }

                MonthItem(
                    key = key,
                    displayName = formattedDisplay,
                    count = monthTx.size,
                    income = inc,
                    expense = exp,
                    net = inc - exp,
                    isCurrentMonth = key == currentMonthKey
                )
            }
    }

    val isDark = isSystemInDarkTheme()
    val incomeColor = if (isDark) IncomeGreenDark else IncomeGreenLight
    val expenseColor = if (isDark) ExpenseRedDark else ExpenseRedLight
    val transferColor = if (isDark) TransferBlueDark else TransferBlueLight

    if (selectedMonthKey == null) {
        // VIEW 1: Monthly Ledger Folder List (User requested month list first)
        MonthListView(
            monthItems = monthItems,
            allTransactions = allTx,
            currency = currency,
            numberFormatter = numberFormatter,
            incomeColor = incomeColor,
            expenseColor = expenseColor,
            onSelectMonth = { key ->
                selectedMonthKey = key
                sheetSearchQuery = ""
                sortField = SortField.SL
                sortOrder = SortOrder.ASC
            },
            modifier = modifier
        )
    } else {
        // VIEW 2: Data Sheet View for selected month (with SL column & direct update)
        val selectedMonthItem = monthItems.find { it.key == selectedMonthKey }
        val displayTitle = if (selectedMonthKey == "ALL") {
            "All Records Sheet"
        } else {
            selectedMonthItem?.displayName ?: selectedMonthKey ?: "Data Sheet"
        }

        // Transactions for the selected month
        val monthTransactions = remember(allTx, selectedMonthKey) {
            if (selectedMonthKey == "ALL") {
                allTx
            } else {
                allTx.filter { it.dateString.startsWith(selectedMonthKey!!) }
            }
        }

        // Filtered by search within sheet
        val filteredSheetList = remember(monthTransactions, sheetSearchQuery) {
            if (sheetSearchQuery.isBlank()) {
                monthTransactions
            } else {
                monthTransactions.filter { tx ->
                    tx.note.contains(sheetSearchQuery, ignoreCase = true) ||
                        tx.category.contains(sheetSearchQuery, ignoreCase = true) ||
                        tx.account.contains(sheetSearchQuery, ignoreCase = true) ||
                        tx.dateString.contains(sheetSearchQuery) ||
                        tx.amount.toString().contains(sheetSearchQuery)
                }
            }
        }

        // Sorted list
        val sortedList = remember(filteredSheetList, sortField, sortOrder) {
            when (sortField) {
                SortField.SL -> if (sortOrder == SortOrder.DESC) {
                    filteredSheetList.sortedByDescending { it.timestamp }
                } else {
                    filteredSheetList.sortedBy { it.timestamp }
                }
                SortField.DATE -> if (sortOrder == SortOrder.DESC) {
                    filteredSheetList.sortedByDescending { it.timestamp }
                } else {
                    filteredSheetList.sortedBy { it.timestamp }
                }
                SortField.AMOUNT -> if (sortOrder == SortOrder.DESC) {
                    filteredSheetList.sortedByDescending { it.amount }
                } else {
                    filteredSheetList.sortedBy { it.amount }
                }
                SortField.CATEGORY -> if (sortOrder == SortOrder.DESC) {
                    filteredSheetList.sortedByDescending { it.category }
                } else {
                    filteredSheetList.sortedBy { it.category }
                }
            }
        }

        val totalIncome = remember(monthTransactions) {
            monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        }
        val totalExpense = remember(monthTransactions) {
            monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }
        val netBalance = totalIncome - totalExpense

        MonthSpreadsheetView(
            displayTitle = displayTitle,
            monthKey = selectedMonthKey!!,
            monthItems = monthItems,
            sortedList = sortedList,
            totalCount = monthTransactions.size,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            currency = currency,
            numberFormatter = numberFormatter,
            incomeColor = incomeColor,
            expenseColor = expenseColor,
            transferColor = transferColor,
            searchQuery = sheetSearchQuery,
            onSearchChange = { sheetSearchQuery = it },
            sortField = sortField,
            sortOrder = sortOrder,
            onSortChange = { field ->
                if (sortField == field) {
                    sortOrder = if (sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                } else {
                    sortField = field
                    sortOrder = if (field == SortField.AMOUNT || field == SortField.DATE) SortOrder.DESC else SortOrder.ASC
                }
            },
            onBackToMonths = { selectedMonthKey = null },
            onSelectOtherMonth = { selectedMonthKey = it },
            onUpdateTransaction = { tx -> onOpenNewLog(tx) },
            modifier = modifier
        )
    }
}

/**
 * Screen 1: Month List Overview
 * "Ledger section e first month er ekta list rakho.. Se list click korle sheet er data gulo dekha jabe."
 */
@Composable
private fun MonthListView(
    monthItems: List<MonthItem>,
    allTransactions: List<TransactionEntity>,
    currency: String,
    numberFormatter: DecimalFormat,
    incomeColor: androidx.compose.ui.graphics.Color,
    expenseColor: androidx.compose.ui.graphics.Color,
    onSelectMonth: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTimeIncome = remember(allTransactions) {
        allTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val allTimeExpense = remember(allTransactions) {
        allTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val allTimeNet = allTimeIncome - allTimeExpense

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("month_list_view")
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Monthly Ledgers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select a month to open its data sheet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // All-time Financial Overview Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ledger Archive Summary",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${allTransactions.size} total entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Inflow",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+$currency${numberFormatter.format(allTimeIncome)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = incomeColor
                            )
                        }

                        Column {
                            Text(
                                text = "Total Outflow",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-$currency${numberFormatter.format(allTimeExpense)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = expenseColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Cumulative Net",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currency${numberFormatter.format(allTimeNet)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (allTimeNet >= 0) incomeColor else expenseColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Master Sheet Quick Access
            Card(
                onClick = { onSelectMonth("ALL") },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_item_ALL")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Master Sheet (All Records)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View & edit all transactions across all time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Open →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Available Monthly Sheets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        items(
            items = monthItems,
            key = { it.key }
        ) { month ->
            MonthCardItem(
                month = month,
                currency = currency,
                numberFormatter = numberFormatter,
                incomeColor = incomeColor,
                expenseColor = expenseColor,
                onClick = { onSelectMonth(month.key) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthCardItem(
    month: MonthItem,
    currency: String,
    numberFormatter: DecimalFormat,
    incomeColor: androidx.compose.ui.graphics.Color,
    expenseColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (month.isCurrentMonth) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("month_item_${month.key}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (month.isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = month.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (month.isCurrentMonth) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "${month.count} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "In: +$currency${numberFormatter.format(month.income)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = incomeColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Out: -$currency${numberFormatter.format(month.expense)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = expenseColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Net: $currency${numberFormatter.format(month.net)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (month.net >= 0) incomeColor else expenseColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sheet →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Screen 2: Month Data Sheet with Serial Number (SL) and Direct Update
 * "Data sheet e new ekta Serial number column add koro. Chaile sheet thake data update kora jabe."
 */
@Composable
private fun MonthSpreadsheetView(
    displayTitle: String,
    monthKey: String,
    monthItems: List<MonthItem>,
    sortedList: List<TransactionEntity>,
    totalCount: Int,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    currency: String,
    numberFormatter: DecimalFormat,
    incomeColor: androidx.compose.ui.graphics.Color,
    expenseColor: androidx.compose.ui.graphics.Color,
    transferColor: androidx.compose.ui.graphics.Color,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortField: SortField,
    sortOrder: SortOrder,
    onSortChange: (SortField) -> Unit,
    onBackToMonths: () -> Unit,
    onSelectOtherMonth: (String) -> Unit,
    onUpdateTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    // Column widths for dense spreadsheet
    val colSlWidth = 48.dp // NEW Serial Number Column!
    val colDateWidth = 86.dp
    val colTypeWidth = 72.dp
    val colCatWidth = 96.dp
    val colAccountWidth = 86.dp
    val colNoteWidth = 135.dp
    val colAmountWidth = 96.dp
    val colActionWidth = 64.dp // Direct edit action column

    val totalTableWidth = colSlWidth + colDateWidth + colTypeWidth + colCatWidth + colAccountWidth + colNoteWidth + colAmountWidth + colActionWidth

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("spreadsheet_screen")
    ) {
        // Navigation & Month Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToMonths,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("back_to_months_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Months",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$totalCount entries recorded in this sheet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Month Switcher Chips horizontally
                Box(
                    modifier = Modifier
                        .clickable { onBackToMonths() }
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📁 All Months",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Month Selector Bar for one-tap switching between months
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isSelected = monthKey == "ALL"
                    MonthChip(
                        label = "All Time",
                        isSelected = isSelected,
                        onClick = { onSelectOtherMonth("ALL") }
                    )
                }
                items(monthItems) { item ->
                    val isSelected = monthKey == item.key
                    MonthChip(
                        label = item.displayName.take(8),
                        isSelected = isSelected,
                        onClick = { onSelectOtherMonth(item.key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Stats Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "In: +$currency${numberFormatter.format(totalIncome)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = incomeColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Out: -$currency${numberFormatter.format(totalExpense)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = expenseColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Net: $currency${numberFormatter.format(netBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (netBalance >= 0) incomeColor else expenseColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = "Filter sheet records...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("spreadsheet_search_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Informative reminder banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Tap any row or Edit button to update record directly",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Spreadsheet Table Area with Horizontal Scroll
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(totalTableWidth)
                    .testTag("spreadsheet_table")
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. SERIAL NUMBER COLUMN (SL)
                    HeaderCell(
                        text = "SL",
                        width = colSlWidth,
                        isSorted = sortField == SortField.SL,
                        sortOrder = sortOrder,
                        textAlign = TextAlign.Center,
                        onClick = { onSortChange(SortField.SL) }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(18.dp))

                    // 2. DATE COLUMN
                    HeaderCell(
                        text = "Date",
                        width = colDateWidth,
                        isSorted = sortField == SortField.DATE,
                        sortOrder = sortOrder,
                        onClick = { onSortChange(SortField.DATE) }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(18.dp))

                    // 3. TYPE COLUMN
                    HeaderCell(
                        text = "Type",
                        width = colTypeWidth,
                        isSorted = false,
                        sortOrder = sortOrder,
                        onClick = {}
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(18.dp))

                    // 4. CATEGORY COLUMN
                    HeaderCell(
                        text = "Category",
                        width = colCatWidth,
                        isSorted = sortField == SortField.CATEGORY,
                        sortOrder = sortOrder,
                        onClick = { onSortChange(SortField.CATEGORY) }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(18.dp))

                    // 5. ACCOUNT COLUMN
                    HeaderCell(
                        text = "Account",
                        width = colAccountWidth,
                        isSorted = false,
                        sortOrder = sortOrder,
                        onClick = {}
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(18.dp))

                    // 6. NOTE COLUMN
                    HeaderCell(
                        text = "Note",
                        width = colNoteWidth,
                        isSorted = false,
                        sortOrder = sortOrder,
                        onClick = {}
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(18.dp))

                    // 7. AMOUNT COLUMN
                    HeaderCell(
                        text = "Amount",
                        width = colAmountWidth,
                        isSorted = sortField == SortField.AMOUNT,
                        sortOrder = sortOrder,
                        textAlign = TextAlign.End,
                        onClick = { onSortChange(SortField.AMOUNT) }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(18.dp))

                    // 8. ACTION COLUMN (Direct Update)
                    HeaderCell(
                        text = "Action",
                        width = colActionWidth,
                        isSorted = false,
                        sortOrder = sortOrder,
                        textAlign = TextAlign.Center,
                        onClick = {}
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // Rows List
                if (sortedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No records in this sheet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add transactions using the '+' button below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(
                            items = sortedList,
                            key = { _, item -> item.id }
                        ) { index, tx ->
                            val rowBg = if (index % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }

                            val amtColor = when (tx.type) {
                                TransactionType.INCOME -> incomeColor
                                TransactionType.EXPENSE -> expenseColor
                                TransactionType.TRANSFER -> transferColor
                            }

                            val amtText = when (tx.type) {
                                TransactionType.INCOME -> "+$currency${numberFormatter.format(tx.amount)}"
                                TransactionType.EXPENSE -> "-$currency${numberFormatter.format(tx.amount)}"
                                TransactionType.TRANSFER -> "⇄ $currency${numberFormatter.format(tx.amount)}"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .clickable { onUpdateTransaction(tx) }
                                    .padding(vertical = 6.dp)
                                    .testTag("spreadsheet_row_${tx.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. SERIAL NUMBER (SL)
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(colSlWidth),
                                    maxLines = 1
                                )

                                // 2. DATE
                                Text(
                                    text = tx.dateString,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .width(colDateWidth)
                                        .padding(horizontal = 6.dp),
                                    maxLines = 1
                                )

                                // 3. TYPE
                                Text(
                                    text = tx.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = amtColor,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .width(colTypeWidth)
                                        .padding(horizontal = 6.dp),
                                    maxLines = 1
                                )

                                // 4. CATEGORY
                                Text(
                                    text = tx.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .width(colCatWidth)
                                        .padding(horizontal = 6.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // 5. ACCOUNT
                                Text(
                                    text = tx.account,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .width(colAccountWidth)
                                        .padding(horizontal = 6.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // 6. NOTE
                                Text(
                                    text = tx.note.ifBlank { "—" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .width(colNoteWidth)
                                        .padding(horizontal = 6.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // 7. AMOUNT
                                Text(
                                    text = amtText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = amtColor,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .width(colAmountWidth)
                                        .padding(horizontal = 6.dp),
                                    maxLines = 1
                                )

                                // 8. ACTION (Direct Update Button)
                                Box(
                                    modifier = Modifier
                                        .width(colActionWidth)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { onUpdateTransaction(tx) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("spreadsheet_edit_btn_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit / Update Record",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isSorted: Boolean,
    sortOrder: SortOrder,
    textAlign: TextAlign = TextAlign.Start,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (textAlign) {
            TextAlign.End -> Arrangement.End
            TextAlign.Center -> Arrangement.Center
            else -> Arrangement.Start
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign
        )
        if (isSorted) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = if (sortOrder == SortOrder.DESC) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
