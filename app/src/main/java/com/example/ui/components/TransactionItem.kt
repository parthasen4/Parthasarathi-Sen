package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.FinanceUtils

@Composable
fun TransactionRow(
    transaction: Transaction,
    accountsMap: Map<Long, Account>,
    categoriesMap: Map<Long, Category>,
    loansMap: Map<Long, Loan>,
    onDelete: () -> Unit,
    onConvertToEmi: ((Account, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fromAcc = accountsMap[transaction.fromAccountId]
    val toAcc = transaction.toAccountId?.let { accountsMap[it] }
    val category = transaction.categoryId?.let { categoriesMap[it] }
    val loan = transaction.linkedLoanId?.let { loansMap[it] }

    val icon: ImageVector
    val color: Color
    val prefix: String
    val title: String

    when (transaction.type) {
        TransactionType.INCOME -> {
            icon = Icons.Default.ArrowDownward
            color = IncomeGreen
            prefix = "+"
            title = category?.name ?: "Income"
        }
        TransactionType.EXPENSE -> {
            icon = Icons.Default.ArrowUpward
            color = ExpenseRed
            prefix = "-"
            title = category?.name ?: "Expense"
        }
        TransactionType.TRANSFER -> {
            icon = Icons.Default.SwapHoriz
            color = TransferBlue
            prefix = ""
            title = "Transfer → ${toAcc?.name ?: "Account"}"
        }
        TransactionType.CARD_BILL_PAY -> {
            icon = Icons.Default.CreditCard
            color = CreditOrange
            prefix = ""
            title = "Bill Pay → ${toAcc?.name ?: "Credit Card"}"
        }
        TransactionType.LOAN_REPAY -> {
            icon = Icons.Default.AccountBalanceWallet
            color = LoanPurple
            prefix = ""
            title = "EMI: ${loan?.lenderName ?: "Loan"}"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_row_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = transaction.type.name, tint = color)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = fromAcc?.name ?: "Account",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinanceUtils.formatDdMmYyyyToDisplay(transaction.dateDdMmYyyy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!transaction.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.notes ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }

                if (fromAcc?.type == AccountType.CREDIT_CARD && transaction.type == TransactionType.EXPENSE && onConvertToEmi != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SuggestionChip(
                        onClick = { onConvertToEmi(fromAcc, transaction.amount) },
                        label = { Text("Convert to EMI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = CreditOrange
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = CreditOrange.copy(alpha = 0.1f),
                            labelColor = CreditOrange
                        ),
                        border = null,
                        modifier = Modifier.testTag("tx_convert_emi_${transaction.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${FinanceUtils.formatCurrency(kotlin.math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_tx_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
