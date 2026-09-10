package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType

object CategoryConstants {
    val expenseCategories = listOf(
        "Food",
        "Transport",
        "Shopping",
        "Groceries",
        "Bills",
        "Rent",
        "Entertainment",
        "Health",
        "Education",
        "Others"
    )

    val incomeCategories = listOf(
        "Salary",
        "Freelance",
        "Business",
        "Investment",
        "Gift",
        "Others"
    )

    val defaultAccounts = listOf(
        "Cash",
        "bKash",
        "Bank",
        "Nagad",
        "Credit Card"
    )

    fun getIconForCategory(category: String, type: TransactionType): ImageVector {
        if (type == TransactionType.TRANSFER) return Icons.Default.SwapHoriz
        return when (category.lowercase()) {
            "food", "lunch", "dinner", "breakfast", "cafe" -> Icons.Default.Restaurant
            "transport", "bus", "train", "taxi", "fuel", "ride" -> Icons.Default.DirectionsBus
            "shopping", "clothes" -> Icons.Default.ShoppingBag
            "groceries", "supermarket", "market" -> Icons.Default.LocalGroceryStore
            "bills", "utilities", "electricity", "water", "internet" -> Icons.Default.ReceiptLong
            "rent", "housing" -> Icons.Default.Home
            "entertainment", "movies", "games", "streaming" -> Icons.Default.Movie
            "health", "medical", "pharmacy", "doctor" -> Icons.Default.LocalHospital
            "education", "tuition", "books" -> Icons.Default.School
            "salary" -> Icons.Default.Paid
            "freelance", "business", "work" -> Icons.Default.Work
            "investment" -> Icons.Default.AccountBalance
            "gift" -> Icons.Default.AttachMoney
            else -> if (type == TransactionType.INCOME) Icons.Default.Paid else Icons.Default.MoreHoriz
        }
    }
}

@Composable
fun CategoryIconBadge(
    category: String,
    type: TransactionType,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp
) {
    val icon = CategoryConstants.getIconForCategory(category, type)
    val (bgColor, tintColor) = when (type) {
        TransactionType.INCOME -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            Color(0xFF15803D)
        )
        TransactionType.EXPENSE -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        TransactionType.TRANSFER -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            Color(0xFF1D4ED8)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
