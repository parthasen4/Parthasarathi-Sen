package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    accounts: List<Account>,
    investments: List<Investment>,
    loans: List<Loan>,
    transactions: List<Transaction>,
    categories: List<Category>,
    onAddTransactionClick: () -> Unit,
    onQuickActionClick: (TransactionType) -> Unit,
    onConvertToEmiClick: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val accountsMap = accounts.associateBy { it.id }
    val categoriesMap = categories.associateBy { it.id }
    val loansMap = loans.associateBy { it.id }

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            NetWorthHeroCard(
                accounts = accounts,
                investments = investments,
                loans = loans
            )
        }

        // Quick Action Buttons
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionChip(
                        icon = Icons.Default.Add,
                        label = "New Transaction",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onAddTransactionClick,
                        tag = "quick_action_add_tx"
                    )
                    QuickActionChip(
                        icon = Icons.Default.SwapHoriz,
                        label = "Transfer",
                        color = TransferBlue,
                        onClick = { onQuickActionClick(TransactionType.TRANSFER) },
                        tag = "quick_action_transfer"
                    )
                    QuickActionChip(
                        icon = Icons.Default.CreditCard,
                        label = "Pay Card Bill",
                        color = CreditOrange,
                        onClick = { onQuickActionClick(TransactionType.CARD_BILL_PAY) },
                        tag = "quick_action_card_bill"
                    )
                    QuickActionChip(
                        icon = Icons.Default.CurrencyExchange,
                        label = "Card EMI",
                        color = CreditOrange,
                        onClick = onConvertToEmiClick,
                        tag = "quick_action_card_emi"
                    )
                    QuickActionChip(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Pay Loan EMI",
                        color = LoanPurple,
                        onClick = { onQuickActionClick(TransactionType.LOAN_REPAY) },
                        tag = "quick_action_loan_repay"
                    )
                }
            }
        }

        // Cashflow Overview
        item {
            CashflowComparisonCard(
                totalIncome = totalIncome,
                totalExpense = totalExpense
            )
        }

        // Accounts Glance
        item {
            Column {
                Text(
                    text = "Accounts (${accounts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                accounts.take(3).forEach { account ->
                    AccountCard(
                        account = account,
                        onConvertToEmi = { onConvertToEmiClick() },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onSeeAllTransactions,
                    modifier = Modifier.testTag("see_all_transactions_button")
                ) {
                    Text("See All")
                }
            }
        }

        // Recent Transactions List
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions logged yet. Tap '+' to start!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                TransactionRow(
                    transaction = tx,
                    accountsMap = accountsMap,
                    categoriesMap = categoriesMap,
                    loansMap = loansMap,
                    onDelete = { onDeleteTransaction(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp)) // Padding for bottom bar & FAB
        }
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    tag: String
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
