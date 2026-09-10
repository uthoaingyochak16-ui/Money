package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthSummary(
    val monthDisplay: String, // e.g., "September 2026"
    val monthYearKey: String, // e.g., "2026-09"
    val totalIncome: Double,
    val totalExpense: Double,
    val budgetAmount: Double,
    val remainingAmount: Double,
    val todayExpense: Double,
    val todayCategoryBreakdown: List<Pair<String, Double>>,
    val recentTransactions: List<TransactionEntity>
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val numberFormatter = DecimalFormat("#,##0")

    val currencySymbol = MutableStateFlow("৳")

    // Calendar & month selection
    private val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val monthDisplayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val currentMonthKey = MutableStateFlow(monthYearFormat.format(Date()))
    val currentMonthDisplay = MutableStateFlow(monthDisplayFormat.format(Date()))

    // Search and filter in Logs/Spreadsheet
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<TransactionType?>(null)
    val filterCategory = MutableStateFlow<String?>(null)
    val filterAccount = MutableStateFlow<String?>(null)

    // Raw transactions flow from Room
    val allTransactions: StateFlow<List<TransactionEntity>>

    // Monthly summary calculation
    val monthSummary: StateFlow<MonthSummary>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db.transactionDao())

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        allTransactions = repository.allTransactions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        monthSummary = combine(
            allTransactions,
            currentMonthKey,
            currentMonthDisplay
        ) { transactions, monthKey, monthDisplay ->
            val todayStr = dateFormat.format(Date())

            val currentMonthTx = transactions.filter { it.dateString.startsWith(monthKey) }

            val income = currentMonthTx
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val expense = currentMonthTx
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            // Default budget is 30,000 if not set, or income if higher
            val budget = if (income > 0) income else 30000.0
            val remaining = (if (budget > 0) budget else income) - expense

            val todayTx = currentMonthTx.filter { it.dateString == todayStr }
            val todayExpense = todayTx
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val todayCatMap = todayTx
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            val recent = transactions.take(15)

            MonthSummary(
                monthDisplay = monthDisplay,
                monthYearKey = monthKey,
                totalIncome = income,
                totalExpense = expense,
                budgetAmount = budget,
                remainingAmount = remaining,
                todayExpense = todayExpense,
                todayCategoryBreakdown = todayCatMap,
                recentTransactions = recent
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthSummary(
                monthDisplay = monthDisplayFormat.format(Date()),
                monthYearKey = monthYearFormat.format(Date()),
                totalIncome = 0.0,
                totalExpense = 0.0,
                budgetAmount = 30000.0,
                remainingAmount = 30000.0,
                todayExpense = 0.0,
                todayCategoryBreakdown = emptyList(),
                recentTransactions = emptyList()
            )
        )
    }

    // Filtered transactions for Logs & Spreadsheet
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        filterType,
        filterCategory,
        filterAccount
    ) { list, query, type, cat, account ->
        list.filter { tx ->
            val matchesQuery = query.isBlank() ||
                tx.note.contains(query, ignoreCase = true) ||
                tx.category.contains(query, ignoreCase = true) ||
                tx.account.contains(query, ignoreCase = true) ||
                tx.amount.toString().contains(query)

            val matchesType = type == null || tx.type == type
            val matchesCat = cat == null || tx.category.equals(cat, ignoreCase = true)
            val matchesAccount = account == null || tx.account.equals(account, ignoreCase = true)

            matchesQuery && matchesType && matchesCat && matchesAccount
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Account Balances
    val accountBalances: StateFlow<Map<String, Double>> = allTransactions.combine(
        MutableStateFlow(Unit)
    ) { list, _ ->
        val balances = mutableMapOf(
            "Cash" to 0.0,
            "bKash" to 0.0,
            "Bank" to 0.0,
            "Nagad" to 0.0
        )
        for (tx in list) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    val current = balances[tx.account] ?: 0.0
                    balances[tx.account] = current + tx.amount
                }
                TransactionType.EXPENSE -> {
                    val current = balances[tx.account] ?: 0.0
                    balances[tx.account] = current - tx.amount
                }
                TransactionType.TRANSFER -> {
                    val fromCurrent = balances[tx.account] ?: 0.0
                    balances[tx.account] = fromCurrent - tx.amount
                    if (!tx.toAccount.isNullOrBlank()) {
                        val toCurrent = balances[tx.toAccount] ?: 0.0
                        balances[tx.toAccount] = toCurrent + tx.amount
                    }
                }
            }
        }
        balances
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = mapOf("Cash" to 0.0, "bKash" to 0.0, "Bank" to 0.0, "Nagad" to 0.0)
    )

    // Category Spending for current month (Reports)
    val monthlyCategorySpending: StateFlow<List<Pair<String, Double>>> = combine(
        allTransactions,
        currentMonthKey
    ) { list, monthKey ->
        list.filter { it.dateString.startsWith(monthKey) && it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Daily spending trend for current month (Reports)
    val monthlyDailySpending: StateFlow<List<Pair<Int, Double>>> = combine(
        allTransactions,
        currentMonthKey
    ) { list, monthKey ->
        val daysInMonth = 30
        val dailyMap = mutableMapOf<Int, Double>()
        for (i in 1..daysInMonth) {
            dailyMap[i] = 0.0
        }

        list.filter { it.dateString.startsWith(monthKey) && it.type == TransactionType.EXPENSE }
            .forEach { tx ->
                val day = tx.dateString.substringAfterLast("-").toIntOrNull() ?: 1
                val cur = dailyMap[day] ?: 0.0
                dailyMap[day] = cur + tx.amount
            }

        dailyMap.toList().sortedBy { it.first }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun formatAmount(amount: Double): String {
        return "${currencySymbol.value}${numberFormatter.format(amount)}"
    }

    fun formatSignedAmount(type: TransactionType, amount: Double): String {
        val sym = currencySymbol.value
        val formatted = numberFormatter.format(amount)
        return when (type) {
            TransactionType.INCOME -> "+$sym$formatted"
            TransactionType.EXPENSE -> "-$sym$formatted"
            TransactionType.TRANSFER -> "⇄ $sym$formatted"
        }
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        account: String,
        toAccount: String? = null,
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        imageUri: String? = null,
        fileUri: String? = null,
        fileName: String? = null,
        remoteLink: String? = null,
        isAutoLoggedFromSms: Boolean = false,
        rawSmsBody: String? = null
    ) {
        viewModelScope.launch {
            val dateStr = dateFormat.format(Date(timestamp))
            val entity = TransactionEntity(
                type = type,
                amount = amount,
                category = category,
                account = account,
                toAccount = toAccount,
                timestamp = timestamp,
                note = note.trim(),
                dateString = dateStr,
                imageUri = imageUri,
                fileUri = fileUri,
                fileName = fileName,
                remoteLink = remoteLink?.trim()?.ifBlank { null },
                isAutoLoggedFromSms = isAutoLoggedFromSms,
                rawSmsBody = rawSmsBody
            )
            repository.insertTransaction(entity)
        }
    }

    // AI Advisor State
    val aiAdvisorResponse = MutableStateFlow<String?>(null)
    val isAiThinking = MutableStateFlow(false)

    fun askAiAdvisor(question: String) {
        viewModelScope.launch {
            isAiThinking.value = true
            val summary = monthSummary.value
            val context = """
                Month: ${summary.monthDisplay}
                Total Inflow: ${currencySymbol.value}${summary.totalIncome}
                Total Outflow: ${currencySymbol.value}${summary.totalExpense}
                Monthly Budget: ${currencySymbol.value}${summary.budgetAmount}
                Remaining Balance: ${currencySymbol.value}${summary.remainingAmount}
                Recent Expenses: ${summary.todayCategoryBreakdown.joinToString { "${it.first}: ${currencySymbol.value}${it.second}" }}
            """.trimIndent()

            val response = com.example.data.ai.GeminiAiService.askFinancialAdvisor(context, question)
            aiAdvisorResponse.value = response
            isAiThinking.value = false
        }
    }

    // Cellular SMS Simulation and Inbox Sync
    fun simulateSmsTransaction(sender: String, body: String, onComplete: ((TransactionEntity?) -> Unit)? = null) {
        viewModelScope.launch {
            val tx = com.example.data.sms.SmsSyncHelper.simulateIncomingSms(getApplication(), sender, body)
            onComplete?.invoke(tx)
        }
    }

    fun syncInboxSms(onResult: (com.example.data.sms.SmsScanSummary) -> Unit) {
        viewModelScope.launch {
            val summary = com.example.data.sms.SmsSyncHelper.scanAndSyncInbox(getApplication())
            onResult(summary)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            repository.setBudget(currentMonthKey.value, amount)
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.clearAll()
            repository.seedInitialDataIfEmpty()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun getDateGroupHeader(dateString: String): String {
        val today = dateFormat.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(cal.time)

        return when (dateString) {
            today -> "TODAY"
            yesterday -> "YESTERDAY"
            else -> {
                try {
                    val date = dateFormat.parse(dateString)
                    val headerFormat = SimpleDateFormat("dd MMMM, EEE", Locale.getDefault())
                    if (date != null) headerFormat.format(date).uppercase(Locale.getDefault()) else dateString
                } catch (e: Exception) {
                    dateString
                }
            }
        }
    }

    fun getGreeting(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }
}
