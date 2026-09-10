package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey
    val monthYear: String, // format: "2026-09"
    val budgetAmount: Double
)
