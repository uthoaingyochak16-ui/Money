package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val account: String,
    val toAccount: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val dateString: String, // YYYY-MM-DD for fast grouping and filtering
    val imageUri: String? = null,
    val fileUri: String? = null,
    val fileName: String? = null,
    val remoteLink: String? = null,
    val isAutoLoggedFromSms: Boolean = false,
    val rawSmsBody: String? = null
)
