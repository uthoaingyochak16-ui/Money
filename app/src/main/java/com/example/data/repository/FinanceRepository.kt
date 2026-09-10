package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.model.MonthlyBudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsForMonth(monthPrefix: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMonth(monthPrefix)

    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudgetEntity?> =
        transactionDao.getBudgetForMonth(monthYear)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun setBudget(monthYear: String, amount: Double) =
        transactionDao.setBudget(MonthlyBudgetEntity(monthYear, amount))

    suspend fun clearAll() =
        transactionDao.clearAllTransactions()

    suspend fun seedInitialDataIfEmpty() {
        if (transactionDao.getTransactionCount() > 0) return

        // Populate realistic starter data matching the prompt's scenario
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonth = monthFormat.format(cal.time)

        // Set default monthly budget of 30,000
        transactionDao.setBudget(MonthlyBudgetEntity(currentMonth, 30000.0))

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        cal.timeInMillis = now
        val todayStr = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgoStr = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -2)
        val fourDaysAgoStr = dateFormat.format(cal.time)

        // Previous month dates
        val prevCal1 = Calendar.getInstance()
        prevCal1.add(Calendar.MONTH, -1)
        prevCal1.set(Calendar.DAY_OF_MONTH, 28)
        val prevMonth1Day28 = dateFormat.format(prevCal1.time)
        val prevMonth1Key = monthFormat.format(prevCal1.time)
        transactionDao.setBudget(MonthlyBudgetEntity(prevMonth1Key, 30000.0))

        prevCal1.set(Calendar.DAY_OF_MONTH, 15)
        val prevMonth1Day15 = dateFormat.format(prevCal1.time)

        prevCal1.set(Calendar.DAY_OF_MONTH, 2)
        val prevMonth1Day02 = dateFormat.format(prevCal1.time)

        // Two months ago dates
        val prevCal2 = Calendar.getInstance()
        prevCal2.add(Calendar.MONTH, -2)
        prevCal2.set(Calendar.DAY_OF_MONTH, 25)
        val prevMonth2Day25 = dateFormat.format(prevCal2.time)
        val prevMonth2Key = monthFormat.format(prevCal2.time)
        transactionDao.setBudget(MonthlyBudgetEntity(prevMonth2Key, 28000.0))

        prevCal2.set(Calendar.DAY_OF_MONTH, 1)
        val prevMonth2Day01 = dateFormat.format(prevCal2.time)

        val starterTransactions = listOf(
            // Today (Current Month)
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 250.0,
                category = "Food",
                account = "bKash",
                timestamp = now - (2 * 60 * 60 * 1000L),
                note = "Lunch",
                dateString = todayStr
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 100.0,
                category = "Transport",
                account = "Cash",
                timestamp = now - (4 * 60 * 60 * 1000L),
                note = "Bus",
                dateString = todayStr
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 400.0,
                category = "Shopping",
                account = "bKash",
                timestamp = now - (6 * 60 * 60 * 1000L),
                note = "Stationery & household",
                dateString = todayStr
            ),

            // Yesterday
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 500.0,
                category = "Groceries",
                account = "Cash",
                timestamp = now - dayMillis - (3 * 60 * 60 * 1000L),
                note = "Vegetables & fruits",
                dateString = yesterdayStr
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 80.0,
                category = "Transport",
                account = "Cash",
                timestamp = now - dayMillis - (7 * 60 * 60 * 1000L),
                note = "Rickshaw fare",
                dateString = yesterdayStr
            ),

            // Earlier this month
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 30000.0,
                category = "Salary",
                account = "Bank",
                timestamp = now - (7 * dayMillis),
                note = "Monthly Salary",
                dateString = fourDaysAgoStr
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 7500.0,
                category = "Rent",
                account = "Bank",
                timestamp = now - (6 * dayMillis),
                note = "Apartment Rent share",
                dateString = fourDaysAgoStr
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 1200.0,
                category = "Bills",
                account = "bKash",
                timestamp = now - (5 * dayMillis),
                note = "Electricity & Internet Bill",
                dateString = twoDaysAgoStr
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 1470.0,
                category = "Groceries",
                account = "Nagad",
                timestamp = now - (3 * dayMillis),
                note = "Weekly Supermarket",
                dateString = twoDaysAgoStr
            ),

            // Previous Month Records
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 30000.0,
                category = "Salary",
                account = "Bank",
                timestamp = now - (32 * dayMillis),
                note = "Monthly Salary",
                dateString = prevMonth1Day02
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 7500.0,
                category = "Rent",
                account = "Bank",
                timestamp = now - (30 * dayMillis),
                note = "Apartment Rent share",
                dateString = prevMonth1Day02
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 3800.0,
                category = "Groceries",
                account = "Cash",
                timestamp = now - (22 * dayMillis),
                note = "Monthly Grocery & Provisions",
                dateString = prevMonth1Day15
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 1600.0,
                category = "Bills",
                account = "bKash",
                timestamp = now - (15 * dayMillis),
                note = "Utilities and Wifi",
                dateString = prevMonth1Day28
            ),

            // Two Months Ago Records
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 28000.0,
                category = "Salary",
                account = "Bank",
                timestamp = now - (62 * dayMillis),
                note = "Monthly Salary",
                dateString = prevMonth2Day01
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 7500.0,
                category = "Rent",
                account = "Bank",
                timestamp = now - (60 * dayMillis),
                note = "Apartment Rent",
                dateString = prevMonth2Day01
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 4100.0,
                category = "Shopping",
                account = "bKash",
                timestamp = now - (45 * dayMillis),
                note = "Clothing and electronics",
                dateString = prevMonth2Day25
            )
        )

        transactionDao.insertTransactions(starterTransactions)
    }
}
