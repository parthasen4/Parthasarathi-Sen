package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.FinanceUtils
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    loans: List<Loan>,
    investments: List<Investment>,
    modifier: Modifier = Modifier
) {
    val categoriesMap = categories.associateBy { it.id }

    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpense = expenseTransactions.sumOf { it.amount }
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    // Group expenses by category
    val expensesByCategory = expenseTransactions
        .groupBy { it.categoryId }
        .map { (catId, txs) ->
            val label = catId?.let { categoriesMap[it]?.name } ?: "General Expense"
            val total = txs.sumOf { it.amount }
            label to total
        }
        .sortedByDescending { it.second }

    val slices = expensesByCategory.mapIndexed { index, (label, value) ->
        SliceData(
            label = label,
            value = value,
            color = ChartPalette[index % ChartPalette.size]
        )
    }

    // Health metrics
    val liquidAssets = accounts.filter { it.type != AccountType.CREDIT_CARD }.sumOf { it.currentBalance }
    val investmentAssets = investments.sumOf { it.principalAmount }
    val totalAssets = liquidAssets + investmentAssets

    val cardDebts = accounts.filter { it.type == AccountType.CREDIT_CARD }.sumOf { if (it.currentBalance < 0) abs(it.currentBalance) else 0.0 }
    val loanDebts = loans.sumOf { it.remainingPrincipal }
    val totalLiabilities = cardDebts + loanDebts

    val savingsRate = if (totalIncome > 0) {
        (((totalIncome - totalExpense) / totalIncome) * 100).coerceIn(-100.0, 100.0)
    } else 0.0

    val debtToAsset = if (totalAssets > 0) {
        ((totalLiabilities / totalAssets) * 100).coerceAtLeast(0.0)
    } else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Financial Intelligence",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Category Donut Chart
        item {
            CategorySpendingDonutChart(
                slices = slices,
                totalExpense = totalExpense
            )
        }

        // Cashflow Overview Card
        item {
            CashflowComparisonCard(
                totalIncome = totalIncome,
                totalExpense = totalExpense
            )
        }

        // Key Ratios Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Financial Health Ratios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Savings Rate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${savingsRate.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (savingsRate >= 20.0) IncomeGreen else ExpenseRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Debt to Assets", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${debtToAsset.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (debtToAsset < 40.0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (savingsRate >= 20.0) "Great job! A savings rate above 20% indicates healthy financial growth."
                        else "Tip: Try lowering discretionary expenses to improve your monthly savings rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
