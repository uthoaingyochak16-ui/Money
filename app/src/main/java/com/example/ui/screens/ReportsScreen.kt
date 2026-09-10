package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryConstants
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.viewmodel.FinanceViewModel
import java.text.DecimalFormat

@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.monthSummary.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val categorySpending by viewModel.monthlyCategorySpending.collectAsStateWithLifecycle()
    val dailySpending by viewModel.monthlyDailySpending.collectAsStateWithLifecycle()
    val numberFormatter = remember { DecimalFormat("#,##0") }

    val totalExpense = summary.totalExpense
    val totalIncome = summary.totalIncome
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt().coerceAtLeast(0) else 0

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val incomeColor = if (isDark) IncomeGreenDark else IncomeGreenLight
    val expenseColor = if (isDark) ExpenseRedDark else ExpenseRedLight

    // Muted semantic chart palette for categories
    val categoryColors = remember {
        listOf(
            Color(0xFF334155), // Slate
            Color(0xFF475569),
            Color(0xFF64748B),
            Color(0xFF94A3B8),
            Color(0xFF0F766E), // Teal
            Color(0xFF0369A1), // Sky
            Color(0xFF4338CA), // Indigo
            Color(0xFF7C3AED), // Violet
            Color(0xFFB45309)  // Amber
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Financial Analysis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary.monthDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 1: Month Overview Balance & Savings
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Cash Flow Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Income",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+$currency${numberFormatter.format(totalIncome)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = incomeColor
                        )
                    }

                    Column {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-$currency${numberFormatter.format(totalExpense)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = expenseColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net Saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currency${numberFormatter.format(netSavings)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Savings rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$savingsRate%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section 2: "Where did my money go?" Category Breakdown
        item {
            Text(
                text = "Where did my money go?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Spending distribution by category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categorySpending.isEmpty()) {
                Text(
                    text = "No spending recorded this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                // Donut Ring Chart & Legend Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            var startAngle = -90f
                            val strokeWidth = 18.dp.toPx()

                            categorySpending.forEachIndexed { index, pair ->
                                val sweep = if (totalExpense > 0) {
                                    (pair.second / totalExpense).toFloat() * 360f
                                } else 0f
                                val color = categoryColors[index % categoryColors.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currency${numberFormatter.format(totalExpense)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Top 3-4 categories summary list
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categorySpending.take(4).forEachIndexed { index, (cat, amt) ->
                            val pct = if (totalExpense > 0) ((amt / totalExpense) * 100).toInt() else 0
                            val color = categoryColors[index % categoryColors.size]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Bars per Category
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categorySpending.forEachIndexed { index, (cat, amt) ->
                        val pct = if (totalExpense > 0) ((amt / totalExpense) * 100).toInt() else 0
                        val barColor = categoryColors[index % categoryColors.size]

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryIconBadge(
                                        category = cat,
                                        type = TransactionType.EXPENSE,
                                        size = 24.dp,
                                        iconSize = 13.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$currency${numberFormatter.format(amt)} ($pct%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { if (totalExpense > 0) (amt / totalExpense).toFloat() else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 3: "How much did I spend each day?" Daily Spending Chart
        item {
            Text(
                text = "Daily spending trend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daily expense progression through the month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val maxDailyExpense = dailySpending.maxOfOrNull { it.second } ?: 1.0
            val chartHeight = 120.dp
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outline

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barCount = dailySpending.size
                    val barSlot = w / barCount.coerceAtLeast(1)
                    val barWidth = (barSlot * 0.6f).coerceAtLeast(2f)

                    // Draw subtle baseline
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1.dp.toPx()
                    )

                    dailySpending.forEachIndexed { i, pair ->
                        val expenseVal = pair.second
                        if (expenseVal > 0) {
                            val barHeight = ((expenseVal / maxDailyExpense.coerceAtLeast(1.0)) * (h - 10f)).toFloat()
                            val x = i * barSlot + (barSlot - barWidth) / 2
                            val y = h - barHeight

                            drawRect(
                                color = primaryColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Day 1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Day 15",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Day 30",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
