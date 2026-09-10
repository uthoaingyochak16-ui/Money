package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.TransferBlueDark
import com.example.ui.theme.TransferBlueLight
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormatter = DecimalFormat("#,##0")
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(transaction.timestamp))

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> if (androidx.compose.foundation.isSystemInDarkTheme()) IncomeGreenDark else IncomeGreenLight
        TransactionType.EXPENSE -> if (androidx.compose.foundation.isSystemInDarkTheme()) ExpenseRedDark else ExpenseRedLight
        TransactionType.TRANSFER -> if (androidx.compose.foundation.isSystemInDarkTheme()) TransferBlueDark else TransferBlueLight
    }

    val amountText = when (transaction.type) {
        TransactionType.INCOME -> "+$currencySymbol${numberFormatter.format(transaction.amount)}"
        TransactionType.EXPENSE -> "-$currencySymbol${numberFormatter.format(transaction.amount)}"
        TransactionType.TRANSFER -> "⇄ $currencySymbol${numberFormatter.format(transaction.amount)}"
    }

    val accountDisplay = if (transaction.type == TransactionType.TRANSFER && !transaction.toAccount.isNullOrBlank()) {
        "${transaction.account} → ${transaction.toAccount}"
    } else {
        transaction.account
    }

    val hasAttachments = !transaction.imageUri.isNullOrBlank() ||
        !transaction.fileUri.isNullOrBlank() ||
        !transaction.remoteLink.isNullOrBlank() ||
        transaction.isAutoLoggedFromSms

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("transaction_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        CategoryIconBadge(
            category = transaction.category,
            type = transaction.type,
            size = 38.dp,
            iconSize = 19.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Left info: Category, Note, Account, and Attachments
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Text(
                    text = accountDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Attachment badges row
            if (hasAttachments) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (transaction.isAutoLoggedFromSms) {
                        AttachmentTag(icon = Icons.Default.Sms, label = "SMS")
                    }
                    if (!transaction.imageUri.isNullOrBlank()) {
                        AttachmentTag(icon = Icons.Default.Image, label = "Photo")
                    }
                    if (!transaction.fileUri.isNullOrBlank()) {
                        AttachmentTag(icon = Icons.Default.Description, label = "Doc")
                    }
                    if (!transaction.remoteLink.isNullOrBlank()) {
                        AttachmentTag(icon = Icons.Default.Link, label = "Link")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right info: Amount & Time
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = amountColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AttachmentTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
