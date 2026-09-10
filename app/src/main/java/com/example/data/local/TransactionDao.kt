package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MonthlyBudgetEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM transactions WHERE dateString LIKE :monthPrefix || '%' ORDER BY timestamp DESC")
    fun getTransactionsByMonth(monthPrefix: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateString = :date ORDER BY timestamp DESC")
    fun getTransactionsByDate(date: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM monthly_budgets WHERE monthYear = :monthYear LIMIT 1")
    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: MonthlyBudgetEntity)
}
