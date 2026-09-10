package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val presetQueries = listOf(
        "How can I cut expenses this month?",
        "Analyze my top spending categories",
        "Am I on track with my monthly budget?",
        "Give me 3 practical savings tips",
        "Explain how bKash & Bank auto-logging works"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini Financial AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Powered by Google Gemini 3.5 Flash. Ask anything about your financial budget, savings strategy, or category breakdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Prompt Suggestion Chips
                Text(
                    text = "Suggested Questions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetQueries.forEach { query ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    queryText = query
                                    isLoading = true
                                    coroutineScope.launch {
                                        val summary = viewModel.monthSummary.value
                                        val context = """
                                            Month: ${summary.monthDisplay}
                                            Income: ৳${summary.totalIncome.toInt()}
                                            Expense: ৳${summary.totalExpense.toInt()}
                                            Budget: ৳${summary.budgetAmount.toInt()}
                                            Remaining: ৳${summary.remainingAmount.toInt()}
                                        """.trimIndent()
                                        aiAnswer = com.example.data.ai.GeminiAiService.askFinancialAdvisor(context, query)
                                        isLoading = false
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = query,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // Input box with send button
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("Ask your financial question...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_advisor_input"),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (queryText.isNotBlank()) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val summary = viewModel.monthSummary.value
                                        val context = """
                                            Month: ${summary.monthDisplay}
                                            Income: ৳${summary.totalIncome.toInt()}
                                            Expense: ৳${summary.totalExpense.toInt()}
                                            Budget: ৳${summary.budgetAmount.toInt()}
                                            Remaining: ৳${summary.remainingAmount.toInt()}
                                        """.trimIndent()
                                        aiAnswer = com.example.data.ai.GeminiAiService.askFinancialAdvisor(context, queryText)
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = queryText.isNotBlank() && !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (queryText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    textStyle = TextStyle(fontSize = 13.sp)
                )

                // AI Response Area
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini is analyzing your finances...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (aiAnswer != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Financial Insights",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiAnswer!!,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
