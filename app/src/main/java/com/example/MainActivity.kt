package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.NewLogBottomSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SpreadsheetScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

enum class MainNavTab {
    HOME,
    LOGS,
    SPREADSHEET,
    REPORTS,
    MORE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    financeViewModel: FinanceViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var isNewLogSheetOpen by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val currency by financeViewModel.currencySymbol.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 70.dp)
            )
        },
        bottomBar = {
            NativeBottomBar(
                currentTab = currentTab,
                onTabSelect = { tab -> currentTab = tab },
                onNewLogClick = {
                    editingTransaction = null
                    isNewLogSheetOpen = true
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Crossfade(
                targetState = currentTab,
                label = "screen_crossfade"
            ) { tab ->
                when (tab) {
                    MainNavTab.HOME -> {
                        HomeScreen(
                            viewModel = financeViewModel,
                            onOpenNewLog = { tx ->
                                editingTransaction = tx
                                isNewLogSheetOpen = true
                            },
                            onNavigateToLogs = { currentTab = MainNavTab.LOGS }
                        )
                    }
                    MainNavTab.LOGS -> {
                        LogsScreen(
                            viewModel = financeViewModel,
                            onOpenNewLog = { tx ->
                                editingTransaction = tx
                                isNewLogSheetOpen = true
                            }
                        )
                    }
                    MainNavTab.SPREADSHEET -> {
                        SpreadsheetScreen(
                            viewModel = financeViewModel,
                            onOpenNewLog = { tx ->
                                editingTransaction = tx
                                isNewLogSheetOpen = true
                            }
                        )
                    }
                    MainNavTab.REPORTS -> {
                        ReportsScreen(
                            viewModel = financeViewModel
                        )
                    }
                    MainNavTab.MORE -> {
                        MoreScreen(
                            viewModel = financeViewModel
                        )
                    }
                }
            }
        }
    }

    // New Log Bottom Sheet
    if (isNewLogSheetOpen) {
        NewLogBottomSheet(
            sheetState = sheetState,
            currencySymbol = currency,
            editingTransaction = editingTransaction,
            onDismiss = {
                isNewLogSheetOpen = false
                editingTransaction = null
            },
            onSave = { type, amount, category, account, toAccount, note, timestamp, imgUri, fUri, fName, rLink ->
                if (editingTransaction != null) {
                    val updated = editingTransaction!!.copy(
                        type = type,
                        amount = amount,
                        category = category,
                        account = account,
                        toAccount = toAccount,
                        note = note,
                        timestamp = timestamp,
                        imageUri = imgUri,
                        fileUri = fUri,
                        fileName = fName,
                        remoteLink = rLink
                    )
                    financeViewModel.updateTransaction(updated)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Transaction updated")
                    }
                } else {
                    financeViewModel.addTransaction(
                        type = type,
                        amount = amount,
                        category = category,
                        account = account,
                        toAccount = toAccount,
                        note = note,
                        timestamp = timestamp,
                        imageUri = imgUri,
                        fileUri = fUri,
                        fileName = fName,
                        remoteLink = rLink
                    )
                    val msg = when (type) {
                        TransactionType.EXPENSE -> "Expense added"
                        TransactionType.INCOME -> "Income recorded"
                        TransactionType.TRANSFER -> "Transfer recorded"
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
                isNewLogSheetOpen = false
                editingTransaction = null
            },
            onDelete = { tx ->
                financeViewModel.deleteTransaction(tx)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Transaction removed")
                }
            }
        )
    }
}

@Composable
fun NativeBottomBar(
    currentTab: MainNavTab,
    onTabSelect: (MainNavTab) -> Unit,
    onNewLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 1. Home
            BottomNavItem(
                label = "Home",
                selected = currentTab == MainNavTab.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onTabSelect(MainNavTab.HOME) },
                testTag = "nav_home"
            )

            // 2. Logs
            BottomNavItem(
                label = "Logs",
                selected = currentTab == MainNavTab.LOGS,
                selectedIcon = Icons.Filled.ReceiptLong,
                unselectedIcon = Icons.Outlined.ReceiptLong,
                onClick = { onTabSelect(MainNavTab.LOGS) },
                testTag = "nav_logs"
            )

            // 3. Central New Log (+) Action
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNewLogClick)
                    .testTag("nav_new_log_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Log",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 4. Ledger / Spreadsheet
            BottomNavItem(
                label = "Ledger",
                selected = currentTab == MainNavTab.SPREADSHEET,
                selectedIcon = Icons.Filled.TableChart,
                unselectedIcon = Icons.Filled.TableChart,
                onClick = { onTabSelect(MainNavTab.SPREADSHEET) },
                testTag = "nav_ledger"
            )

            // 5. Reports
            BottomNavItem(
                label = "Reports",
                selected = currentTab == MainNavTab.REPORTS,
                selectedIcon = Icons.Filled.BarChart,
                unselectedIcon = Icons.Outlined.BarChart,
                onClick = { onTabSelect(MainNavTab.REPORTS) },
                testTag = "nav_reports"
            )

            // 6. More
            BottomNavItem(
                label = "More",
                selected = currentTab == MainNavTab.MORE,
                selectedIcon = Icons.Filled.MoreHoriz,
                unselectedIcon = Icons.Outlined.MoreHoriz,
                onClick = { onTabSelect(MainNavTab.MORE) },
                testTag = "nav_more"
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
